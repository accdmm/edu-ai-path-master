package com.atguigu.exam.config;

import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.service.ExamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis Stream 判卷任务消费者
 *
 * <p>负责消费 exam:grade 队列中的判卷任务并异步执行 AI 判卷。
 * 可靠性机制：</p>
 * <ul>
 *   <li>判卷成功的消息立即 XACK，从 pending list 移除；</li>
 *   <li>判卷失败的消息不 ACK，留在 pending list，由死信扫描（idle&gt;5min）XCLAIM 重投；</li>
 *   <li>同一任务重试达到上限后强制结算（客观题成绩 + 降级总评），避免队列永久积压。</li>
 * </ul>
 */
@Slf4j
@Component
public class ExamGradingWorker implements ApplicationRunner {

    private static final String STREAM = "exam:grade";
    private static final String GROUP = "grader";
    private static final String CONSUMER = "worker1";
    private static final String RETRY_PREFIX = "exam:grade:retry:";

    /** 死信 idle 阈值 */
    private static final long STALE_MS = 5 * 60 * 1000;
    /** 单任务最大重试次数 */
    private static final int MAX_RETRY = 3;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ExamService examService;

    private long lastReclaim = 0;

    @Override
    public void run(ApplicationArguments args) {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM, ReadOffset.from("0"), GROUP);
            log.info("Redis Stream 判卷任务组已就绪: {}", STREAM);
        } catch (Exception e) {
            // 组已存在时抛 BUSYGROUP 异常，忽略即可
            log.debug("判卷任务组已存在或创建失败: {}", e.getMessage());
        }
        Thread worker = new Thread(this::consumeLoop, "exam-grading-worker");
        worker.setDaemon(false);
        worker.start();
        log.info("AI 判卷消费者线程已启动");
    }

    private void consumeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(GROUP, CONSUMER),
                        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                        StreamOffset.create(STREAM, ReadOffset.lastConsumed()));
                if (records != null) {
                    for (MapRecord<String, Object, Object> record : records) {
                        processRecord(record);
                    }
                }
            } catch (Exception e) {
                log.error("消费判卷任务异常: {}", e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            reclaimStale();
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        Object idValObj = record.getValue().get("examRecordId");
        if (idValObj == null) {
            ack(record.getId());
            return;
        }
        final Integer examRecordId;
        try {
            examRecordId = Integer.valueOf(String.valueOf(idValObj));
        } catch (NumberFormatException e) {
            log.warn("非法的判卷任务 id: {}", idValObj);
            ack(record.getId());
            return;
        }

        ExamRecord examRecord = examService.getById(examRecordId);
        if (examRecord == null || "已批阅".equals(examRecord.getStatus())) {
            // 记录不存在或已批阅（幂等），直接确认
            ack(record.getId());
            return;
        }

        try {
            examService.gradeExam(examRecordId);
            stringRedisTemplate.delete(RETRY_PREFIX + examRecordId);
            ack(record.getId());
            log.info("AI 判卷完成 examRecordId={}", examRecordId);
        } catch (Exception e) {
            log.error("AI 判卷失败 examRecordId={} 原因:{}", examRecordId, e.getMessage());
            Long n = stringRedisTemplate.opsForValue().increment(RETRY_PREFIX + examRecordId);
            if (n != null && n >= MAX_RETRY) {
                // 多次失败：按客观题成绩强制结算，避免消息永久积压
                ExamRecord cur = examService.getById(examRecordId);
                if (cur != null && !"已批阅".equals(cur.getStatus())) {
                    cur.setStatus("已批阅");
                    cur.setScore(cur.getScore() == null ? 0 : cur.getScore());
                    cur.setAnswers("AI 判卷多次失败，成绩仅按客观题结算，请在练习页针对薄弱知识点复习。");
                    examService.updateById(cur);
                }
                stringRedisTemplate.delete(RETRY_PREFIX + examRecordId);
                ack(record.getId());
            }
            // 未达上限：不 ACK，留在 pending list，交由死信扫描重投
        }
    }

    /**
     * 死信兜底：扫描超过阈值的未 ACK 消息并重投到本消费者
     */
    private void reclaimStale() {
        long now = System.currentTimeMillis();
        if (now - lastReclaim < STALE_MS) {
            return;
        }
        lastReclaim = now;
        try {
            PendingMessages pending = stringRedisTemplate.opsForStream().pending(STREAM, GROUP, Range.unbounded(), 100);
            if (pending == null) {
                return;
            }
            for (PendingMessage pm : pending) {
                if (pm.getElapsedTimeSinceLastDelivery().toMillis() > STALE_MS) {
                    List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream().claim(
                            STREAM, GROUP, CONSUMER, Duration.ofSeconds(60),
                            RecordId.of(pm.getId().toString()));
                    if (claimed != null) {
                        for (MapRecord<String, Object, Object> record : claimed) {
                            processRecord(record);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("判卷死信扫描异常: {}", e.getMessage());
        }
    }

    private void ack(RecordId recordId) {
        try {
            stringRedisTemplate.opsForStream().acknowledge(STREAM, GROUP, recordId);
        } catch (Exception e) {
            log.warn("确认判卷消息失败: {}", e.getMessage());
        }
    }
}