package com.atguigu.exam.service;

import com.atguigu.exam.vo.LearningPathDetailVo;

import java.util.List;

/**
 * 用户知识点掌握度诊断服务（学习路径、个性化面试等共用）
 * 口径：已批阅考试(score 非空) → 答题记录 → 按题目分类聚合得分率，返回按得分率升序（最薄弱在前）
 */
public interface UserDiagnosisService {

    /**
     * 聚合用户知识点诊断数据
     *
     * @param userId 用户ID
     * @return 诊断项列表（correctRate 升序=最薄弱在前）；无答题数据返回空列表
     */
    List<LearningPathDetailVo.DiagnosisItemVo> buildDiagnosis(Long userId);
}
