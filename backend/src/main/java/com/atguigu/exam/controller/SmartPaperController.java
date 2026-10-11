package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.SmartPaperService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.SmartPaperVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 智能组卷控制器 - 根据薄弱知识点一键组卷（消耗积分）
 * 需登录（SecurityConfig 已收紧 /api/smart-paper/**）
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/api/smart-paper")
@Tag(name = "AI智能组卷", description = "AI 根据薄弱知识点从题库智能选题组卷，每次消耗积分")
public class SmartPaperController {

    @Autowired
    private SmartPaperService smartPaperService;

    @Autowired
    private UserContextUtil userContextUtil;

    @GetMapping("/weak-points")
    @Operation(summary = "查询薄弱知识点", description = "错题本按知识点聚合的薄弱点（未掌握/半掌握错题数倒序，最多3个），组卷前展示 AI 将重点考察的范围")
    public Result<List<SmartPaperVo.WeakPoint>> weakPoints() {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return Result.success(smartPaperService.weakPoints(userContextUtil.getUserId()));
    }

    @PostMapping("/generate")
    @Operation(summary = "AI智能组卷", description = "AI 根据薄弱知识点从题库智能选题组卷（题型覆盖 + 难度梯度 + 薄弱点加权），每次固定消耗积分，生成失败不扣费；返回试卷ID，前端跳 /exam/start/{paperId}")
    public Result<SmartPaperVo> generate(
            @Parameter(description = "题量（可空，默认10，范围5-20）") @RequestParam(required = false) Integer count) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "未登录");
        }
        return smartPaperService.generate(userContextUtil.getUserId(), count);
    }
}