package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 用户积分服务接口
 */
public interface UserCreditService {

    /**
     * 查询用户积分
     */
    Result<Map<String, Object>> getUserCredit(Long userId);

    /**
     * 积分流水（分页）
     */
    Result<IPage<Map<String, Object>>> getCreditRecords(Long userId, Integer page, Integer size);

    /**
     * 积分排行（排行榜页用）
     */
    Result<List<Map<String, Object>>> getCreditRanking(Integer limit);
}