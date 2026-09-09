package com.atguigu.exam.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.InviteCode;
import com.atguigu.exam.entity.PayOrder;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.InviteCodeMapper;
import com.atguigu.exam.mapper.PayOrderMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.PayOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付订单服务实现（支付宝沙箱，购买邀请码）
 */
@Slf4j
@Service
public class PayOrderServiceImpl implements PayOrderService {

    private static final Map<String, BigDecimal> PRICES = Map.of(
            "normal", new BigDecimal("9.90"),
            "vip", new BigDecimal("29.90"),
            "enterprise", new BigDecimal("99.90"));

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "normal", "普通", "vip", "VIP", "enterprise", "企业");

    private static final int NORMAL_BONUS = 100;
    private static final int ENTERPRISE_BONUS = 300;

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private PayOrderMapper payOrderMapper;
    @Autowired
    private InviteCodeMapper inviteCodeMapper;
    @Autowired
    private UserCreditMapper userCreditMapper;
    @Autowired
    private CreditRecordMapper creditRecordMapper;

    @Value("${alipay.app-id}")
    private String alipayAppId;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.return-url}")
    private String returnUrl;

    @Override
    public Result<Map<String, Object>> createOrder(Long userId, String type) {
        try {
            BigDecimal price = PRICES.get(type);
            if (price == null) {
                return Result.error("无效的邀请码类型");
            }
            if (userId == null || userId <= 0L) {
                return Result.error(401, "请先登录");
            }

            String orderNo = "P" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
            PayOrder order = new PayOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setProductType(type);
            order.setAmount(price);
            order.setStatus("CREATED");
            order.setCreateTime(new Date());
            payOrderMapper.insert(order);

            AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
            req.setReturnUrl(returnUrl);
            String subject = "【智能学习平台】" + TYPE_LABELS.get(type) + "邀请码";
            req.setBizContent("{\"out_trade_no\":\"" + orderNo
                    + "\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"total_amount\":\"" + price.toPlainString()
                    + "\",\"subject\":\"" + subject + "\"}");
            AlipayTradePagePayResponse resp = alipayClient.pageExecute(req, "POST");

            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("amount", price);
            data.put("form", resp.getBody());
            return Result.success(data, "创建订单成功，请完成支付");
        } catch (Exception e) {
            log.error("创建支付订单失败 userId={} type={}", userId, type, e);
            return Result.error("创建订单失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> queryStatus(Long userId, String orderNo) {
        try {
            PayOrder order = payOrderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (order.getUserId() == null || !order.getUserId().equals(userId)) {
                return Result.error(403, "无权查看该订单");
            }

            Map<String, Object> data = new HashMap<>();
            if ("PAID".equals(order.getStatus()) || "CLOSED".equals(order.getStatus())) {
                data.put("status", order.getStatus());
                data.put("tradeNo", order.getTradeNo());
                data.put("userId", order.getUserId());
                return Result.success(data);
            }

            // 未支付 → 主动向支付宝确认交易状态（本地演示无需依赖异步 notify）
            AlipayTradeQueryRequest qr = new AlipayTradeQueryRequest();
            qr.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");
            AlipayTradeQueryResponse resp = alipayClient.execute(qr);
            if (resp.isSuccess()
                    && ("TRADE_SUCCESS".equals(resp.getTradeStatus()) || "TRADE_FINISHED".equals(resp.getTradeStatus()))) {
                Map<String, Object> settled = settle(order.getId(), resp.getTradeNo(), new Date());
                data.put("status", "PAID");
                data.put("tradeNo", resp.getTradeNo());
                if (settled != null) {
                    data.putAll(settled);
                }
            } else {
                data.put("status", order.getStatus());
            }
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询支付状态失败 orderNo={}", orderNo, e);
            return Result.error("查询支付状态失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> myOrders(Long userId) {
        try {
            List<PayOrder> orders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                    .eq(PayOrder::getUserId, userId)
                    .orderByDesc(PayOrder::getCreateTime));
            List<Map<String, Object>> data = orders.stream().map(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("productType", o.getProductType());
                m.put("productLabel", TYPE_LABELS.get(o.getProductType()));
                m.put("amount", o.getAmount());
                m.put("status", o.getStatus());
                m.put("tradeNo", o.getTradeNo());
                m.put("payTime", o.getPayTime());
                m.put("createTime", o.getCreateTime());
                return m;
            }).toList();
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询支付记录失败 userId={}", userId, e);
            return Result.error("查询支付记录失败");
        }
    }

    @Override
    public String handleNotify(Map<String, String> params) {
        try {
            boolean signOk = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            if (!signOk) {
                log.warn("支付宝回调验签失败");
                return "failure";
            }
            if (!alipayAppId.equals(params.get("app_id"))) {
                log.warn("支付宝回调 app_id 不匹配");
                return "failure";
            }
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            PayOrder order = payOrderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
            if (order == null) {
                log.warn("支付宝回调订单不存在 orderNo={}", orderNo);
                return "failure";
            }
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                order.setStatus("PAID");
                order.setTradeNo(params.get("trade_no"));
                order.setNotifyTime(new Date());
                settle(order.getId(), order.getTradeNo(), new Date());
            }
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
            return "failure";
        }
    }

    /**
     * 结算：幂等地将订单置为已支付，并生成绑定用户的邀请码 + 发放积分
     */
    @Transactional(rollbackFor = Exception.class)
    protected Map<String, Object> settle(Long orderId, String tradeNo, Date payTime) {
        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getId, orderId)
                .eq(PayOrder::getStatus, "CREATED")
                .set(PayOrder::getStatus, "PAID")
                .set(PayOrder::getTradeNo, tradeNo)
                .set(PayOrder::getPayTime, payTime));
        if (rows == 0) {
            // 已被并发结算（notify 与 trade.query 可能同时触达），幂等返回
            return null;
        }

        PayOrder order = payOrderMapper.selectById(orderId);
        Long userId = order.getUserId();
        String productType = order.getProductType();

        String code = "EDU" + IdUtil.simpleUUID().substring(0, 8).toUpperCase();
        InviteCode ic = new InviteCode();
        ic.setCode(code);
        ic.setType(productType.equals("normal") ? "normal" : productType);
        ic.setStatus("used");
        ic.setActivedBy(userId);
        ic.setActivedAt(new Date());
        ic.setCreateTime(new Date());
        inviteCodeMapper.insert(ic);

        int bonus = "enterprise".equals(productType) ? ENTERPRISE_BONUS : NORMAL_BONUS;
        UserCredit credit = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
        if (credit == null) {
            credit = new UserCredit();
            credit.setUserId(userId);
            credit.setTotalCredits(0);
            credit.setActiveCredits(0);
            credit.setCreateTime(new Date());
            credit.setUpdateTime(new Date());
            userCreditMapper.insert(credit);
        }
        credit.setActiveCredits(credit.getActiveCredits() == null ? 0 : credit.getActiveCredits() + bonus);
        credit.setTotalCredits(credit.getTotalCredits() == null ? 0 : credit.getTotalCredits() + bonus);
        credit.setUpdateTime(new Date());
        userCreditMapper.updateById(credit);

        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeAmount(bonus);
        record.setType("invite");
        record.setSource("支付宝购买" + TYPE_LABELS.get(productType) + "邀请码 " + ic.getCode());
        record.setBalance(credit.getActiveCredits());
        record.setCreateTime(new Date());
        creditRecordMapper.insert(record);

        Map<String, Object> data = new HashMap<>();
        data.put("bonus", bonus);
        data.put("code", ic.getCode());
        data.put("productType", productType);
        data.put("activeCredits", credit.getActiveCredits());
        return data;
    }
}