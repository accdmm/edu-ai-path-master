package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.UserCreditService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户积分服务实现
 */
@Slf4j
@Service
public class UserCreditServiceImpl implements UserCreditService {

    @Autowired
    private UserCreditMapper userCreditMapper;
    @Autowired
    private CreditRecordMapper creditRecordMapper;

    @Override
    public Result<Map<String, Object>> getUserCredit(Long userId) {
        try {
            UserCredit credit = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("totalCredits", credit == null ? 0 : credit.getTotalCredits());
            data.put("activeCredits", credit == null ? 0 : credit.getActiveCredits());
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询用户积分失败", e);
            return Result.error("查询用户积分失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> getCreditRecords(Long userId, Integer page, Integer size) {
        try {
            LambdaQueryWrapper<CreditRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CreditRecord::getUserId, userId)
                    .orderByDesc(CreditRecord::getCreateTime);
            Page<CreditRecord> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<CreditRecord> result = creditRecordMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("changeAmount", r.getChangeAmount());
                m.put("type", r.getType());
                m.put("source", r.getSource());
                m.put("balance", r.getBalance());
                m.put("createTime", r.getCreateTime());
                return m;
            });
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询积分流水失败", e);
            return Result.error("查询积分流水失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getCreditRanking(Integer limit) {
        try {
            LambdaQueryWrapper<UserCredit> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(UserCredit::getTotalCredits)
                    .last("LIMIT " + (limit == null ? 10 : limit));
            List<UserCredit> list = userCreditMapper.selectList(wrapper);
            List<Map<String, Object>> data = list.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", c.getUserId());
                m.put("totalCredits", c.getTotalCredits());
                return m;
            }).toList();
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询积分排行失败", e);
            return Result.error("查询积分排行失败");
        }
    }
}