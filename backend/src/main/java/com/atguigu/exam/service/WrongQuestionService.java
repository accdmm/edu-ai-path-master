package com.atguigu.exam.service;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.WrongQuestion;
import com.atguigu.exam.vo.WrongQuestionStatsVo;
import com.atguigu.exam.vo.WrongQuestionVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 分页查询错题本列表（按最近答错时间倒序），并装配题目详情
     * （题干/选项/标准答案/解析/最近一次作答）
     *
     * @param userId     当前用户
     * @param page       页码（从1开始）
     * @param size       每页数量
     * @param categoryId 知识点分类筛选（可空）
     * @param status     掌握状态筛选 UNMASTERED/LEARNING/MASTERED（可空）
     */
    Page<WrongQuestionVo> pageWrongQuestions(Long userId, long page, long size, Long categoryId, String status);

    /**
     * 错题统计：总览（总/未掌握/半掌握/已掌握）+ 按知识点聚合（按错题数倒序）
     *
     * @param userId 当前用户
     */
    WrongQuestionStatsVo stats(Long userId);
}