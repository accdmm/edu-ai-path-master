package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.vo.AiInterviewStartVo;

import java.util.Map;

/**
 * AI 面试官服务（BOSS 直聘式多轮对话面试）
 * AI 实时提问，根据用户回答逐轮追问/换题，面试结束输出总结评分与学习建议
 */
public interface AiInterviewService {

    /**
     * 开始面试：AI 开场白 + 第一道题
     *
     * @param userId 用户ID
     * @param vo     面试配置（方向/难度/个性化/轮数）
     * @return {interviewId, direction, difficulty, messages:[{role, content}], round, maxRounds, ended}
     */
    Result<Map<String, Object>> start(Long userId, AiInterviewStartVo vo);

    /**
     * 回答当前问题：AI 点评 + 追问或下一题；最后一轮只点评不出题
     *
     * @param userId      用户ID
     * @param interviewId 面试ID
     * @param userAnswer  用户回答
     * @return {comment, question, ended, round, maxRounds, messages:[...]}
     *         （ended=true 时 reportPending=true，报告异步生成，前端轮询 getReport）
     */
    Result<Map<String, Object>> answer(Long userId, Long interviewId, String userAnswer);

    /**
     * 主动结束面试（未答完时）：触发异步报告生成，立即返回
     *
     * @param userId      用户ID
     * @param interviewId 面试ID
     * @return {ended, reportPending, reportStatus}
     */
    Result<Map<String, Object>> finish(Long userId, Long interviewId);

    /**
     * 查询面试报告（前端轮询）：GENERATING 生成中 / DONE 返回完整报告（含每题参考答案 qaReview）/ FAILED 生成失败可重试
     */
    Result<Map<String, Object>> getReport(Long userId, Long interviewId);
}
