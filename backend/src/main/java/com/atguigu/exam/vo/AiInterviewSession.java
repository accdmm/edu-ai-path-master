package com.atguigu.exam.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 面试官会话（持久化到 Redis，TTL 自动过期）
 * 替代原 JVM 内存 Map：后端重启不再丢失进行中的面试会话（积分已扣，不能白扣）
 * 字段 public + 默认构造，便于 fastjson2 直接序列化/反序列化，服务内按 s.userId 直连访问
 */
public class AiInterviewSession {

    public Long userId;
    public String direction;
    public String difficulty;
    public int maxRounds;
    public int round;
    public long startTime;
    public long lastActive;
    /** 个性化时的薄弱知识点文本 */
    public String diagnosisText = "";
    /** 已问过的题目（降级换题时去重） */
    public List<String> askedQuestions = new ArrayList<>();
    /** 对话消息 [{role: ai|user, content: ...}] */
    public List<Map<String, String>> messages = new ArrayList<>();
    /** 面试报告状态：NONE / GENERATING / DONE / FAILED */
    public String reportStatus = "NONE";
    /** 已生成的面试报告（含 qaReview 每题参考答案） */
    public Map<String, Object> report;
}
