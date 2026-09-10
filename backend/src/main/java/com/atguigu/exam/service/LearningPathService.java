package com.atguigu.exam.service;

import com.atguigu.exam.vo.LearningPathDetailVo;

/**
 * AI 学习路径服务
 */
public interface LearningPathService {

    /**
     * 发起 AI 学习路径生成（异步）：立即返回 status=GENERATING 的路径，
     * LLM 编排在后台线程执行，完成后该行转 ACTIVE；失败转 FAILED（旧 ACTIVE 不受影响）。
     * 已有 GENERATING 任务时幂等返回，不重复提交。
     *
     * @param userId 用户ID
     * @return 路径详情（status=GENERATING）
     */
    LearningPathDetailVo generatePath(Long userId);

    /**
     * 查询用户当前路径：优先 ACTIVE，其次 GENERATING / FAILED（供前端轮询展示）。
     * 读取时惰性同步 PAPER 节点完成状态（该卷存在已批阅考试记录→自动置完成），
     * 全部节点完成时路径自动置 COMPLETED。
     *
     * @param userId 用户ID
     * @return 无任何路径时返回 null
     */
    LearningPathDetailVo getActivePath(Long userId);

    /**
     * 手动切换节点完成状态（KNOWLEDGE/QUESTION 节点；PAPER 节点由系统自动判定，拒绝手动切换）。
     * 切换后重算路径完成状态（全节点完成→COMPLETED，否则 ACTIVE）。
     *
     * @param userId 当前用户（校验节点归属）
     * @param nodeId 节点ID
     */
    void toggleNode(Long userId, Long nodeId);

    /**
     * 一键生成综合诊断卷（冷启动引导）：从题库各知识点分类混合随机抽取约 10 道现成题，
     * 组卷 DRAFT 并归属当前用户（不出 LLM 题，保证判卷后诊断数据落在多个真实分类）。
     *
     * @param userId 用户ID
     * @return 试卷ID（前端跳 /exam/start/{paperId}）
     */
    Long generateDiagnosticPaper(Long userId);
}
