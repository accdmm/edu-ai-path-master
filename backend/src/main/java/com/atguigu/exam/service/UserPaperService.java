package com.atguigu.exam.service;

import com.atguigu.exam.entity.UserPaper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户-试卷关联服务接口 - 记录用户私有试卷（AI 生成卷归属）
 */
public interface UserPaperService extends IService<UserPaper> {

    /**
     * 查询用户的全部私有试卷关联（含 paper 信息）
     *
     * @param userId 用户ID
     * @return 关联列表，按创建时间倒序
     */
    List<UserPaper> listByUserId(Long userId);

    /**
     * 判断用户是否有权访问指定试卷
     *
     * @param userId  用户ID
     * @param paperId 试卷ID
     * @return true 表示有权访问
     */
    boolean existRelation(Long userId, Long paperId);
}