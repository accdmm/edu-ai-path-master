package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 企业真题服务接口
 */
public interface InterviewQuestionService {

    /**
     * 分页查询真题列表（用户端，仅已审核）
     */
    Result<IPage<Map<String, Object>>> getQuestionList(Long userId, Integer page, Integer size,
                                                       String direction, String difficultyLevel,
                                                       Long companyId, String keyword);

    /**
     * 真题详情（含是否收藏）
     */
    Result<Map<String, Object>> getQuestionDetail(Long userId, Long id);

    /**
     * 热门真题
     */
    Result<List<Map<String, Object>>> getHotQuestions(Integer limit);

    /**
     * 最新真题
     */
    Result<List<Map<String, Object>>> getLatestQuestions(Integer limit);

    /**
     * 相关真题
     */
    Result<List<Map<String, Object>>> getRelatedQuestions(Long id, String direction, Integer limit);

    /**
     * 浏览量 +1
     */
    Result<Void> incrementViewCount(Long id);

    /**
     * 提交 AI 练习评测
     */
    Result<Map<String, Object>> submitEvaluation(Long userId, Long questionId, String userAnswer);

    /**
     * AI 解析本题（消费 5 积分或消耗每日免费额度）
     */
    Result<Map<String, Object>> aiAnalysis(Long userId, Long questionId);

    /**
     * 收藏 / 取消收藏
     */
    Result<Map<String, Object>> toggleFavorite(Long userId, Long questionId);

    /**
     * 题目收藏列表
     */
    Result<IPage<Map<String, Object>>> getFavoriteList(Long userId, Integer page, Integer size);

    /**
     * 方向统计
     */
    Result<List<Map<String, Object>>> getDirectionStats();

    /**
     * 企业统计
     */
    Result<List<Map<String, Object>>> getCompanyStats();
}