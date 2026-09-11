package com.atguigu.exam.common;

/**
 * 积分不足异常：AI 计费场景（如聊天生成试卷）在事务内扣费失败时抛出，
 * 由调用方捕获后向用户返回引导充值/购买积分的友好提示
 */
public class CreditNotEnoughException extends RuntimeException {

    public CreditNotEnoughException(String message) {
        super(message);
    }
}
