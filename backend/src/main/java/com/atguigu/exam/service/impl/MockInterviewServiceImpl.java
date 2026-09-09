package com.atguigu.exam.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.InterviewCompany;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.InterviewQuestionCategory;
import com.atguigu.exam.entity.MockInterview;
import com.atguigu.exam.entity.MockInterviewAnswer;
import com.atguigu.exam.mapper.InterviewCompanyMapper;
import com.atguigu.exam.mapper.InterviewQuestionCategoryMapper;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.mapper.MockInterviewAnswerMapper;
import com.atguigu.exam.mapper.MockInterviewMapper;
import com.atguigu.exam.service.MockInterviewAiService;
import com.atguigu.exam.service.MockInterviewService;
import com.atguigu.exam.vo.InterviewResultVo;
import com.atguigu.exam.vo.MockInterviewAnswerDetailVo;
import com.atguigu.exam.vo.MockInterviewCompleteVo;
import com.atguigu.exam.vo.MockInterviewDetailVo;
import com.atguigu.exam.vo.MockInterviewQuestionVo;
import com.atguigu.exam.vo.MockInterviewStartResponseVo;
import com.atguigu.exam.vo.MockInterviewStartVo;
import com.atguigu.exam.vo.MockInterviewSubmitAnswerVo;
import com.atguigu.exam.vo.PracticeScoreVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 模拟面试服务实现
 */
@Slf4j
@Service
public class MockInterviewServiceImpl implements MockInterviewService {

    @Autowired
    private MockInterviewMapper interviewMapper;
    @Autowired
    private MockInterviewAnswerMapper answerMapper;
    @Autowired
    private InterviewQuestionMapper questionMapper;
    @Autowired
    private InterviewCompanyMapper companyMapper;
    @Autowired
    private InterviewQuestionCategoryMapper categoryMapper;
    @Autowired
    private MockInterviewAiService mockInterviewAiService;

    private static final int MAX_QUESTIONS = 20;
    private static final int DEFAULT_DURATION = 60;

    @Override
    public Result<MockInterviewStartResponseVo> startMockInterview(Long userId, MockInterviewStartVo vo) {
        try {
            String direction = vo.getDirection() == null ? "java" : vo.getDirection();
            int questionCount = vo.getQuestionCount() == null ? 5 : vo.getQuestionCount();
            questionCount = Math.max(3, Math.min(MAX_QUESTIONS, questionCount));
            String difficulty = vo.getDifficulty() == null ? "medium" : vo.getDifficulty();
            String companyType = vo.getCompanyType() == null ? "large" : vo.getCompanyType();
            int duration = vo.getDuration() == null ? DEFAULT_DURATION : vo.getDuration();

            List<InterviewQuestion> questionPool = questionMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getStatus, "approved")
                            .eq(InterviewQuestion::getDirection, direction)
                            .eq(InterviewQuestion::getDifficultyLevel, difficulty)
                            .orderByDesc(InterviewQuestion::getViewCount));

            if (questionPool.size() < questionCount) {
                List<InterviewQuestion> sameDirection = questionMapper.selectList(
                        new LambdaQueryWrapper<InterviewQuestion>()
                                .eq(InterviewQuestion::getStatus, "approved")
                                .eq(InterviewQuestion::getDirection, direction)
                                .orderByDesc(InterviewQuestion::getViewCount));
                for (InterviewQuestion q : sameDirection) {
                    if (questionPool.size() >= questionCount) {
                        break;
                    }
                    boolean exists = questionPool.stream().anyMatch(p -> p.getId().equals(q.getId()));
                    if (!exists) {
                        questionPool.add(q);
                    }
                }
            }

            List<InterviewQuestion> picked = questionPool.subList(0, Math.min(questionCount, questionPool.size()));
            if (picked.isEmpty()) {
                return Result.error("该方向暂无可用题目，请先到企业真题库查看");
            }

            MockInterview record = new MockInterview();
            record.setUserId(userId);
            record.setDirection(direction);
            record.setQuestionCount(picked.size());
            record.setDifficulty(difficulty);
            record.setCompanyType(companyType);
            record.setDuration(duration);
            record.setStatus("in_progress");
            record.setTotalScore(0);
            record.setMaxScore(picked.size() * 100);
            record.setCompletedQuestions(0);
            record.setStartTime(new Date());
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            interviewMapper.insert(record);

            List<MockInterviewQuestionVo> questions = new ArrayList<>();
            for (InterviewQuestion q : picked) {
                MockInterviewQuestionVo qv = new MockInterviewQuestionVo();
                qv.setId(q.getId());
                qv.setDirection(q.getDirection());
                qv.setDifficulty(q.getDifficultyLevel());
                qv.setContent(q.getQuestionContent());
                qv.setTitle(truncate(q.getQuestionContent(), 50));
                questions.add(qv);
            }

            MockInterviewStartResponseVo responseVo = new MockInterviewStartResponseVo();
            responseVo.setId(record.getId());
            responseVo.setQuestions(questions);
            return Result.success(responseVo, "模拟面试已开始");
        } catch (Exception e) {
            log.error("开始模拟面试失败", e);
            return Result.error("开始模拟面试失败");
        }
    }

    @Override
    public Result<Object> submitAnswer(Long userId, MockInterviewSubmitAnswerVo vo) {
        try {
            if (vo.getQuestionId() == null) {
                return Result.error("题目ID不能为空");
            }
            InterviewQuestion q = questionMapper.selectById(vo.getQuestionId());
            if (q == null) {
                return Result.error(404, "题目不存在");
            }

            Map<String, Object> ai = mockInterviewAiService.gradeAnswer(
                    q.getDirection(), q.getQuestionContent(), q.getDifficultyLevel(), vo.getUserAnswer());

            if (vo.getInterviewRecordId() == null) {
                // 单题练习模式
                PracticeScoreVo practiceVo = new PracticeScoreVo();
                practiceVo.setScore((Integer) ai.get("score"));
                practiceVo.setComment((String) ai.get("comment"));
                return Result.success(practiceVo);
            }

            // 面试模式：保存答题记录
            Long interviewId = vo.getInterviewRecordId();
            MockInterview record = interviewMapper.selectById(interviewId);
            if (record == null) {
                return Result.error(404, "面试记录不存在");
            }
            if (!record.getUserId().equals(userId)) {
                return Result.error(403, "无权操作该面试记录");
            }
            if ("completed".equals(record.getStatus())) {
                return Result.error("该面试已完成，无法继续提交答案");
            }

            MockInterviewAnswer answer = new MockInterviewAnswer();
            answer.setInterviewId(interviewId);
            answer.setQuestionId(vo.getQuestionId());
            answer.setQuestionContent(q.getQuestionContent());
            answer.setDirection(q.getDirection());
            answer.setDifficultyLevel(q.getDifficultyLevel());
            answer.setUserAnswer(vo.getUserAnswer());
            answer.setVoiceFileUrl(vo.getVoiceFileUrl());
            answer.setAnswerTime(vo.getAnswerTime());
            answer.setScore((Integer) ai.get("score"));
            answer.setMaxScore(100);
            answer.setAiEvaluation((String) ai.get("comment"));
            answer.setTechnicalAccuracy((Integer) ai.get("technicalAccuracy"));
            answer.setClarity((Integer) ai.get("clarity"));
            answer.setLogic((Integer) ai.get("logic"));
            answer.setCreateTime(new Date());
            answerMapper.insert(answer);

            // 回写进度
            long answered = answerMapper.selectCount(new LambdaQueryWrapper<MockInterviewAnswer>()
                    .eq(MockInterviewAnswer::getInterviewId, interviewId));
            record.setCompletedQuestions(Math.toIntExact(answered));
            record.setUpdateTime(new Date());
            interviewMapper.updateById(record);

            return Result.success(ai, "答案已评分");
        } catch (Exception e) {
            log.error("提交答案失败", e);
            return Result.error("提交答案失败");
        }
    }

    @Override
    public Result<MockInterviewCompleteVo> completeMockInterview(Long userId, Long interviewId) {
        try {
            MockInterview record = interviewMapper.selectById(interviewId);
            if (record == null) {
                return Result.error(404, "面试记录不存在");
            }
            if (!record.getUserId().equals(userId)) {
                return Result.error(403, "无权操作该面试记录");
            }
            if ("completed".equals(record.getStatus())) {
                MockInterviewCompleteVo existing = buildCompleteVo(record);
                return Result.success(existing);
            }

            List<MockInterviewAnswer> answers = answerMapper.selectList(
                    new LambdaQueryWrapper<MockInterviewAnswer>()
                            .eq(MockInterviewAnswer::getInterviewId, interviewId));

            int totalScore = 0;
            for (MockInterviewAnswer a : answers) {
                totalScore += a.getScore() == null ? 0 : a.getScore();
            }
            record.setTotalScore(totalScore);
            record.setMaxScore(record.getQuestionCount() * 100);
            record.setAverageScore(answers.isEmpty()
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(1.0 * totalScore / answers.size()).setScale(2, RoundingMode.HALF_UP));
            record.setCompletedQuestions(answers.size());
            record.setStatus("completed");
            record.setEndTime(new Date());
            record.setUpdateTime(new Date());
            interviewMapper.updateById(record);

            // 生成面试官总结（若存在答题记录）
            if (!answers.isEmpty()) {
                try {
                    List<MockInterviewAnswerDetailVo> details = toDetailVos(answers);
                    Map<String, Object> summary = mockInterviewAiService.summarizeInterview(details);
                    record.setInterviewerSummary((String) summary.get("summary"));
                    record.setImprovementSuggestions(JSON.toJSONString(summary.get("improvements")));
                    interviewMapper.updateById(record);
                } catch (Exception e) {
                    log.warn("生成面试官总结失败，使用默认总结", e);
                    if (record.getInterviewerSummary() == null) {
                        record.setInterviewerSummary("本次模拟面试已完成，感谢参与！");
                    }
                    if (record.getImprovementSuggestions() == null) {
                        record.setImprovementSuggestions("[\"加强基础概念理解\",\"增加实战练习\"]");
                    }
                    interviewMapper.updateById(record);
                }
            }

            return Result.success(buildCompleteVo(record), "面试已完成");
        } catch (Exception e) {
            log.error("完成面试失败", e);
            return Result.error("完成面试失败");
        }
    }

    @Override
    public Result<MockInterviewDetailVo> getMockInterviewDetail(Long userId, Long interviewId) {
        try {
            MockInterview record = interviewMapper.selectById(interviewId);
            if (record == null) {
                return Result.error(404, "面试记录不存在");
            }
            if (!record.getUserId().equals(userId)) {
                return Result.error(403, "无权查看该面试记录");
            }
            return Result.success(buildDetailVo(record));
        } catch (Exception e) {
            log.error("查询面试详情失败", e);
            return Result.error("查询面试详情失败");
        }
    }

    @Override
    public Result<InterviewResultVo> getInterviewResult(Long userId, Long interviewId) {
        try {
            MockInterview record = interviewMapper.selectById(interviewId);
            if (record == null) {
                return Result.error(404, "面试记录不存在");
            }
            if (!record.getUserId().equals(userId)) {
                return Result.error(403, "无权查看该面试记录");
            }

            InterviewResultVo vo = new InterviewResultVo();
            vo.setId(record.getId());
            vo.setTotalScore(record.getTotalScore());
            vo.setMaxScore(record.getMaxScore());
            vo.setAverageScore(record.getAverageScore() == null ? 0.0 : record.getAverageScore().doubleValue());
            vo.setCompletedQuestions(record.getCompletedQuestions());
            vo.setDuration(record.getDuration());
            vo.setAnswers(buildDetailVo(record).getAnswers());

            Map<String, Object> feedback = new HashMap<>();
            feedback.put("summary", record.getInterviewerSummary() == null ? "" : record.getInterviewerSummary());
            List<String> improvements = new ArrayList<>();
            try {
                if (record.getImprovementSuggestions() != null && !record.getImprovementSuggestions().isBlank()) {
                    improvements = JSON.parseArray(record.getImprovementSuggestions(), String.class);
                }
            } catch (Exception ignored) {
            }
            feedback.put("improvements", improvements);
            vo.setInterviewerFeedback(feedback);

            List<Map<String, Object>> suggestions = new ArrayList<>();
            for (String imp : improvements) {
                Map<String, Object> s = new HashMap<>();
                s.put("title", imp);
                s.put("description", "建议针对该项加强针对性练习。");
                suggestions.add(s);
            }
            vo.setLearningSuggestions(suggestions);
            vo.setAbilityScores(new HashMap<>());
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询面试结果失败", e);
            return Result.error("查询面试结果失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> getMyInterviews(Long userId, Integer page, Integer size) {
        try {
            LambdaQueryWrapper<MockInterview> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MockInterview::getUserId, userId)
                    .orderByDesc(MockInterview::getCreateTime);
            Page<MockInterview> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<MockInterview> result = interviewMapper.selectPage(pg, wrapper);
            IPage<Map<String, Object>> data = result.convert(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("direction", r.getDirection());
                m.put("difficulty", r.getDifficulty());
                m.put("questionCount", r.getQuestionCount());
                m.put("status", r.getStatus());
                m.put("averageScore", r.getAverageScore() == null ? 0 : r.getAverageScore());
                m.put("completedQuestions", r.getCompletedQuestions());
                m.put("createTime", r.getCreateTime());
                return m;
            });
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询我的面试列表失败", e);
            return Result.error("查询我的面试列表失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> listCompanies() {
        try {
            List<InterviewCompany> companies = companyMapper.selectList(new LambdaQueryWrapper<InterviewCompany>()
                    .orderByDesc(InterviewCompany::getTotalQuestions));
            List<Map<String, Object>> list = companies.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("companyName", c.getName());
                m.put("companyLogo", c.getLogo());
                m.put("description", c.getDescription());
                m.put("isPremium", c.getIsPremium());
                m.put("totalQuestions", c.getTotalQuestions());
                return m;
            }).collect(Collectors.toList());
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询企业列表失败", e);
            return Result.error("查询企业列表失败");
        }
    }

    @Override
    public Result<Map<String, Object>> getCompanyDetail(Long companyId) {
        try {
            InterviewCompany c = companyMapper.selectById(companyId);
            if (c == null) {
                return Result.error(404, "企业不存在");
            }
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("companyName", c.getName());
            m.put("companyLogo", c.getLogo());
            m.put("description", c.getDescription());
            m.put("isPremium", c.getIsPremium());
            m.put("totalQuestions", c.getTotalQuestions());
            return Result.success(m);
        } catch (Exception e) {
            log.error("查询企业详情失败", e);
            return Result.error("查询企业详情失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getCompanyClusters(Long companyId) {
        try {
            List<InterviewQuestionCategory> categories = categoryMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestionCategory>()
                            .eq(InterviewQuestionCategory::getCompanyId, companyId)
                            .orderByAsc(InterviewQuestionCategory::getSort));
            List<Map<String, Object>> list = categories.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("direction", c.getDirection());
                m.put("difficulty", c.getDifficulty());
                m.put("year", c.getYear());
                m.put("round", c.getRound());
                m.put("questionCount", c.getQuestionCount());
                return m;
            }).collect(Collectors.toList());
            return Result.success(list);
        } catch (Exception e) {
            log.error("查询企业题集失败", e);
            return Result.error("查询企业题集失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getCompanyQuestions(Long companyId, String direction) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewQuestion::getCompanyId, companyId)
                    .eq(InterviewQuestion::getStatus, "approved")
                    .eq(direction != null && !direction.isBlank(), InterviewQuestion::getDirection, direction)
                    .orderByDesc(InterviewQuestion::getViewCount);
            List<InterviewQuestion> list = questionMapper.selectList(wrapper);
            List<Map<String, Object>> data = list.stream().map(q -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", q.getId());
                m.put("questionContent", q.getQuestionContent());
                m.put("direction", q.getDirection());
                m.put("difficultyLevel", q.getDifficultyLevel());
                m.put("interviewYear", q.getInterviewYear());
                m.put("viewCount", q.getViewCount());
                return m;
            }).collect(Collectors.toList());
            return Result.success(data);
        } catch (Exception e) {
            log.error("查询企业真题失败", e);
            return Result.error("查询企业真题失败");
        }
    }

    private MockInterviewCompleteVo buildCompleteVo(MockInterview record) {
        MockInterviewCompleteVo vo = new MockInterviewCompleteVo();
        vo.setId(record.getId());
        vo.setTotalScore(record.getTotalScore());
        vo.setMaxScore(record.getMaxScore());
        vo.setAverageScore(record.getAverageScore() == null ? 0.0 : record.getAverageScore().doubleValue());
        vo.setTotalQuestions(record.getQuestionCount());
        vo.setCompletedQuestions(record.getCompletedQuestions());
        vo.setDuration(record.getDuration());

        List<Map<String, Object>> details = new ArrayList<>();
        List<MockInterviewAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<MockInterviewAnswer>()
                        .eq(MockInterviewAnswer::getInterviewId, record.getId()));
        for (MockInterviewAnswer a : answers) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("questionTitle", a.getQuestionContent());
            detail.put("score", a.getScore());
            detail.put("maxScore", a.getMaxScore());
            detail.put("aiEvaluation", a.getAiEvaluation());
            details.add(detail);
        }
        vo.setDetails(details);
        return vo;
    }

    private MockInterviewDetailVo buildDetailVo(MockInterview record) {
        MockInterviewDetailVo vo = new MockInterviewDetailVo();
        vo.setId(record.getId());
        vo.setDirection(record.getDirection());
        vo.setStartTime(record.getStartTime());
        vo.setEndTime(record.getEndTime());
        vo.setTotalQuestions(record.getQuestionCount());
        vo.setCompletedQuestions(record.getCompletedQuestions());
        vo.setDuration(record.getDuration());
        vo.setStatus(record.getStatus());
        vo.setTotalScore(record.getTotalScore());
        vo.setMaxScore(record.getMaxScore());
        vo.setAverageScore(record.getAverageScore() == null ? 0.0 : record.getAverageScore().doubleValue());
        vo.setInterviewerSummary(record.getInterviewerSummary());

        if (record.getImprovementSuggestions() != null) {
            try {
                vo.setImprovementSuggestions(JSON.parseArray(record.getImprovementSuggestions(), String.class));
            } catch (Exception e) {
                vo.setImprovementSuggestions(new ArrayList<>());
            }
        } else {
            vo.setImprovementSuggestions(new ArrayList<>());
        }

        List<MockInterviewAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<MockInterviewAnswer>()
                        .eq(MockInterviewAnswer::getInterviewId, record.getId()));
        vo.setAnswers(toDetailVos(answers));
        return vo;
    }

    private List<MockInterviewAnswerDetailVo> toDetailVos(List<MockInterviewAnswer> answers) {
        List<MockInterviewAnswerDetailVo> vos = new ArrayList<>();
        for (MockInterviewAnswer a : answers) {
            MockInterviewAnswerDetailVo vo = new MockInterviewAnswerDetailVo();
            vo.setId(a.getId());
            vo.setScore(a.getScore());
            vo.setMaxScore(a.getMaxScore());
            vo.setUserAnswer(a.getUserAnswer());
            vo.setVoiceFileUrl(a.getVoiceFileUrl());
            vo.setAiEvaluation(a.getAiEvaluation());
            vo.setTechnicalAccuracy(a.getTechnicalAccuracy());
            vo.setClarity(a.getClarity());
            vo.setLogic(a.getLogic());
            MockInterviewAnswerDetailVo.QuestionBriefVo brief = new MockInterviewAnswerDetailVo.QuestionBriefVo();
            brief.setDirection(a.getDirection());
            brief.setDifficultyLevel(a.getDifficultyLevel());
            brief.setQuestionContent(a.getQuestionContent());
            vo.setQuestion(brief);
            vos.add(vo);
        }
        return vos;
    }

    private String truncate(String content, int length) {
        if (content == null) {
            return "";
        }
        return content.length() <= length ? content : content.substring(0, length) + "...";
    }
}