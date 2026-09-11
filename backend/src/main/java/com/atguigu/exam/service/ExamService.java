package com.atguigu.exam.service;

import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.vo.ExamRankingVO;
import com.atguigu.exam.vo.StartExamVo;
import com.atguigu.exam.vo.SubmitAnswerVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {

    /**
     * 开始考试业务
     * @param startExamVo
     * @return
     */
    ExamRecord startExam(StartExamVo startExamVo);

    /**
     * 获取考试记录详情
     * @param id
     * @return
     */
    ExamRecord customGetExamRecordById(Integer id);

    /**
     * 提交考试答案
     * @param examRecordId
     * @param answers
     */
    void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;

    /**
     * ai试卷批阅功能
     * @param examRecordId
     * @return
     */
    ExamRecord gradeExam(Integer examRecordId) throws InterruptedException;

    /**
     * 删除考试记录
     * @param id
     */
    void customRemoveById(Integer id);

    /**
     * 查询排行榜业务
     * @param paperId
     * @param limit
     * @return
     */
    List<ExamRankingVO> customGetRanking(Integer paperId, Integer limit);

    /**
     * 查询当前用户的考试记录列表（按开始时间倒序）
     */
    List<ExamRecord> customGetMyRecords(Long userId);

    /**
     * 强制结算：按已保存的答题记录统计总分并置"已批阅"（失败简答题按占位 0 分计）。
     * 用于 AI 判卷多次重试仍失败时的兜底，避免成绩永久卡在"判卷中"。
     *
     * @return 强制结算后的总分
     */
    int forceSettleGrading(Integer examRecordId);
}
 