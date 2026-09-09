package com.atguigu.exam.service;

import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.vo.AiGenerateRequestVo;

/**
 * AI 生成试卷落库服务接口 - 生成整套试卷并持久化
 *
 * 流程：大模型出题 → 动态建分类 → 题目/选项/答案入库 → 试卷(DRAFT) → 试卷题目关联 → 用户试卷归属
 */
public interface AiGeneratedPaperService {

    /**
     * 生成一套完整试卷并持久化
     *
     * @param userId  归属用户ID
     * @param request 出题入参
     * @return 创建好的试卷对象（含总分数、题数）
     */
    Paper generateAndSave(Long userId, AiGenerateRequestVo request);
}