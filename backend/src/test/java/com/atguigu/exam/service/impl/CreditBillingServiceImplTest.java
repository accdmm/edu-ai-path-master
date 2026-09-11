package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 积分计费服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class CreditBillingServiceImplTest {

    @Mock
    private UserCreditMapper userCreditMapper;

    @Mock
    private CreditRecordMapper creditRecordMapper;

    @InjectMocks
    private CreditBillingServiceImpl creditBillingService;

    @Test
    void deductShouldReturnTrueWhenBalanceEnough() {
        when(userCreditMapper.update(any(), any())).thenReturn(1);
        assertTrue(creditBillingService.deductIfEnough(100L, 5));
    }

    @Test
    void deductShouldReturnFalseWhenBalanceNotEnough() {
        when(userCreditMapper.update(any(), any())).thenReturn(0);
        assertFalse(creditBillingService.deductIfEnough(100L, 5));
    }

    @Test
    void deductShouldRejectInvalidInput() {
        assertFalse(creditBillingService.deductIfEnough(null, 5));
        assertFalse(creditBillingService.deductIfEnough(100L, 0));
        assertFalse(creditBillingService.deductIfEnough(100L, -1));
        verify(userCreditMapper, never()).update(any(), any());
    }

    @Test
    void currentBalanceShouldReturnZeroWhenNoAccount() {
        when(userCreditMapper.selectOne(any())).thenReturn(null);
        assertEquals(0, creditBillingService.currentBalance(100L));
    }

    @Test
    void recordShouldInsertCreditRecordWithFields() {
        creditBillingService.record(100L, -5, "ai-paper", "AI生成试卷", 95);

        ArgumentCaptor<CreditRecord> captor = ArgumentCaptor.forClass(CreditRecord.class);
        verify(creditRecordMapper).insert(captor.capture());
        CreditRecord record = captor.getValue();
        assertEquals(100L, record.getUserId());
        assertEquals(-5, record.getChangeAmount());
        assertEquals("ai-paper", record.getType());
        assertEquals("AI生成试卷", record.getSource());
        assertEquals(95, record.getBalance());
    }
}
