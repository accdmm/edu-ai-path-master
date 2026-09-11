package com.atguigu.exam.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能客服“生成整套试卷”意图识别单元测试
 */
class ChatIntentTest {

    @Test
    void standardPaperRequestShouldTrigger() {
        // 官方引导话术：帮我生成一套 XX 试卷
        assertTrue(ChatController.isGeneratePaperRequest("帮我生成一套JVM试卷"));
        assertTrue(ChatController.isGeneratePaperRequest("出一套Java并发题目"));
        assertTrue(ChatController.isGeneratePaperRequest("来一套MySQL的练习题"));
        assertTrue(ChatController.isGeneratePaperRequest("帮我生成一套试卷"));
    }

    @Test
    void singleQuestionAnalysisShouldNotTrigger() {
        // 回归用例：以前“帮我生成”+任意“题”字会误触发整套出题
        assertFalse(ChatController.isGeneratePaperRequest("帮我生成这道题的解析"));
        assertFalse(ChatController.isGeneratePaperRequest("帮我出个主意做这道题"));
        assertFalse(ChatController.isGeneratePaperRequest("帮我生成一个解题思路"));
    }

    @Test
    void normalChatShouldNotTrigger() {
        assertFalse(ChatController.isGeneratePaperRequest("你好"));
        assertFalse(ChatController.isGeneratePaperRequest("这道题怎么解"));
        assertFalse(ChatController.isGeneratePaperRequest(""));
        assertFalse(ChatController.isGeneratePaperRequest(null));
    }
}
