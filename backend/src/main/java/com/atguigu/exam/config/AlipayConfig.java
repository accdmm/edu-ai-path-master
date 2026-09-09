package com.atguigu.exam.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝开放平台客户端配置（当前指向沙箱环境）
 */
@Configuration
public class AlipayConfig {

    @Value("${alipay.gateway}")
    private String gateway;

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key}")
    private String privateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    /**
     * 统一构建 AlipayClient，后续页面支付（pagePay）、交易查询（trade.query）都走它
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(gateway, appId, privateKey, "json", "UTF-8", alipayPublicKey, "RSA2");
    }
}