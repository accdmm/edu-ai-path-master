package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.CreditBillingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 积分计费服务实现
 * 扣费使用条件原子 UPDATE（WHERE active_credits >= cost）防止超扣，
 * 与全项目 AI 面试/判卷/解析的既有扣费写法保持一致
 */
@Slf4j
@Service
public class CreditBillingServiceImpl implements CreditBillingService {

    @Autowired
    private UserCreditMapper userCreditMapper;

    @Autowired
    private CreditRecordMapper creditRecordMapper;

    @Override
    public boolean deductIfEnough(Long userId, int cost) {
        if (userId == null || cost <= 0) {
            return false;
        }
        int updated = userCreditMapper.update(null, new UpdateWrapper<UserCredit>()
                .eq("user_id", userId)
                .ge("active_credits", cost)
                .setSql("active_credits = active_credits - " + cost)
                .setSql("update_time = NOW()"));
        return updated > 0;
    }

    @Override
    public int currentBalance(Long userId) {
        if (userId == null) {
            return 0;
        }
        UserCredit credit = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
        return credit == null || credit.getActiveCredits() == null ? 0 : credit.getActiveCredits();
    }

    @Override
    public void record(Long userId, int changeAmount, String type, String source, int balance) {
        try {
            CreditRecord record = new CreditRecord();
            record.setUserId(userId);
            record.setChangeAmount(changeAmount);
            record.setType(type);
            record.setSource(source);
            record.setBalance(balance);
            record.setCreateTime(new Date());
            creditRecordMapper.insert(record);
        } catch (Exception e) {
            // 流水失败不影响主业务（积分已变动），仅记录日志供人工对账
            log.error("写入积分流水失败 userId={} change={} type={}", userId, changeAmount, type, e);
        }
    }
}
