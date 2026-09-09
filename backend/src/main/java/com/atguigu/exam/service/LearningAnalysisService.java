package com.atguigu.exam.service;

import com.atguigu.exam.vo.LearningReportVo;

import java.util.List;

/**
 * 学习分析服务 - AI 分析模块聚合考试、刷题、面试数据
 */
public interface LearningAnalysisService {

    /**
     * 生成指定用户的学习分析报告
     */
    LearningReportVo getReport(Long userId);

    /**
     * 基于报告调用 AI 生成个性化学习建议（失败时降级为规则型建议）
     */
    List<String> generateAiSuggest(Long userId, LearningReportVo report);
}