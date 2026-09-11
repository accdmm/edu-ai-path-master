package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.UserPaper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.mapper.UserPaperMapper;
import com.atguigu.exam.service.UserPaperService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户-试卷关联服务实现类
 */
@Slf4j
@Service
public class UserPaperServiceImpl extends ServiceImpl<UserPaperMapper, UserPaper> implements UserPaperService {

    public static final String RELATION_AI_GENERATED = "AI_GENERATED";

    /** 手动组卷归属 */
    public static final String RELATION_MANUAL = "MANUAL";

    @Autowired
    private PaperMapper paperMapper;

    @Override
    public List<UserPaper> listByUserId(Long userId) {
        LambdaQueryWrapper<UserPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaper::getUserId, userId);
        wrapper.orderByDesc(UserPaper::getCreateTime);
        List<UserPaper> userPapers = list(wrapper);
        // 填充 paper 信息
        for (UserPaper userPaper : userPapers) {
            try {
                userPaper.setPaper(paperMapper.selectById(userPaper.getPaperId()));
            } catch (Exception e) {
                log.debug("加载用户 {} 试卷 {} 信息失败", userId, userPaper.getPaperId(), e);
            }
        }
        return userPapers;
    }

    @Override
    public boolean existRelation(Long userId, Long paperId) {
        if (userId == null || paperId == null) {
            return false;
        }
        LambdaQueryWrapper<UserPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaper::getUserId, userId);
        wrapper.eq(UserPaper::getPaperId, paperId);
        return count(wrapper) > 0;
    }
}