package com.atguigu.exam.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.InviteCode;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.InviteCodeMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.InviteCodeService;
import com.atguigu.exam.vo.InviteCodeActivateVo;
import com.atguigu.exam.vo.InviteCodeGenerateVo;
import com.atguigu.exam.vo.InviteCodeRequestVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邀请码服务实现
 */
@Slf4j
@Service
public class InviteCodeServiceImpl implements InviteCodeService {

    private static final int ACTIVATE_CREDITS = 100;

    @Autowired
    private InviteCodeMapper inviteCodeMapper;
    @Autowired
    private UserCreditMapper userCreditMapper;
    @Autowired
    private CreditRecordMapper creditRecordMapper;

    @Override
    public Result<List<String>> generateCodes(InviteCodeGenerateVo vo) {
        try {
            int count = vo.getCount() == null ? 1 : Math.min(50, Math.max(1, vo.getCount()));
            String type = vo.getType() == null ? "normal" : vo.getType();
            Date now = new Date();

            List<String> codes = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String code = "EDU" + IdUtil.simpleUUID().substring(0, 8).toUpperCase();
                InviteCode ic = new InviteCode();
                ic.setCode(code);
                ic.setType(type);
                ic.setStatus("unused");
                ic.setCreateTime(now);
                inviteCodeMapper.insert(ic);
                codes.add(code);
            }
            return Result.success(codes, "生成成功");
        } catch (Exception e) {
            log.error("生成邀请码失败", e);
            return Result.error("生成邀请码失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> activate(Long userId, InviteCodeActivateVo vo) {
        try {
            String code = vo.getCode();
            if (code == null || code.isBlank()) {
                return Result.error("邀请码不能为空");
            }
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            InviteCode ic = inviteCodeMapper.selectOne(
                    new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode, code.trim().toUpperCase()));
            if (ic == null) {
                return Result.error("邀请码不存在");
            }
            if ("used".equals(ic.getStatus())) {
                return Result.error("邀请码已被使用");
            }
            if (ic.getExpireTime() != null && ic.getExpireTime().before(new Date())) {
                ic.setStatus("expired");
                inviteCodeMapper.updateById(ic);
                return Result.error("邀请码已过期");
            }

            // 校验当前用户是否已激活过
            boolean alreadyActivated = inviteCodeMapper.selectCount(
                    new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getActivedBy, userId)) > 0;
            if (alreadyActivated && !"demo".equals(ic.getType())) {
                return Result.error("该账号已激活过邀请码");
            }

            // 标记已使用
            ic.setStatus("used");
            ic.setActivedBy(userId);
            ic.setActivedAt(new Date());
            inviteCodeMapper.updateById(ic);

            // 发放积分
            UserCredit credit = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
            if (credit == null) {
                credit = new UserCredit();
                credit.setUserId(userId);
                credit.setTotalCredits(0);
                credit.setActiveCredits(0);
                credit.setCreateTime(new Date());
                credit.setUpdateTime(new Date());
                userCreditMapper.insert(credit);
            }
            int bonus = "enterprise".equals(ic.getType()) ? 300 : ACTIVATE_CREDITS;
            credit.setActiveCredits(credit.getActiveCredits() == null ? 0 : credit.getActiveCredits() + bonus);
            credit.setTotalCredits(credit.getTotalCredits() == null ? 0 : credit.getTotalCredits() + bonus);
            credit.setUpdateTime(new Date());
            userCreditMapper.updateById(credit);

            CreditRecord record = new CreditRecord();
            record.setUserId(userId);
            record.setChangeAmount(bonus);
            record.setType("invite");
            record.setSource("激活邀请码 " + ic.getCode());
            record.setBalance(credit.getActiveCredits());
            record.setCreateTime(new Date());
            creditRecordMapper.insert(record);

            Map<String, Object> data = new HashMap<>();
            data.put("code", ic.getCode());
            data.put("type", ic.getType());
            data.put("bonus", bonus);
            data.put("activeCredits", credit.getActiveCredits());
            return Result.success(data, "激活成功");
        } catch (Exception e) {
            log.error("激活邀请码失败", e);
            throw e;
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> listCodes(Integer page, Integer size, String status, String type) {
        try {
            LambdaQueryWrapper<InviteCode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(status != null && !status.isBlank(), InviteCode::getStatus, status)
                    .eq(type != null && !type.isBlank(), InviteCode::getType, type)
                    .orderByDesc(InviteCode::getCreateTime);
            Page<InviteCode> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<InviteCode> result = inviteCodeMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("code", c.getCode());
                m.put("type", c.getType());
                m.put("status", c.getStatus());
                m.put("activedBy", c.getActivedBy());
                m.put("activedAt", c.getActivedAt());
                m.put("expireTime", c.getExpireTime());
                m.put("createTime", c.getCreateTime());
                return m;
            });
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询邀请码列表失败", e);
            return Result.error("查询邀请码列表失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> listInvitees(Integer page, Integer size) {
        try {
            List<InviteCode> usedCodes = inviteCodeMapper.selectList(
                    new LambdaQueryWrapper<InviteCode>()
                            .eq(InviteCode::getStatus, "used")
                            .orderByDesc(InviteCode::getActivedAt));
            List<Map<String, Object>> rows = usedCodes.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", c.getActivedBy());
                m.put("code", c.getCode());
                m.put("type", c.getType());
                m.put("activedAt", c.getActivedAt());
                return m;
            }).toList();

            int total = rows.size();
            int start = (page == null ? 1 : page - 1) * (size == null ? 10 : size);
            int end = Math.min(start + (size == null ? 10 : size), total);
            Page<Map<String, Object>> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size, total);
            pg.setRecords(start >= total ? new ArrayList<>() : new ArrayList<>(rows.subList(start, end)));
            return Result.success(pg);
        } catch (Exception e) {
            log.error("查询被邀请用户失败", e);
            return Result.error("查询被邀请用户失败");
        }
    }

    @Override
    public Result<Void> deleteCode(Long id) {
        try {
            inviteCodeMapper.deleteById(id);
            return Result.success(null, "删除成功");
        } catch (Exception e) {
            log.error("删除邀请码失败", e);
            return Result.error("删除邀请码失败");
        }
    }

    @Override
    public Result<Void> requestCode(InviteCodeRequestVo vo) {
        // 未接入审批流，占位返回提示
        return Result.error("邀请码申请功能未开通，请联系管理员获取");
    }
}