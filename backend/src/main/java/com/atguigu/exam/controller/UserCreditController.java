package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.UserCreditService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户积分控制器
 */
@RestController
@RequestMapping
@CrossOrigin
@Tag(name = "用户积分")
public class UserCreditController {

    @Autowired
    private UserCreditService userCreditService;

    @Operation(summary = "查询用户积分")
    @GetMapping("/api/user-interview-credits/user/{userId}")
    public Result<Map<String, Object>> getUserCredit(@PathVariable Long userId) {
        return userCreditService.getUserCredit(userId);
    }

    @Operation(summary = "查询用户可用积分")
    @GetMapping("/api/user-interview-credits/active/{userId}")
    public Result<Map<String, Object>> getActiveCredit(@PathVariable Long userId) {
        return userCreditService.getUserCredit(userId);
    }

    @Operation(summary = "积分流水")
    @GetMapping("/interview/credits/history")
    public Result<IPage<Map<String, Object>>> creditHistory(@RequestParam(required = false) Long userId,
                                                            @RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size) {
        long uid = userId == null ? 1L : userId;
        return userCreditService.getCreditRecords(uid, page, size);
    }

    @Operation(summary = "积分排行榜")
    @GetMapping("/api/user-interview-credits/ranking")
    public Result<List<Map<String, Object>>> ranking(@RequestParam(defaultValue = "10") Integer limit) {
        return userCreditService.getCreditRanking(limit);
    }
}