package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.AiInterviewService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.AiInterviewStartVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 面试官控制器（BOSS 直聘式多轮对话面试）
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-interview")
@Tag(name = "AI 面试官", description = "AI 实时提问、追问的多轮对话面试")
public class AiInterviewController {

    @Autowired
    private AiInterviewService aiInterviewService;

    @Autowired
    private UserContextUtil userContextUtil;

    @PostMapping("/start")
    @Operation(summary = "开始 AI 面试", description = "AI 开场白并出第一题，返回面试会话")
    public Result<Map<String, Object>> start(@RequestBody AiInterviewStartVo vo) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        return aiInterviewService.start(userContextUtil.getUserId(), vo);
    }

    @PostMapping("/answer")
    @Operation(summary = "回答当前问题", description = "提交回答，AI 点评并追问或换下一题")
    public Result<Map<String, Object>> answer(@RequestBody Map<String, Object> body) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        Long interviewId = body.get("interviewId") == null
                ? null : Long.valueOf(body.get("interviewId").toString());
        String userAnswer = body.get("userAnswer") == null ? null : body.get("userAnswer").toString();
        return aiInterviewService.answer(userContextUtil.getUserId(), interviewId, userAnswer);
    }

    @PostMapping("/finish")
    @Operation(summary = "结束 AI 面试", description = "提前结束面试并触发异步报告生成，立即返回")
    public Result<Map<String, Object>> finish(@RequestBody Map<String, Object> body) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        Long interviewId = body.get("interviewId") == null
                ? null : Long.valueOf(body.get("interviewId").toString());
        return aiInterviewService.finish(userContextUtil.getUserId(), interviewId);
    }

    @GetMapping("/report/{interviewId}")
    @Operation(summary = "查询面试报告", description = "前端轮询：GENERATING 生成中 / DONE 返回完整报告（含每题参考答案）/ FAILED 可重试")
    public Result<Map<String, Object>> report(@PathVariable Long interviewId) {
        if (!userContextUtil.isAuthenticated()) {
            return Result.error(401, "请先登录");
        }
        return aiInterviewService.getReport(userContextUtil.getUserId(), interviewId);
    }
}
