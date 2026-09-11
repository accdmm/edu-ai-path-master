package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.MockInterview;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.mapper.InterviewCompanyMapper;
import com.atguigu.exam.mapper.InterviewQuestionCategoryMapper;
import com.atguigu.exam.mapper.MockInterviewAnswerMapper;
import com.atguigu.exam.mapper.MockInterviewMapper;
import com.atguigu.exam.service.CreditBillingService;
import com.atguigu.exam.service.UserDiagnosisService;
import com.atguigu.exam.service.MockInterviewAiService;
import com.atguigu.exam.vo.MockInterviewStartResponseVo;
import com.atguigu.exam.vo.MockInterviewStartVo;
import com.atguigu.exam.common.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 模拟面试开考计费单元测试：积分不足拒绝开考且不产生垃圾记录
 */
@ExtendWith(MockitoExtension.class)
class MockInterviewStartBillingTest {

    @Mock
    private MockInterviewMapper interviewMapper;

    @Mock
    private MockInterviewAnswerMapper answerMapper;

    @Mock
    private InterviewQuestionMapper questionMapper;

    @Mock
    private InterviewCompanyMapper companyMapper;

    @Mock
    private InterviewQuestionCategoryMapper categoryMapper;

    @Mock
    private MockInterviewAiService mockInterviewAiService;

    @Mock
    private UserDiagnosisService userDiagnosisService;

    @Mock
    private CreditBillingService creditBillingService;

    @InjectMocks
    private MockInterviewServiceImpl mockInterviewService;

    private InterviewQuestion question(long id, String content) {
        InterviewQuestion q = new InterviewQuestion();
        q.setId(id);
        q.setDirection("java");
        q.setDifficultyLevel("medium");
        q.setQuestionContent(content);
        q.setStatus("approved");
        return q;
    }

    private MockInterviewStartVo startVo() {
        return new MockInterviewStartVo();
    }

    @Test
    void startShouldRejectWith4002WhenCreditsNotEnough() {
        when(questionMapper.selectList(any())).thenReturn(Arrays.asList(
                question(1, "什么是 JVM"),
                question(2, "什么是线程池"),
                question(3, "什么是事务")));
        when(creditBillingService.deductIfEnough(eq(100L), eq(10))).thenReturn(false);

        Result<MockInterviewStartResponseVo> result = mockInterviewService.startMockInterview(100L, startVo());

        assertEquals(4002, result.getCode().intValue());
        verify(interviewMapper, never()).insert(any(MockInterview.class));
        verify(creditBillingService, never()).record(any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void startShouldChargeAndCreateRecordWhenCreditsEnough() {
        when(questionMapper.selectList(any())).thenReturn(Arrays.asList(
                question(1, "什么是 JVM"),
                question(2, "什么是线程池"),
                question(3, "什么是事务")));
        when(creditBillingService.deductIfEnough(eq(100L), eq(10))).thenReturn(true);
        when(creditBillingService.currentBalance(eq(100L))).thenReturn(90);

        Result<MockInterviewStartResponseVo> result = mockInterviewService.startMockInterview(100L, startVo());

        assertEquals(200, result.getCode().intValue());
        assertEquals(3, result.getData().getQuestions().size());
        verify(interviewMapper).insert(any(MockInterview.class));
        verify(creditBillingService).record(eq(100L), eq(-10), eq("mock-interview"), eq("AI模拟面试"), eq(90));
    }
}
