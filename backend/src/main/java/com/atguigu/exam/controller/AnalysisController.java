package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.LearningAnalysisService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.LearningReportVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习分析控制器 - AI 分析模块
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@Tag(name = "AI学习分析", description = "智能学习报告、能力雷达图、AI个性化学习建议")
public class AnalysisController {

    @Autowired
    private LearningAnalysisService learningAnalysisService;

    @Autowired
    private UserContextUtil userContextUtil;

    /**
     * 获取当前登录用户的学习分析报告
     */
    @GetMapping("/learning-report")
    @Operation(summary = "获取学习分析报告", description = "聚合考试趋势、知识点掌握度、雷达图数据")
    public Result<LearningReportVo> learningReport() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        LearningReportVo report = learningAnalysisService.getReport(userContextUtil.getUserId());
        log.info("学习分析报告接口调用成功！");
        return Result.success(report);
    }

    /**
     * 生成 AI 个性化学习建议
     */
    @GetMapping("/ai-suggest")
    @Operation(summary = "生成 AI 学习建议", description = "基于学习报告调用 AI 生成个性化学习建议，失败时降级为规则建议")
    public Result<List<String>> aiSuggest() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        Long userId = userContextUtil.getUserId();
        LearningReportVo report = learningAnalysisService.getReport(userId);
        List<String> suggestions = learningAnalysisService.generateAiSuggest(userId, report);
        log.info("AI 学习建议接口调用成功！");
        return Result.success(suggestions);
    }
}