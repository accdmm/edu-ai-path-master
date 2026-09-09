package com.atguigu.exam.mapper;

import com.atguigu.exam.entity.UserPaper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-试卷关联 Mapper
 */
@Mapper
public interface UserPaperMapper extends BaseMapper<UserPaper> {
}