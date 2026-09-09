package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.MockInterviewService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.MockInterviewCompleteVo;
import com.atguigu.exam.vo.MockInterviewStartResponseVo;
import com.atguigu.exam.vo.MockInterviewStartVo;
import com.atguigu.exam.vo.MockInterviewSubmitAnswerVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 模拟面试控制器
 */
@RestController
@RequestMapping("/api/mock-interview")
@CrossOrigin
@Tag(name = "AI 模拟面试")
public class MockInterviewController {

    @Autowired
    private MockInterviewService mockInterviewService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "开始模拟面试")
    @PostMapping("/start")
    public Result<MockInterviewStartResponseVo> start(@RequestBody MockInterviewStartVo vo) {
        return mockInterviewService.startMockInterview(userContextUtil.getUserId(), vo);
    }

    @Operation(summary = "提交单题答案（面试/练习）")
    @PostMapping("/submit-answer")
    public Result<Object> submitAnswer(@RequestBody MockInterviewSubmitAnswerVo vo) {
        return mockInterviewService.submitAnswer(userContextUtil.getUserId(), vo);
    }

    @Operation(summary = "完成面试")
    @PostMapping("/{interviewId}/complete")
    public Result<MockInterviewCompleteVo> complete(@PathVariable Long interviewId) {
        return mockInterviewService.completeMockInterview(userContextUtil.getUserId(), interviewId);
    }

    @Operation(summary = "我的面试记录")
    @GetMapping("/user/{userId}/records")
    public Result<IPage<Map<String, Object>>> records(@PathVariable Long userId,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        return mockInterviewService.getMyInterviews(userId, page, size);
    }
}