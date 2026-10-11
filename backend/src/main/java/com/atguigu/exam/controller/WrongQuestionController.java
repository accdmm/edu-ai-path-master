package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.WrongQuestionService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.WrongQuestionStatsVo;
import com.atguigu.exam.vo.WrongQuestionVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 错题本控制器 - 错题列表查询、知识点维度统计、掌握状态管理与移除
 * 需登录（SecurityConfig 已收紧 /api/wrong-questions/**）
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/wrong-questions")
@Tag(name = "错题本", description = "错题自动采集结果查询：列表、知识点统计，支撑巩固练习")
public class WrongQuestionController {

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Autowired
    private UserContextUtil userContextUtil;

    @GetMapping("/list")
    @Operation(summary = "分页查询错题本", description = "按最近答错时间倒序，返回错题记录及题干/选项/标准答案/解析/最近作答")
    public Result<Page<WrongQuestionVo>> list(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "知识点分类筛选") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "掌握状态筛选：UNMASTERED/LEARNING/MASTERED") @RequestParam(required = false) String status) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(wrongQuestionService.pageWrongQuestions(
                userContextUtil.getUserId(), page, size, categoryId, status));
    }

    @GetMapping("/stats")
    @Operation(summary = "错题统计", description = "总览（总/未掌握/半掌握/已掌握）+ 按知识点聚合（按错题数倒序）")
    public Result<WrongQuestionStatsVo> stats() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(wrongQuestionService.stats(userContextUtil.getUserId()));
    }

    @PostMapping("/{id}/toggle-master")
    @Operation(summary = "切换掌握状态", description = "未掌握/半掌握 → 已掌握（可撤销）；已掌握 → 回落到未掌握。之后再次答错由判卷状态机自动回退")
    public Result<Void> toggleMaster(@Parameter(description = "错题记录ID") @PathVariable Long id) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        wrongQuestionService.toggleMaster(userContextUtil.getUserId(), id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移出错题本", description = "删除指定错题记录；该题之后若再次答错会被重新收录")
    public Result<Void> remove(@Parameter(description = "错题记录ID") @PathVariable Long id) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        wrongQuestionService.removeWrongQuestion(userContextUtil.getUserId(), id);
        return Result.success(null);
    }

    @PostMapping("/{id}/ai-analysis")
    @Operation(summary = "AI解析错题", description = "AI 讲解错题考点、解题步骤与易错点；每日 3 次免费，超出每次扣 5 积分（与刷题/真题解析共用额度）")
    public Result<Map<String, Object>> aiAnalysis(@Parameter(description = "错题记录ID") @PathVariable Long id) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return wrongQuestionService.aiAnalysis(userContextUtil.getUserId(), id);
    }

    @PostMapping("/consolidation-paper")
    @Operation(summary = "生成错题巩固练习卷", description = "取未掌握/半掌握错题（原题）+ 同知识点强化题组卷（DRAFT 归属当前用户，不消耗积分），返回试卷ID；前端跳 /exam/start/{paperId}")
    public Result<Long> consolidationPaper() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(wrongQuestionService.generateConsolidationPaper(userContextUtil.getUserId()));
    }
}