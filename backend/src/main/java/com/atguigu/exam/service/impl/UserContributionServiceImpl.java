package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.UserContribution;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.UserContributionMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.UserContributionService;
import com.atguigu.exam.vo.ContributionReviewVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户上传真题审核服务实现
 */
@Slf4j
@Service
public class UserContributionServiceImpl implements UserContributionService {

    @Autowired
    private UserContributionMapper contributionMapper;

    @Autowired
    private UserCreditMapper userCreditMapper;

    @Autowired
    private CreditRecordMapper creditRecordMapper;

    /** 真题贡献被采纳的积分奖励（仅首次采纳发放，拒绝/重复审核不重复发） */
    private static final int CONTRIBUTION_REWARD = 20;

    @Override
    public Result<Map<String, Object>> submitContribution(Long userId, String content, List<String> imageUrls, String contact) {
        try {
            if (content == null || content.isBlank()) {
                return Result.error("题目内容不能为空");
            }
            UserContribution uc = new UserContribution();
            uc.setUserId(userId);
            uc.setContent(content);
            uc.setImageUrls(imageUrls == null || imageUrls.isEmpty() ? "[]" : JSON.toJSONString(imageUrls));
            uc.setContact(contact);
            uc.setStatus(0);
            uc.setCreatedTime(new Date());
            contributionMapper.insert(uc);

            Map<String, Object> data = new HashMap<>();
            data.put("id", uc.getId());
            data.put("status", 0);
            return Result.success(data, "提交成功，等待审核");
        } catch (Exception e) {
            log.error("用户上传真题失败", e);
            return Result.error("用户上传真题失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> getMyContributions(Long userId, Integer page, Integer size) {
        try {
            LambdaQueryWrapper<UserContribution> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserContribution::getUserId, userId)
                    .orderByDesc(UserContribution::getCreatedTime);
            Page<UserContribution> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<UserContribution> result = contributionMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(this::toView);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询我的上传列表失败", e);
            return Result.error("查询我的上传列表失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> getPendingContributions(Integer page, Integer size, Integer status) {
        try {
            LambdaQueryWrapper<UserContribution> wrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                wrapper.eq(UserContribution::getStatus, status);
            }
            wrapper.orderByAsc(UserContribution::getStatus)
                    .orderByDesc(UserContribution::getCreatedTime);
            Page<UserContribution> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<UserContribution> result = contributionMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(this::toView);
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询待审核列表失败", e);
            return Result.error("查询待审核列表失败");
        }
    }

    @Override
    public Result<Void> reviewContribution(ContributionReviewVo vo) {
        try {
            if (vo == null || vo.getId() == null) {
                return Result.error("记录ID不能为空");
            }
            UserContribution uc = contributionMapper.selectById(vo.getId());
            if (uc == null) {
                return Result.error(404, "记录不存在");
            }
            // 仅“待审(0) → 采纳(1)”发放奖励，重复审核/拒绝不重复发积分
            boolean firstApproval = vo.getStatus() != null && vo.getStatus() == 1
                    && (uc.getStatus() == null || uc.getStatus() == 0);
            uc.setStatus(vo.getStatus());
            uc.setAdminRemark(vo.getAdminRemark());
            uc.setReviewedTime(new Date());
            contributionMapper.updateById(uc);
            if (firstApproval) {
                grantContributionReward(uc.getUserId());
            }
            String msg = vo.getStatus() != null && vo.getStatus() == 1 ? "已采纳" : "已拒绝";
            return Result.success(null, msg);
        } catch (Exception e) {
            log.error("审核真题失败", e);
            return Result.error("审核真题失败");
        }
    }

    /**
     * 贡献采纳奖励：条件原子加积分 + 写流水
     */
    private void grantContributionReward(Long userId) {
        if (userId == null) {
            return;
        }
        int updated = userCreditMapper.update(null, new UpdateWrapper<UserCredit>()
                .eq("user_id", userId)
                .setSql("active_credits = active_credits + " + CONTRIBUTION_REWARD)
                .setSql("total_credits = total_credits + " + CONTRIBUTION_REWARD)
                .setSql("update_time = NOW()"));
        if (updated == 0) {
            // 用户还没有积分账户：初始化后再发
            UserCredit credit = new UserCredit();
            credit.setUserId(userId);
            credit.setTotalCredits(CONTRIBUTION_REWARD);
            credit.setActiveCredits(CONTRIBUTION_REWARD);
            credit.setCreateTime(new Date());
            credit.setUpdateTime(new Date());
            userCreditMapper.insert(credit);
        }
        int balance;
        UserCredit latest = userCreditMapper.selectOne(
                new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
        balance = latest == null || latest.getActiveCredits() == null ? 0 : latest.getActiveCredits();
        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeAmount(CONTRIBUTION_REWARD);
        record.setType("contribution");
        record.setSource("真题贡献被采纳");
        record.setBalance(balance);
        record.setCreateTime(new Date());
        creditRecordMapper.insert(record);
    }

    private Map<String, Object> toView(UserContribution uc) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", uc.getId());
        m.put("userId", uc.getUserId());
        m.put("content", uc.getContent());
        m.put("images", new ArrayList<>());
        try {
            if (uc.getImageUrls() != null && !uc.getImageUrls().isBlank()) {
                m.put("images", JSON.parseArray(uc.getImageUrls(), String.class));
            }
        } catch (Exception ignored) {
        }
        m.put("contact", uc.getContact());
        m.put("status", uc.getStatus());
        m.put("adminRemark", uc.getAdminRemark());
        m.put("createdTime", uc.getCreatedTime());
        m.put("reviewedTime", uc.getReviewedTime());
        return m;
    }
}