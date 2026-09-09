package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 支付订单服务接口（支付宝沙箱）
 */
public interface PayOrderService {

    /**
     * 创建订单 + 生成支付宝电脑网站支付 form
     */
    Result<Map<String, Object>> createOrder(Long userId, String type);

    /**
     * 查询订单支付状态；未支付时向支付宝确认（trade.query），成功则自动结算
     */
    Result<Map<String, Object>> queryStatus(Long userId, String orderNo);

    /**
     * 当前用户的支付记录
     */
    Result<List<Map<String, Object>>> myOrders(Long userId);

    /**
     * 处理支付宝异步通知（验签 + 幂等结算）
     */
    String handleNotify(Map<String, String> params);
}