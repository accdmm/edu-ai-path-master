package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.vo.InviteCodeActivateVo;
import com.atguigu.exam.vo.InviteCodeGenerateVo;
import com.atguigu.exam.vo.InviteCodeRequestVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 邀请码服务接口
 */
public interface InviteCodeService {

    /**
     * 生成邀请码（管理端）
     */
    Result<List<String>> generateCodes(InviteCodeGenerateVo vo);

    /**
     * 激活邀请码：校验 + 发放积分
     */
    Result<Map<String, Object>> activate(Long userId, InviteCodeActivateVo vo);

    /**
     * 分页查询邀请码（管理端）
     */
    Result<IPage<Map<String, Object>>> listCodes(Integer page, Integer size, String status, String type);

    /**
     * 被邀请用户列表（管理端）
     */
    Result<IPage<Map<String, Object>>> listInvitees(Integer page, Integer size);

    /**
     * 删除邀请码
     */
    Result<Void> deleteCode(Long id);

    /**
     * 申请邀请码（未开通，占位返回）
     */
    Result<Void> requestCode(InviteCodeRequestVo vo);
}