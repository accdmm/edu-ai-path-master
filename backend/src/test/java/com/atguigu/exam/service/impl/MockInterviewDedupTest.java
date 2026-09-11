package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.MockInterviewAnswer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * AI 模拟面试同题去重单元测试：汇总时同一题保留最高分，不重复计分
 */
class MockInterviewDedupTest {

    private MockInterviewAnswer answer(Long questionId, Integer score) {
        MockInterviewAnswer a = new MockInterviewAnswer();
        a.setQuestionId(questionId);
        a.setScore(score);
        return a;
    }

    @Test
    void duplicateQuestionShouldKeepMaxScore() {
        List<MockInterviewAnswer> answers = new ArrayList<>(Arrays.asList(
                answer(1L, 60), answer(2L, 80), answer(1L, 85)));

        List<MockInterviewAnswer> deduped = MockInterviewServiceImpl.distinctByQuestionKeepMax(answers);

        // 去重后按首次出现顺序：1L 在前（保留最高分 85），2L 在后
        assertEquals(2, deduped.size());
        assertEquals(1L, deduped.get(0).getQuestionId());
        assertEquals(85, deduped.get(0).getScore());
        assertEquals(2L, deduped.get(1).getQuestionId());
        assertEquals(80, deduped.get(1).getScore());
    }

    @Test
    void nullOrSingleListShouldReturnAsIs() {
        assertNull(MockInterviewServiceImpl.distinctByQuestionKeepMax(null));
        List<MockInterviewAnswer> single = List.of(answer(1L, 60));
        assertSame(single, MockInterviewServiceImpl.distinctByQuestionKeepMax(single));
    }

    @Test
    void answersWithoutQuestionIdShouldAllBeKept() {
        List<MockInterviewAnswer> answers = new ArrayList<>(Arrays.asList(
                answer(null, 60), answer(1L, 70), answer(null, 90)));

        List<MockInterviewAnswer> deduped = MockInterviewServiceImpl.distinctByQuestionKeepMax(answers);

        assertEquals(3, deduped.size());
    }
}
