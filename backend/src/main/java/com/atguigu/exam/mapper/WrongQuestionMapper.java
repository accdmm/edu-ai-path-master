package com.atguigu.exam.mapper;

import com.atguigu.exam.entity.WrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 错题本 Mapper
 */
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 物理删除指定错题（绕过逻辑删除）。
     * 原因：唯一键 uk_user_question(user_id, question_id) 不含 is_deleted，
     * 逻辑删除的残留行会让同一道题重新答错时的新插入撞唯一键而收录失败
     */
    @Delete("DELETE FROM wrong_question WHERE id = #{id} AND user_id = #{userId}")
    int purgeOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 物理清理"用户 + 题目"维度的残留行（含历史逻辑删除数据），供重新收录前调用：
     * 无残留时为无害空操作，有残留时先物理清除再插入，保证错题可被重新收录
     */
    @Delete("DELETE FROM wrong_question WHERE user_id = #{userId} AND question_id = #{questionId}")
    int purgeByUserAndQuestion(@Param("userId") Long userId, @Param("questionId") Long questionId);
}