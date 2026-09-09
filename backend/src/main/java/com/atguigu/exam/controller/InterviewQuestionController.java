package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.InterviewQuestionService;
import com.atguigu.exam.utils.UserContextUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业真题用户端控制器
 */
@RestController
@RequestMapping("/api/interview-questions")
@CrossOrigin
@Tag(name = "企业真题（用户端）")
public class InterviewQuestionController {

    @Autowired
    private InterviewQuestionService interviewQuestionService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "真题列表")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String difficultyLevel,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String keyword) {
        return interviewQuestionService.getQuestionList(
                userContextUtil.getUserId(), page, size, direction, difficultyLevel, companyId, keyword);
    }

    @Operation(summary = "真题详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return interviewQuestionService.getQuestionDetail(userContextUtil.getUserId(), id);
    }

    @Operation(summary = "热门真题")
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return interviewQuestionService.getHotQuestions(limit);
    }

    @Operation(summary = "最新真题")
    @GetMapping("/latest")
    public Result<List<Map<String, Object>>> latest(@RequestParam(defaultValue = "10") Integer limit) {
        return interviewQuestionService.getLatestQuestions(limit);
    }

    @Operation(summary = "浏览量+1")
    @PostMapping("/{id}/view")
    public Result<Void> view(@PathVariable Long id) {
        return interviewQuestionService.incrementViewCount(id);
    }

    @Operation(summary = "相关真题")
    @GetMapping("/{id}/related")
    public Result<List<Map<String, Object>>> related(@PathVariable Long id,
                                                     @RequestParam(required = false) String direction,
                                                     @RequestParam(defaultValue = "5") Integer limit) {
        return interviewQuestionService.getRelatedQuestions(id, direction, limit);
    }

    @Operation(summary = "提交AI练习评测")
    @PostMapping("/evaluation")
    public Result<Map<String, Object>> evaluation(@RequestBody Map<String, Object> body) {
        Long questionId = body.get("questionId") != null
                ? Long.valueOf(body.get("questionId").toString()) : null;
        String userAnswer = body.get("userAnswer") != null ? body.get("userAnswer").toString() : null;
        return interviewQuestionService.submitEvaluation(userContextUtil.getUserId(), questionId, userAnswer);
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{id}/favorite")
    public Result<Map<String, Object>> favorite(@PathVariable Long id) {
        return interviewQuestionService.toggleFavorite(userContextUtil.getUserId(), id);
    }

    @Operation(summary = "方向统计")
    @GetMapping("/stats/direction")
    public Result<List<Map<String, Object>>> directionStats() {
        return interviewQuestionService.getDirectionStats();
    }

    @Operation(summary = "公司统计")
    @GetMapping("/stats/company")
    public Result<List<Map<String, Object>>> companyStats() {
        return interviewQuestionService.getCompanyStats();
    }
}