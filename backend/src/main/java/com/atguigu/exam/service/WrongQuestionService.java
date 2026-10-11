package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.WrongQuestion;
import com.atguigu.exam.vo.WrongQuestionStatsVo;
import com.atguigu.exam.vo.WrongQuestionVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 错题本服务
 */
public interface WrongQuestionService extends IService<WrongQuestion> {

    /** 连续答对达到该次数判定为已掌握 */
    int MASTER_STREAK = 2;

    /**
     * 判卷结算后同步错题本掌握度状态机：
     * <ul>
     *   <li>答错(is_correct=0)：已收录则 wrongCount+1、连续答对归零、状态回落 UNMASTERED（已掌握后再次答错同样回退）；未收录则自动收录</li>
     *   <li>部分正确(is_correct=2)：同上，状态为 LEARNING（半掌握）</li>
     *   <li>答对(is_correct=1)：仅回血已收录的错题——连续答对+1，达到 {@link #MASTER_STREAK} 次自动转为 MASTERED；
     *       未收录的答对题不入库</li>
     * </ul>
     * 幂等：依据 last_exam_record_id 判定本场考试该题是否已同步，判卷重跑/重复触发不会重复计数。
     *
     * @param userId        考试记录归属用户
     * @param examRecordId  本次考试记录ID
     * @param answerRecords 本次答题记录（已判分）
     * @param questionMap   题目ID → 题目 映射（取分类用）
     */
    void syncFromGrading(Long userId, Integer examRecordId,
                         List<AnswerRecord> answerRecords, Map<Long, Question> questionMap);

    /**
     * 手动切换掌握状态：未掌握/半掌握 → 已掌握；已掌握 → 回落到未掌握（可撤销误操作）。
     * 手动标记已掌握的题若之后再次答错，由判卷状态机自动回退为未掌握。
     *
     * @param userId 当前用户（校验错题归属）
     * @param id     错题记录ID
     */
    void toggleMaster(Long userId, Long id);

    /**
     * 移出错题本（物理删除）。该题之后若再次答错会被重新收录。
     *
     * @param userId 当前用户（校验错题归属）
     * @param id     错题记录ID
     */
    void removeWrongQuestion(Long userId, Long id);

    /**
     * 分页查询错题本列表（按最近答错时间倒序），并装配题目详情
     * （题干/选项/标准答案/解析/最近一次作答）
     *
     * @param userId     当前用户
     * @param page       页码（从1开始）
     * @param size       每页数量
     * @param categoryId 知识点分类筛选（可空）
     * @param status     掌握状态筛选 UNMASTERED/LEARNING/MASTERED（可空）
     */
    Page<WrongQuestionVo> pageWrongQuestions(Long userId, long page, long size, Long categoryId, String status);

    /**
     * 错题统计：总览（总/未掌握/半掌握/已掌握）+ 按知识点聚合（按错题数倒序）
     *
     * @param userId 当前用户
     */
    WrongQuestionStatsVo stats(Long userId);

    /**
     * AI 解析错题：讲解考点、解题步骤与常见误区。
     * 复用统一 AI 解析计费：每日 3 次免费，超出每次扣 5 积分（type=ai-analysis，与刷题/真题解析共用额度）。
     *
     * @param userId 当前用户（校验错题归属）
     * @param id     错题记录ID
     * @return 统一响应：{analysis, free, remainingFree, activeCredits}；积分不足返回 4002
     */
    Result<Map<String, Object>> aiAnalysis(Long userId, Long id);

    /**
     * 基于错题生成巩固练习卷：取未掌握/半掌握错题（原题）+ 同知识点强化题组卷，
     * 落库为归属当前用户的 DRAFT 试卷（不消耗积分），考完后判卷自动回写掌握度。
     *
     * @param userId 当前用户
     * @return 试卷ID（前端跳 /exam/start/{paperId}）；无可用错题时抛业务异常
     */
    Long generateConsolidationPaper(Long userId);
}