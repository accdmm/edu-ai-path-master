package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.vo.ContributionReviewVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 用户上传真题审核服务接口
 */
public interface UserContributionService {

    /**
     * 用户上传真题
     */
    Result<Map<String, Object>> submitContribution(Long userId, String content, List<String> imageUrls, String contact);

    /**
     * 我的上传记录（分页）
     */
    Result<IPage<Map<String, Object>>> getMyContributions(Long userId, Integer page, Integer size);

    /**
     * 待审核列表（管理端）
     */
    Result<IPage<Map<String, Object>>> getPendingContributions(Integer page, Integer size, Integer status);

    /**
     * 审核（管理端）
     */
    Result<Void> reviewContribution(ContributionReviewVo vo);
}