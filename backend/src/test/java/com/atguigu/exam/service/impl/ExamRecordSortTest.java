package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Question;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 成绩单答题记录排序单元测试：题序必须与卷面一致
 */
class ExamRecordSortTest {

    private Question question(long id) {
        Question q = new Question();
        q.setId(id);
        return q;
    }

    private AnswerRecord answer(int questionId) {
        return new AnswerRecord(1, questionId, "A");
    }

    @Test
    void answersShouldFollowPaperOrder() {
        // 卷面题目顺序：8, 2, 1, 3（题目 ID 不连续，模拟真实组卷）
        List<Question> paperQuestions = Arrays.asList(question(8), question(2), question(1), question(3));
        // 答题记录乱序到达：1, 8, 3
        List<AnswerRecord> records = new ArrayList<>(Arrays.asList(answer(1), answer(8), answer(3)));

        ExamServiceImpl.sortAnswerRecordsByPaper(records, paperQuestions);

        assertEquals(8, records.get(0).getQuestionId());
        assertEquals(1, records.get(1).getQuestionId());
        assertEquals(3, records.get(2).getQuestionId());
    }

    @Test
    void unknownQuestionShouldGoLast() {
        List<Question> paperQuestions = Arrays.asList(question(8), question(2));
        // questionId=99 不在卷内（如考试中改过卷），排最后且不干扰已知题目
        List<AnswerRecord> records = new ArrayList<>(Arrays.asList(answer(99), answer(2), answer(8)));

        ExamServiceImpl.sortAnswerRecordsByPaper(records, paperQuestions);

        assertEquals(8, records.get(0).getQuestionId());
        assertEquals(2, records.get(1).getQuestionId());
        assertEquals(99, records.get(2).getQuestionId());
    }
}
