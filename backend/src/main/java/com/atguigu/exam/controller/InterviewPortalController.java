package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.MockInterviewService;
import com.atguigu.exam.utils.UserContextUtil;
import com.atguigu.exam.vo.InterviewResultVo;
import com.atguigu.exam.vo.MockInterviewDetailVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试门户控制器（/interview/*）
 */
@RestController
@RequestMapping("/interview")
@CrossOrigin
@Tag(name = "面试门户")
public class InterviewPortalController {

    @Autowired
    private MockInterviewService mockInterviewService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "面试详情")
    @GetMapping("/mock-interview/{interviewId}")
    public Result<MockInterviewDetailVo> detail(@PathVariable Long interviewId) {
        return mockInterviewService.getMockInterviewDetail(userContextUtil.getUserId(), interviewId);
    }

    @Operation(summary = "面试结果")
    @GetMapping("/result/{interviewId}")
    public Result<InterviewResultVo> result(@PathVariable Long interviewId) {
        return mockInterviewService.getInterviewResult(userContextUtil.getUserId(), interviewId);
    }

    @Operation(summary = "面试历史")
    @GetMapping("/history")
    public Result<IPage<Map<String, Object>>> history(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        return mockInterviewService.getMyInterviews(userContextUtil.getUserId(), page, size);
    }

    @Operation(summary = "面试统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        IPage<Map<String, Object>> page = mockInterviewService.getMyInterviews(
                userContextUtil.getUserId(), 1, 100).getData();
        Map<String, Object> data = new HashMap<>();
        if (page != null) {
            data.put("total", page.getTotal());
            data.put("completed", page.getRecords().stream().filter(r -> "completed".equals(r.get("status"))).count());
        }
        return Result.success(data);
    }

    @Operation(summary = "分享面试结果")
    @PostMapping("/share")
    public Result<Map<String, Object>> share(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = new HashMap<>();
        data.put("shareUrl", "/interview/result/" + body.get("interviewId"));
        data.put("shareType", body.get("shareType"));
        return Result.success(data);
    }
}