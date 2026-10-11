package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.WrongQuestion;
import com.atguigu.exam.mapper.WrongQuestionMapper;
import com.atguigu.exam.service.WrongQuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 错题本服务实现
 *
 * 采集规则：
 * - 答错 is_correct=0 → 状态 UNMASTERED（未掌握），wrong_count+1，correct_streak 归零
 * - 部分正确 is_correct=2 → 状态 LEARNING（半掌握），同上
 * - 答对 is_correct=1 不在本模块处理（掌握度状态机在后续模块完善）
 * 幂等：以 last_exam_record_id 判定本场考试是否已采集过该题，判卷重跑/重复触发不会重复计数
 */
@Slf4j
@Service
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion> implements WrongQuestionService {

    @Override
    public void collectFromExam(Long userId, Integer examRecordId,
                                List<AnswerRecord> answerRecords, Map<Long, Question> questionMap) {
        if (userId == null || examRecordId == null || ObjectUtils.isEmpty(answerRecords) || questionMap == null) {
            return;
        }
        List<WrongQuestion> toInsert = new ArrayList<>();
        List<WrongQuestion> toUpdate = new ArrayList<>();
        for (AnswerRecord ar : answerRecords) {
            if (ar.getQuestionId() == null) {
                continue;
            }
            Integer isCorrect = ar.getIsCorrect();
            // 仅采集答错(0)与部分正确(2)
            if (isCorrect == null || isCorrect == 1) {
                continue;
            }
            long questionId = ar.getQuestionId().longValue();
            WrongQuestion existing = getOne(new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getUserId, userId)
                    .eq(WrongQuestion::getQuestionId, questionId), false);
            // 幂等：本场考试已采集过该题则跳过
            if (existing != null && existing.getLastExamRecordId() != null
                    && existing.getLastExamRecordId() == examRecordId.longValue()) {
                continue;
            }
            Question question = questionMap.get(questionId);
            Long categoryId = question == null ? null : question.getCategoryId();
            String status = isCorrect == 2 ? "LEARNING" : "UNMASTERED";
            if (existing == null) {
                WrongQuestion wrong = new WrongQuestion();
                wrong.setUserId(userId);
                wrong.setQuestionId(questionId);
                wrong.setCategoryId(categoryId);
                wrong.setWrongCount(1);
                wrong.setCorrectStreak(0);
                wrong.setStatus(status);
                wrong.setLastWrongTime(LocalDateTime.now());
                wrong.setLastExamRecordId(examRecordId.longValue());
                toInsert.add(wrong);
            } else {
                existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
                existing.setCorrectStreak(0);
                existing.setStatus(status);
                existing.setLastWrongTime(LocalDateTime.now());
                existing.setLastExamRecordId(examRecordId.longValue());
                if (existing.getCategoryId() == null && categoryId != null) {
                    existing.setCategoryId(categoryId);
                }
                toUpdate.add(existing);
            }
        }
        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }
        if (!toInsert.isEmpty() || !toUpdate.isEmpty()) {
            log.info("错题本采集完成 examRecordId={} userId={} 新增{}条 更新{}条",
                    examRecordId, userId, toInsert.size(), toUpdate.size());
        }
    }
}