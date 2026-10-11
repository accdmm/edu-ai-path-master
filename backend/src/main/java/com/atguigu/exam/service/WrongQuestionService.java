package com.atguigu.exam.service;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.WrongQuestion;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 错题本服务
 */
public interface WrongQuestionService extends IService<WrongQuestion> {

    /**
     * 判卷结算后自动采集错题：答错(is_correct=0)→UNMASTERED，部分正确(is_correct=2)→LEARNING。
     * 幂等：同一考试记录同一道题只累计一次（依据 last_exam_record_id 去重），判卷重跑不会重复计数。
     *
     * @param userId        考试记录归属用户
     * @param examRecordId  本次考试记录ID
     * @param answerRecords 本次答题记录（已判分）
     * @param questionMap   题目ID → 题目 映射（取分类/题干用）
     */
    void collectFromExam(Long userId, Integer examRecordId,
                         List<AnswerRecord> answerRecords, Map<Long, Question> questionMap);
}