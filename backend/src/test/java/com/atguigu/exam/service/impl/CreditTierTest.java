package com.atguigu.exam.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 邀请码/支付档位积分单元测试：VIP 档必须有区别于普通档的可感知权益
 */
class CreditTierTest {

    @Test
    void inviteCodeBonusTiers() {
        assertEquals(100, InviteCodeServiceImpl.bonusForType("normal"));
        assertEquals(200, InviteCodeServiceImpl.bonusForType("vip"));
        assertEquals(300, InviteCodeServiceImpl.bonusForType("enterprise"));
        // 未知/空档位按普通档兜底
        assertEquals(100, InviteCodeServiceImpl.bonusForType(null));
        assertEquals(100, InviteCodeServiceImpl.bonusForType("unknown"));
    }

    @Test
    void payOrderBonusTiers() {
        assertEquals(100, PayOrderServiceImpl.bonusForType("normal"));
        assertEquals(200, PayOrderServiceImpl.bonusForType("vip"));
        assertEquals(300, PayOrderServiceImpl.bonusForType("enterprise"));
        assertEquals(100, PayOrderServiceImpl.bonusForType(null));
        assertEquals(100, PayOrderServiceImpl.bonusForType("unknown"));
    }
}
