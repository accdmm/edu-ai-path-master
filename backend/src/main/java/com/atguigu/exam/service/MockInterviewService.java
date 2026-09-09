package com.atguigu.exam.service;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.vo.MockInterviewCompleteVo;
import com.atguigu.exam.vo.MockInterviewDetailVo;
import com.atguigu.exam.vo.MockInterviewStartResponseVo;
import com.atguigu.exam.vo.MockInterviewStartVo;
import com.atguigu.exam.vo.MockInterviewSubmitAnswerVo;
import com.atguigu.exam.vo.InterviewResultVo;
import com.atguigu.exam.vo.PracticeScoreVo;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * AI 模拟面试服务接口
 */
public interface MockInterviewService {

    /**
     * 开始一次模拟面试：按方向/难度抽题，建立面试记录
     */
    Result<MockInterviewStartResponseVo> startMockInterview(Long userId, MockInterviewStartVo vo);

    /**
     * 提交单题答案：有 interviewRecordId 走面试模式（存答案评分），否则单题练习
     */
    Result<Object> submitAnswer(Long userId, MockInterviewSubmitAnswerVo vo);

    /**
     * 完成面试：汇总得分、生成面试官总结
     */
    Result<MockInterviewCompleteVo> completeMockInterview(Long userId, Long interviewId);

    /**
     * 面试详情（含逐题答题记录）
     */
    Result<MockInterviewDetailVo> getMockInterviewDetail(Long userId, Long interviewId);

    /**
     * 面试结果（含总结、雷达图、学习建议）
     */
    Result<InterviewResultVo> getInterviewResult(Long userId, Long interviewId);

    /**
     * 我的面试记录（分页）
     */
    Result<IPage<Map<String, Object>>> getMyInterviews(Long userId, Integer page, Integer size);

    /**
     * 企业下拉（用于题目管理）
     */
    Result<List<Map<String, Object>>> listCompanies();

    /**
     * 企业后端续接口：企业详情
     */
    Result<Map<String, Object>> getCompanyDetail(Long companyId);

    /**
     * 企业题集分组（公司详情页用）
     */
    Result<List<Map<String, Object>>> getCompanyClusters(Long companyId);

    /**
     * 企业真题（公司详情页用）
     */
    Result<List<Map<String, Object>>> getCompanyQuestions(Long companyId, String direction);
}