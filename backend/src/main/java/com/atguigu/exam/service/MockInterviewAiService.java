package com.atguigu.exam.service;

import com.atguigu.exam.vo.MockInterviewAnswerDetailVo;

import java.util.List;
import java.util.Map;

/**
 * AI 模拟面试官服务接口
 * 复用 KimiAiService.callKimiAi() 调用大模型，实现逐题评分与整体总结
 */
public interface MockInterviewAiService {

    /**
     * AI 评分单题面试答案
     *
     * @param direction    技术方向
     * @param questionContent 题目内容
     * @param difficultyLevel 难度
     * @param userAnswer   用户答案
     * @return {score:0-100, comment:评语, technicalAccuracy:1-5, clarity:1-5, logic:1-5}
     */
    Map<String, Object> gradeAnswer(String direction, String questionContent, String difficultyLevel, String userAnswer);

    /**
     * AI 生成整体面试总结与改进建议
     *
     * @param answers 已评分的答题列表（含题目、得分、评价）
     * @return {summary:总结, strengths:[...], improvements:[...], abilityScores:{technicalAccuracy,clarity,logic,knowledge,experience}}
     */
    Map<String, Object> summarizeInterview(List<MockInterviewAnswerDetailVo> answers);
}