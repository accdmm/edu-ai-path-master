package com.atguigu.exam.vo;

import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AI 面试会话 Redis 序列化单元测试：保证重启后从 Redis 恢复的会话字段完整
 */
class AiInterviewSessionCodecTest {

    @Test
    void sessionShouldSurviveJsonRoundTrip() {
        AiInterviewSession s = new AiInterviewSession();
        s.userId = 7L;
        s.direction = "java";
        s.difficulty = "medium";
        s.maxRounds = 6;
        s.round = 3;
        s.startTime = 1000L;
        s.lastActive = 2000L;
        s.diagnosisText = "薄弱知识点：JVM 得分率 40%";
        s.askedQuestions.add("谈谈 HashMap 的底层实现");
        s.askedQuestions.add("JVM 垃圾回收算法有哪些");
        s.messages.add(Map.of("role", "ai", "content", "你好，我是你的 AI 面试官。"));
        s.messages.add(Map.of("role", "user", "content", "好的"));
        s.reportStatus = "GENERATING";

        String json = JSON.toJSONString(s);
        AiInterviewSession back = JSON.parseObject(json, AiInterviewSession.class);

        assertEquals(7L, back.userId);
        assertEquals("java", back.direction);
        assertEquals("medium", back.difficulty);
        assertEquals(6, back.maxRounds);
        assertEquals(3, back.round);
        assertEquals(1000L, back.startTime);
        assertEquals("薄弱知识点：JVM 得分率 40%", back.diagnosisText);
        assertEquals(2, back.askedQuestions.size());
        assertEquals("谈谈 HashMap 的底层实现", back.askedQuestions.get(0));
        assertEquals(2, back.messages.size());
        assertEquals("ai", back.messages.get(0).get("role"));
        assertEquals("你好，我是你的 AI 面试官。", back.messages.get(0).get("content"));
        assertEquals("GENERATING", back.reportStatus);
        assertNull(back.report);
    }

    @Test
    void reportMapShouldSurviveRoundTrip() {
        AiInterviewSession s = new AiInterviewSession();
        s.reportStatus = "DONE";
        s.report = Map.of(
                "score", 82,
                "summary", "整体表现良好",
                "strengths", List.of("基础扎实"));

        AiInterviewSession back = JSON.parseObject(JSON.toJSONString(s), AiInterviewSession.class);

        assertEquals("DONE", back.reportStatus);
        assertEquals(82, back.report.get("score"));
        assertEquals("整体表现良好", back.report.get("summary"));
    }
}
