package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.vo.SmartPaperVo;

import java.util.List;

/**
 * AI 智能组卷服务 - 根据用户薄弱知识点从题库智能选题组卷（消耗积分）
 */
public interface SmartPaperService {

    /**
     * AI 根据薄弱知识点智能组卷：
     * <ul>
     *   <li>薄弱点 = 错题本按知识点聚合（未掌握/半掌握错题数排序，取前几个）；无错题时按题库综合组卷</li>
     *   <li>AI 从候选题库（薄弱知识点优先 + 其他知识点补充）中编排选题，题型覆盖与难度梯度由 AI 把控；
     *       AI 返回异常/数量不足时规则化兜底补足，保证题量充足</li>
     *   <li>计费：每次固定消耗积分（生成失败不扣费）；落库为归属当前用户的 DRAFT 试卷，考完自动回写掌握度</li>
     * </ul>
     *
     * @param userId        当前用户
     * @param questionCount 题量（可空，默认 10，范围 5-20）
     * @return 试卷ID、实际组成与扣费结果；积分不足返回 4002
     */
    Result<SmartPaperVo> generate(Long userId, Integer questionCount);

    /**
     * 查询当前用户薄弱知识点（供组卷前展示，AI 将重点考察这些知识点）
     *
     * @param userId 当前用户
     */
    List<SmartPaperVo.WeakPoint> weakPoints(Long userId);
}