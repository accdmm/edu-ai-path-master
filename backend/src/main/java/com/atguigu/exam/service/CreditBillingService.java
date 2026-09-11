package com.atguigu.exam.service;

/**
 * 积分计费服务：AI 能力扣费/记账的统一入口
 */
public interface CreditBillingService {

    /**
     * 条件扣减积分（原子 UPDATE，余额不足时不扣）
     *
     * @param userId 用户ID
     * @param cost   扣减数量（正数）
     * @return true=扣减成功；false=积分不足或用户不存在
     */
    boolean deductIfEnough(Long userId, int cost);

    /**
     * 查询当前可用积分（无记录返回 0）
     */
    int currentBalance(Long userId);

    /**
     * 写积分流水
     *
     * @param changeAmount 变动数量（扣费为负、发放为正）
     * @param type         业务类型（如 ai-paper / mock-interview / contribution）
     * @param source       流水说明
     * @param balance      变动后余额快照
     */
    void record(Long userId, int changeAmount, String type, String source, int balance);
}
