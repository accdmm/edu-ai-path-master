package com.atguigu.exam.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考试时长策略单元测试
 */
class ExamTimePolicyTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 11, 10, 0, 0);

    @Test
    void withinDurationShouldNotBeOvertime() {
        // 120 分钟时长，开考 119 分钟：未超时
        assertFalse(ExamTimePolicy.isOvertime(START, 120, START.plusMinutes(119)));
    }

    @Test
    void withinGraceShouldNotBeOvertime() {
        // 时长 120 分钟 + 5 分钟宽限：第 124 分钟仍可交卷
        assertFalse(ExamTimePolicy.isOvertime(START, 120, START.plusMinutes(124)));
    }

    @Test
    void beyondGraceShouldBeOvertime() {
        // 时长 120 分钟 + 5 分钟宽限：第 126 分钟超时
        assertTrue(ExamTimePolicy.isOvertime(START, 120, START.plusMinutes(126)));
    }

    @Test
    void nullOrNonPositiveDurationMeansUnlimited() {
        // 不限时试卷：任何时候都不算超时
        assertFalse(ExamTimePolicy.isOvertime(START, null, START.plusDays(1)));
        assertFalse(ExamTimePolicy.isOvertime(START, 0, START.plusDays(1)));
        assertFalse(ExamTimePolicy.isOvertime(START, -5, START.plusDays(1)));
    }

    @Test
    void nullStartTimeOrNullNowMeansNotOvertime() {
        assertFalse(ExamTimePolicy.isOvertime(null, 60, START.plusDays(1)));
        assertFalse(ExamTimePolicy.isOvertime(START, 60, null));
    }
}
