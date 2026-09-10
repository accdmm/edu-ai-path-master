package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.CreditRecord;
import com.atguigu.exam.entity.InterviewCompany;
import com.atguigu.exam.entity.InterviewFavorite;
import com.atguigu.exam.entity.InterviewQuestion;
import com.atguigu.exam.entity.UserCredit;
import com.atguigu.exam.mapper.CreditRecordMapper;
import com.atguigu.exam.mapper.InterviewCompanyMapper;
import com.atguigu.exam.mapper.InterviewFavoriteMapper;
import com.atguigu.exam.mapper.InterviewQuestionMapper;
import com.atguigu.exam.mapper.UserCreditMapper;
import com.atguigu.exam.service.InterviewQuestionService;
import com.atguigu.exam.service.MockInterviewAiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业真题服务实现
 */
@Slf4j
@Service
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    @Autowired
    private InterviewQuestionMapper questionMapper;
    @Autowired
    private InterviewCompanyMapper companyMapper;
    @Autowired
    private InterviewFavoriteMapper favoriteMapper;
    @Autowired
    private UserCreditMapper userCreditMapper;
    @Autowired
    private CreditRecordMapper creditRecordMapper;
    @Autowired
    private MockInterviewAiService mockInterviewAiService;

    private static final int AI_ANALYSIS_COST = 5;
    private static final int AI_ANALYSIS_DAILY_FREE = 3;

    @Override
    public Result<IPage<Map<String, Object>>> getQuestionList(Long userId, Integer page, Integer size,
                                                              String direction, String difficultyLevel,
                                                              Long companyId, String keyword) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewQuestion::getStatus, "approved")
                    .eq(direction != null && !direction.isBlank(), InterviewQuestion::getDirection, direction)
                    .eq(difficultyLevel != null && !difficultyLevel.isBlank(), InterviewQuestion::getDifficultyLevel, difficultyLevel)
                    .eq(companyId != null, InterviewQuestion::getCompanyId, companyId)
                    .and(keyword != null && !keyword.isBlank(),
                            w -> w.like(InterviewQuestion::getQuestionContent, keyword)
                                    .or().like(InterviewQuestion::getReferenceAnswer, keyword))
                    .orderByDesc(InterviewQuestion::getViewCount);

            Page<InterviewQuestion> pg = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<InterviewQuestion> result = questionMapper.selectPage(pg, wrapper);

            return Result.success(result.convert(q -> buildQuestionView(q, userId, true)));
        } catch (Exception e) {
            log.error("真题列表查询失败", e);
            return Result.error("真题列表查询失败");
        }
    }

    @Override
    public Result<Map<String, Object>> getQuestionDetail(Long userId, Long id) {
        try {
            InterviewQuestion q = questionMapper.selectById(id);
            if (q == null) {
                return Result.error(404, "题目不存在");
            }
            return Result.success(buildQuestionView(q, userId, false));
        } catch (Exception e) {
            log.error("真题详情查询失败", e);
            return Result.error("真题详情查询失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getHotQuestions(Integer limit) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewQuestion::getStatus, "approved")
                    .orderByDesc(InterviewQuestion::getViewCount)
                    .last("LIMIT " + (limit == null ? 10 : limit));
            List<InterviewQuestion> list = questionMapper.selectList(wrapper);
            List<Map<String, Object>> views = list.stream().map(q -> buildQuestionView(q, null, true)).toList();
            return Result.success(views);
        } catch (Exception e) {
            log.error("热门真题查询失败", e);
            return Result.error("热门真题查询失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getLatestQuestions(Integer limit) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewQuestion::getStatus, "approved")
                    .orderByDesc(InterviewQuestion::getCreateTime)
                    .last("LIMIT " + (limit == null ? 10 : limit));
            List<InterviewQuestion> list = questionMapper.selectList(wrapper);
            List<Map<String, Object>> views = list.stream().map(q -> buildQuestionView(q, null, true)).toList();
            return Result.success(views);
        } catch (Exception e) {
            log.error("最新真题查询失败", e);
            return Result.error("最新真题查询失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getRelatedQuestions(Long id, String direction, Integer limit) {
        try {
            LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewQuestion::getStatus, "approved")
                    .ne(InterviewQuestion::getId, id)
                    .eq(direction != null && !direction.isBlank(), InterviewQuestion::getDirection, direction)
                    .orderByDesc(InterviewQuestion::getViewCount)
                    .last("LIMIT " + (limit == null ? 5 : limit));
            List<InterviewQuestion> list = questionMapper.selectList(wrapper);
            List<Map<String, Object>> views = list.stream().map(q -> buildQuestionView(q, null, true)).toList();
            return Result.success(views);
        } catch (Exception e) {
            log.error("相关真题查询失败", e);
            return Result.error("相关真题查询失败");
        }
    }

    @Override
    public Result<Void> incrementViewCount(Long id) {
        try {
            InterviewQuestion q = new InterviewQuestion();
            q.setId(id);
            q.setViewCount(questionMapper.selectById(id).getViewCount() + 1);
            questionMapper.updateById(q);
            return Result.success(null);
        } catch (Exception e) {
            log.error("浏览量更新失败", e);
            return Result.error("浏览量更新失败");
        }
    }

    @Override
    public Result<Map<String, Object>> submitEvaluation(Long userId, Long questionId, String userAnswer) {
        try {
            InterviewQuestion q = questionMapper.selectById(questionId);
            if (q == null) {
                return Result.error(404, "题目不存在");
            }
            Map<String, Object> ai = mockInterviewAiService.gradeAnswer(
                    q.getDirection(), q.getQuestionContent(), q.getDifficultyLevel(), userAnswer);

            Map<String, Object> data = new HashMap<>();
            data.put("score", ai.get("score"));
            data.put("comment", ai.get("comment"));
            data.put("technicalAccuracy", ai.get("technicalAccuracy"));
            data.put("clarity", ai.get("clarity"));
            data.put("logic", ai.get("logic"));
            return Result.success(data);
        } catch (Exception e) {
            log.error("提交评测失败", e);
            return Result.error("提交评测失败");
        }
    }

    @Override
    public Result<Map<String, Object>> aiAnalysis(Long userId, Long questionId) {
        try {
            InterviewQuestion q = questionMapper.selectById(questionId);
            if (q == null) {
                return Result.error(404, "题目不存在");
            }
            if (userId == null) {
                return Result.error(401, "请先登录");
            }

            // 先掉 LLM，成功后再计费，避免失败扣费
            String analysis = mockInterviewAiService.explainQuestion(
                    q.getDirection(), q.getQuestionContent(), q.getReferenceAnswer());
            if (analysis == null || analysis.isBlank()) {
                return Result.error("AI 解析暂时繁忙，请稍后重试");
            }

            UserCredit credit = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
            if (credit == null) {
                credit = new UserCredit();
                credit.setUserId(userId);
                credit.setTotalCredits(0);
                credit.setActiveCredits(0);
                credit.setCreateTime(new Date());
                credit.setUpdateTime(new Date());
                userCreditMapper.insert(credit);
            }

            // 今日免费次数（changeAmount=0 表示免费额度）
            Date todayStart = Date.from(LocalDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            long freeCount = creditRecordMapper.selectCount(new LambdaQueryWrapper<CreditRecord>()
                    .eq(CreditRecord::getUserId, userId)
                    .eq(CreditRecord::getType, "ai-analysis")
                    .eq(CreditRecord::getChangeAmount, 0)
                    .ge(CreditRecord::getCreateTime, todayStart));

            boolean free = freeCount < AI_ANALYSIS_DAILY_FREE;
            if (!free) {
                // 非免费：条件扣费，active_credits >= 5 才生效，防并发超扣，total_credits 为累计获得不减
                int updated = userCreditMapper.update(null, new UpdateWrapper<UserCredit>()
                        .eq("user_id", userId)
                        .ge("active_credits", AI_ANALYSIS_COST)
                        .setSql("active_credits = active_credits - " + AI_ANALYSIS_COST)
                        .setSql("update_time = NOW()"));
                if (updated == 0) {
                    return Result.error(4002, "积分不足，AI 解析每道题需 " + AI_ANALYSIS_COST + " 积分，可购买邀请码获取积分");
                }
            }

            UserCredit latest = userCreditMapper.selectOne(
                    new LambdaQueryWrapper<UserCredit>().eq(UserCredit::getUserId, userId));
            int balance = latest == null || latest.getActiveCredits() == null ? 0 : latest.getActiveCredits();

            CreditRecord record = new CreditRecord();
            record.setUserId(userId);
            record.setChangeAmount(free ? 0 : -AI_ANALYSIS_COST);
            record.setType("ai-analysis");
            record.setSource(free ? "AI解析免费额度" : "AI题目解析");
            record.setBalance(balance);
            record.setCreateTime(new Date());
            creditRecordMapper.insert(record);

            Map<String, Object> data = new HashMap<>();
            data.put("analysis", analysis);
            data.put("free", free);
            data.put("remainingFree", Math.max(0, AI_ANALYSIS_DAILY_FREE - (int) (free ? freeCount + 1 : freeCount)));
            data.put("activeCredits", balance);
            return Result.success(data);
        } catch (Exception e) {
            log.error("AI 解析题目失败", e);
            return Result.error("AI 解析失败，请稍后重试");
        }
    }

    @Override
    public Result<Map<String, Object>> toggleFavorite(Long userId, Long questionId) {
        try {
            LambdaQueryWrapper<InterviewFavorite> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewFavorite::getUserId, userId)
                    .eq(InterviewFavorite::getQuestionId, questionId);
            InterviewFavorite favorite = favoriteMapper.selectOne(wrapper);

            Map<String, Object> data = new HashMap<>();
            if (favorite != null) {
                favoriteMapper.deleteById(favorite.getId());
                data.put("favorited", false);
            } else {
                InterviewFavorite nf = new InterviewFavorite();
                nf.setUserId(userId);
                nf.setQuestionId(questionId);
                nf.setCreateTime(new Date());
                favoriteMapper.insert(nf);
                data.put("favorited", true);
            }

            long count = favoriteMapper.selectCount(
                    new LambdaQueryWrapper<InterviewFavorite>().eq(InterviewFavorite::getQuestionId, questionId));
            data.put("favoriteCount", count);
            return Result.success(data);
        } catch (Exception e) {
            log.error("收藏操作失败", e);
            return Result.error("收藏操作失败");
        }
    }

    @Override
    public Result<IPage<Map<String, Object>>> getFavoriteList(Long userId, Integer page, Integer size) {
        try {
            LambdaQueryWrapper<InterviewFavorite> fr = new LambdaQueryWrapper<>();
            fr.eq(InterviewFavorite::getUserId, userId)
                    .orderByDesc(InterviewFavorite::getCreateTime);
            Page<InterviewFavorite> fp = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
            IPage<InterviewFavorite> favoritePage = favoriteMapper.selectPage(fp, fr);

            IPage<Map<String, Object>> result = favoritePage.convert(f -> {
                InterviewQuestion q = questionMapper.selectById(f.getQuestionId());
                Map<String, Object> view = new HashMap<>();
                if (q != null) {
                    view = buildQuestionView(q, userId, true);
                }
                view.put("favoriteId", f.getId());
                view.put("favoriteTime", f.getCreateTime());
                return view;
            });
            return Result.success(result);
        } catch (Exception e) {
            log.error("收藏列表查询失败", e);
            return Result.error("收藏列表查询失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getDirectionStats() {
        try {
            List<Map<String, Object>> list = questionMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InterviewQuestion>()
                            .select("direction AS name", "COUNT(*) AS value")
                            .eq("status", "approved")
                            .eq("is_deleted", 0)
                            .groupBy("direction"));
            list.forEach(m -> m.put("value", ((Number) m.getOrDefault("value", 0)).longValue()));
            return Result.success(list);
        } catch (Exception e) {
            log.error("方向统计失败", e);
            return Result.error("方向统计失败");
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getCompanyStats() {
        try {
            List<Map<String, Object>> list = questionMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InterviewQuestion>()
                            .select("company_id AS companyId", "COUNT(*) AS value")
                            .eq("status", "approved")
                            .eq("is_deleted", 0)
                            .groupBy("company_id"));
            list.forEach(m -> m.put("value", ((Number) m.getOrDefault("value", 0)).longValue()));
            return Result.success(list);
        } catch (Exception e) {
            log.error("企业统计失败", e);
            return Result.error("企业统计失败");
        }
    }

    /**
     * 组装题目视图：附带企业名、状态、收藏标记
     */
    private Map<String, Object> buildQuestionView(InterviewQuestion q, Long userId, boolean brief) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", q.getId());
        map.put("direction", q.getDirection());
        map.put("difficultyLevel", q.getDifficultyLevel());
        map.put("companyId", q.getCompanyId());
        map.put("companyName", getCompanyName(q.getCompanyId()));
        map.put("interviewYear", q.getInterviewYear());
        map.put("questionContent", q.getQuestionContent());
        map.put("viewCount", q.getViewCount());
        map.put("favoriteCount", q.getFavoriteCount());
        map.put("status", q.getStatus());
        map.put("createTime", q.getCreateTime());
        if (!brief) {
            map.put("referenceAnswer", q.getReferenceAnswer());
        }
        if (userId != null) {
            boolean favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<InterviewFavorite>()
                    .eq(InterviewFavorite::getUserId, userId)
                    .eq(InterviewFavorite::getQuestionId, q.getId())) > 0;
            map.put("isFavorited", favorited);
        } else {
            map.put("isFavorited", false);
        }
        return map;
    }

    private String getCompanyName(Long companyId) {
        if (companyId == null) {
            return null;
        }
        InterviewCompany company = companyMapper.selectById(companyId);
        return company == null ? null : company.getName();
    }
}