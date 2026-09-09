package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.InterviewCompany;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.InterviewQuestionCategory;
import com.atguigu.exam.vo.InterviewQuestionManageQueryVo;
import com.atguigu.exam.vo.InterviewQuestionSaveVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 管理端内容服务：真题 CRUD、企业、题集分类
 */
public interface AdminContentService {

    /**
     * 管理端分页查询真题（含企业名/分类名）
     */
    Result<IPage<Map<String, Object>>> manageQuestionPage(InterviewQuestionManageQueryVo query);

    /**
     * 新增/编辑真题（管理端）
     */
    Result<Map<String, Object>> saveQuestion(InterviewQuestionSaveVo vo);

    /**
     * 删除真题（逻辑删）
     */
    Result<Void> deleteQuestion(Long id);

    /**
     * 企业列表（管理端下拉）
     */
    Result<List<Map<String, Object>>> listCompanies();

    /**
     * 题集分类列表（按企业筛选）
     */
    Result<List<Map<String, Object>>> listCategories(Long companyId);

    /**
     * 保留：兼容旧实体字段映射
     */
    InterviewQuestion toEntity(InterviewQuestionSaveVo vo);
}