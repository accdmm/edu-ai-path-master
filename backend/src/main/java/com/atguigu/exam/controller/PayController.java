package com.atguigu.exam.controller;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.service.PayOrderService;
import com.atguigu.exam.utils.UserContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付宝沙箱支付控制器
 */
@RestController
@RequestMapping("/api/pay")
@CrossOrigin
@Tag(name = "支付宝沙箱支付")
public class PayController {

    @Autowired
    private PayOrderService payOrderService;
    @Autowired
    private UserContextUtil userContextUtil;

    @Operation(summary = "创建支付订单（购买邀请码）")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        return payOrderService.createOrder(userContextUtil.getUserId(), body.get("type"));
    }

    @Operation(summary = "查询订单支付状态（未支付时向支付宝确认并自动结算）")
    @GetMapping("/order/status")
    public Result<Map<String, Object>> status(@RequestParam String orderNo) {
        return payOrderService.queryStatus(userContextUtil.getUserId(), orderNo);
    }

    @Operation(summary = "当前用户的支付记录")
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders() {
        return payOrderService.myOrders(userContextUtil.getUserId());
    }

    @Operation(summary = "支付宝异步通知（验签 + 幂等结算）")
    @PostMapping("/alipay/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        return payOrderService.handleNotify(params);
    }
}