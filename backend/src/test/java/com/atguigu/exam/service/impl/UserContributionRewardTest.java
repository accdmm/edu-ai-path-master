package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.UserContribution;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.UserContributionMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.vo.ContributionReviewVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 真题贡献采纳奖励单元测试：仅首次采纳发积分，拒绝/重复审核不重复发
 */
@ExtendWith(MockitoExtension.class)
class UserContributionRewardTest {

    @Mock
    private UserContributionMapper contributionMapper;

    @Mock
    private UserCreditMapper userCreditMapper;

    @Mock
    private CreditRecordMapper creditRecordMapper;

    @InjectMocks
    private UserContributionServiceImpl contributionService;

    private UserContribution pendingContribution() {
        UserContribution uc = new UserContribution();
        uc.setUserId(100L);
        uc.setStatus(0);
        return uc;
    }

    private ContributionReviewVo review(Integer status) {
        ContributionReviewVo vo = new ContributionReviewVo();
        vo.setId(1L);
        vo.setStatus(status);
        return vo;
    }

    @Test
    void firstApprovalShouldGrantReward() {
        when(contributionMapper.selectById(1L)).thenReturn(pendingContribution());
        when(userCreditMapper.update(any(), any())).thenReturn(1);
        UserCredit latest = new UserCredit();
        latest.setActiveCredits(120);
        when(userCreditMapper.selectOne(any())).thenReturn(latest);

        contributionService.reviewContribution(review(1));

        verify(userCreditMapper).update(any(), any());
        ArgumentCaptor<CreditRecord> captor = ArgumentCaptor.forClass(CreditRecord.class);
        verify(creditRecordMapper).insert(captor.capture());
        assertEquals(20, captor.getValue().getChangeAmount());
        assertEquals("contribution", captor.getValue().getType());
        assertEquals(120, captor.getValue().getBalance());
    }

    @Test
    void reApprovalShouldNotDoubleReward() {
        UserContribution approved = pendingContribution();
        approved.setStatus(1);
        when(contributionMapper.selectById(1L)).thenReturn(approved);

        contributionService.reviewContribution(review(1));

        verify(userCreditMapper, never()).update(any(), any());
        verify(creditRecordMapper, never()).insert(any(CreditRecord.class));
    }

    @Test
    void rejectionShouldNotGrantReward() {
        when(contributionMapper.selectById(1L)).thenReturn(pendingContribution());

        contributionService.reviewContribution(review(2));

        verify(userCreditMapper, never()).update(any(), any());
        verify(creditRecordMapper, never()).insert(any(CreditRecord.class));
    }
}
