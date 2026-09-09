package com.atguigu.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.UserPaper;
import com.atguigu.exam.mapper.AnswerRecordMapper;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.MockInterviewMapper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.mapper.UserPaperMapper;
import com.atguigu.exam.service.LearningAnalysisService;
import com.atguigu.exam.vo.LearningReportVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习分析服务实现
 * 聚合考试记录/答题记录/题目分类/AI试卷/模拟面试数据
 */
@Slf4j
@Service
public class LearningAnalysisServiceImpl implements LearningAnalysisService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private UserPaperMapper userPaperMapper;

    @Autowired
    private MockInterviewMapper mockInterviewMapper;

    @Autowired
    @Qualifier("paperChatModel")
    private ChatLanguageModel paperChatModel;

    @Override
    public LearningReportVo getReport(Long userId) {
        LearningReportVo report = new LearningReportVo();
        LearningReportVo.OverviewVo overview = report.getOverview();

        // 1. 考试记录（按开始时间升序），仅统计已出分的记录
        List<ExamRecord> examRecords = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .isNotNull(ExamRecord::getScore)
                        .orderByAsc(ExamRecord::getStartTime));

        // 2. 成绩趋势
        List<LearningReportVo.TrendItemVo> trendList = new ArrayList<>();
        if (!examRecords.isEmpty()) {
            Map<Long, Paper> paperCache = new HashMap<>();
            for (ExamRecord record : examRecords) {
                Paper paper = paperCache.computeIfAbsent(record.getExamId().longValue(),
                        paperMapper::selectById);
                if (paper == null) {
                    continue;
                }
                int totalScore = paper.getTotalScore() == null ? 0 : paper.getTotalScore().intValue();
                if (totalScore <= 0) {
                    continue;
                }
                LearningReportVo.TrendItemVo item = new LearningReportVo.TrendItemVo();
                item.setExamTime(record.getStartTime());
                item.setPaperName(paper.getName());
                item.setScore(record.getScore());
                item.setTotalScore(totalScore);
                item.setScoreRate(calRate(record.getScore(), totalScore));
                trendList.add(item);
            }
        }
        report.setScoreTrend(trendList);
        // 平均得分率
        int examCount = trendList.size();
        int rateSum = 0;
        for (LearningReportVo.TrendItemVo item : trendList) {
            rateSum += item.getScoreRate();
        }
        overview.setExamCount(examCount);
        if (examCount > 0) {
            overview.setAvgScoreRate(rateSum / examCount);
        }

        // 3. 答题记录（通过考试记录归属用户）
        List<AnswerRecord> answerRecords = new ArrayList<>();
        Set<Long> examRecordIds = examRecords.stream()
                .map(ExamRecord::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!examRecordIds.isEmpty()) {
            answerRecords = answerRecordMapper.selectList(
                    new LambdaQueryWrapper<AnswerRecord>()
                            .in(AnswerRecord::getExamRecordId, examRecordIds));
        }
        overview.setAnswerCount(answerRecords.size());
        int correctCount = (int) answerRecords.stream()
                .filter(a -> Objects.equals(a.getIsCorrect(), 1)).count();
        overview.setCorrectCount(correctCount);
        if (!answerRecords.isEmpty()) {
            overview.setCorrectRate(correctCount * 100 / answerRecords.size());
        }

        // 4. 知识点掌握度（按题目分类聚合分值得分率）
        List<LearningReportVo.CategoryMasteryVo> masteryList = buildCategoryMastery(answerRecords);
        report.setCategoryMastery(masteryList);
        for (LearningReportVo.CategoryMasteryVo mastery : masteryList) {
            if (mastery.getAnswerCount() == 0) {
                continue;
            }
            if (mastery.getMaxScore() <= 0) {
                continue;
            }
            mastery.setCorrectRate(calRate(mastery.getEarnedScore(), mastery.getMaxScore()));
        }

        // 5. 雷达图：知识点掌握度 Top6（按答题量排序），0-100
        List<LearningReportVo.RadarItemVo> radarList = new ArrayList<>();
        List<LearningReportVo.CategoryMasteryVo> top = masteryList.stream()
                .sorted(Comparator.comparingInt(LearningReportVo.CategoryMasteryVo::getAnswerCount).reversed())
                .limit(6)
                .collect(Collectors.toList());
        for (LearningReportVo.CategoryMasteryVo mastery : top) {
            LearningReportVo.RadarItemVo radarItem = new LearningReportVo.RadarItemVo();
            radarItem.setName(mastery.getCategoryName());
            radarItem.setValue(mastery.getMaxScore() <= 0 ? 0 : mastery.getCorrectRate());
            radarList.add(radarItem);
        }
        report.setRadar(radarList);

        // 6. AI 试卷数 + 模拟面试数
        List<UserPaper> userPapers = userPaperMapper.selectList(
                new LambdaQueryWrapper<UserPaper>().eq(UserPaper::getUserId, userId));
        overview.setAiPaperCount(userPapers.size());

        Long interviewCount = mockInterviewMapper.selectCount(
                new LambdaQueryWrapper<com.atguigu.exam.entity.MockInterview>()
                        .eq(com.atguigu.exam.entity.MockInterview::getUserId, userId));
        overview.setInterviewCount(interviewCount == null ? 0 : interviewCount.intValue());

        return report;
    }

    @Override
    public List<String> generateAiSuggest(Long userId, LearningReportVo report) {
        // 规则型兜底建议
        List<String> fallback = new ArrayList<>();
        fallback.add("保持稳定的刷题节奏，建议每天完成 10-15 道练习题并同步复习错题。");
        fallback.add("针对得分较低的知识点做专项练习，用错题本记录每道错题的考点与正确思路。");
        fallback.add("定期参加模拟考试检验学习效果，并根据成绩趋势动态调整复习计划。");
        if (report.getOverview().getAnswerCount() < 20) {
            fallback.add(0, "当前答题数据较少，先完成几次模拟考试，AI 才能给出更精准的学习建议。");
        }
        List<LearningReportVo.CategoryMasteryVo> weak = report.getCategoryMastery().stream()
                .filter(m -> m.getMaxScore() > 0)
                .sorted(Comparator.comparingInt(LearningReportVo.CategoryMasteryVo::getCorrectRate))
                .limit(3)
                .collect(Collectors.toList());
        if (!weak.isEmpty()) {
            String weakNames = weak.stream()
                    .map(LearningReportVo.CategoryMasteryVo::getCategoryName)
                    .collect(Collectors.joining("、"));
            fallback.add(1, "建议优先攻克薄弱知识点：「" + weakNames + "」。");
        }
        try {
            String content = paperChatModel.chat(buildSuggestPrompt(report));
            List<String> suggestions = parseSuggestions(content);
            if (suggestions != null && !suggestions.isEmpty()) {
                return suggestions;
            }
        } catch (Exception e) {
            log.warn("AI 学习建议生成失败，使用规则型建议。原因：{}", e.getMessage());
        }
        return fallback;
    }

    /**
     * 按题目分类聚合答题得分率
     */
    private List<LearningReportVo.CategoryMasteryVo> buildCategoryMastery(List<AnswerRecord> answerRecords) {
        List<LearningReportVo.CategoryMasteryVo> result = new ArrayList<>();
        if (answerRecords == null || answerRecords.isEmpty()) {
            return result;
        }
        Set<Long> questionIds = answerRecords.stream()
                .map(AnswerRecord::getQuestionId).filter(Objects::nonNull)
                .map(Long::valueOf).collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return result;
        }
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        Map<Long, Category> categoryCache = new HashMap<>();
        Map<String, LearningReportVo.CategoryMasteryVo> masteryMap = new LinkedHashMap<>();

        for (AnswerRecord record : answerRecords) {
            if (record.getQuestionId() == null) {
                continue;
            }
            Question question = questionMap.get(record.getQuestionId().longValue());
            if (question == null) {
                continue;
            }
            Long categoryId = question.getCategoryId();
            if (categoryId == null) {
                categoryId = 0L;
            }
            Category category = categoryCache.get(categoryId);
            if (category == null) {
                category = categoryId == 0L ? null : categoryMapper.selectById(categoryId);
                categoryCache.put(categoryId, category);
            }
            String categoryName = category != null && category.getName() != null ? category.getName() : "未分类";
            LearningReportVo.CategoryMasteryVo mastery = masteryMap.computeIfAbsent(categoryName,
                    name -> {
                        LearningReportVo.CategoryMasteryVo item = new LearningReportVo.CategoryMasteryVo();
                        item.setCategoryName(name);
                        return item;
                    });
            int maxScore = question.getScore() == null ? 0 : question.getScore();
            int earnedScore = record.getScore() == null ? 0 : record.getScore();
            mastery.setAnswerCount(mastery.getAnswerCount() + 1);
            mastery.setEarnedScore(mastery.getEarnedScore() + earnedScore);
            mastery.setMaxScore(mastery.getMaxScore() + maxScore);
        }
        result.addAll(masteryMap.values());
        return result;
    }

    /**
     * 组装 AI 学习建议提示词
     */
    private String buildSuggestPrompt(LearningReportVo report) {
        LearningReportVo.OverviewVo overview = report.getOverview();
        StringBuilder weak = new StringBuilder();
        for (int i = 0; i < Math.min(5, report.getCategoryMastery().size()); i++) {
            LearningReportVo.CategoryMasteryVo m = report.getCategoryMastery().get(i);
            if (m.getMaxScore() <= 0) {
                continue;
            }
            weak.append(i + 1).append(". ").append(m.getCategoryName())
                    .append(" 得分率").append(m.getCorrectRate()).append("%，答题")
                    .append(m.getAnswerCount()).append("道\n");
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深 IT 学习规划专家，请为用户生成个性化学习建议。\n\n");
        prompt.append("【学习概况】\n");
        prompt.append("- 考试次数：").append(overview.getExamCount()).append(" 次，平均得分率：")
                .append(overview.getAvgScoreRate()).append("%\n");
        prompt.append("- 累计答题：").append(overview.getAnswerCount()).append(" 道，整体正确率：")
                .append(overview.getCorrectRate()).append("%\n");
        prompt.append("- AI 生成试卷：").append(overview.getAiPaperCount()).append(" 套，模拟面试：")
                .append(overview.getInterviewCount()).append(" 次\n");
        prompt.append("【知识点得分率（按答题量降序展示）】\n");
        prompt.append(report.getCategoryMastery().stream().limit(6)
                .map(m -> "- " + m.getCategoryName() + "：" + m.getCorrectRate() + "%")
                .collect(Collectors.joining("\n"))).append("\n\n");
        prompt.append("请生成 3-5 条有针对性的学习建议：\n");
        prompt.append("1. 明确指出最薄弱的知识点，并给出具体攻克方法\n");
        prompt.append("2. 结合得分率给出复习优先级\n");
        prompt.append("3. 建议具体可执行（如每天练习量、复习方法、合理利用 AI 试卷等）\n\n");
        prompt.append("请严格按照以下 JSON 格式返回，禁止包含其他文字：\n");
        prompt.append("{\"suggestions\":[\"建议1\",\"建议2\",\"建议3\"]}\n");
        return prompt.toString();
    }

    /**
     * 解析 LLM 返回的 suggestions 数组
     */
    private List<String> parseSuggestions(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        int startIndex = content.indexOf("```json");
        int endIndex = content.lastIndexOf("```");
        String jsonText = null;
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            jsonText = content.substring(startIndex + 7, endIndex).trim();
        } else {
            int braceStart = content.indexOf("{");
            int braceEnd = content.lastIndexOf("}");
            if (braceStart != -1 && braceEnd > braceStart) {
                jsonText = content.substring(braceStart, braceEnd + 1);
            }
        }
        if (jsonText == null) {
            return null;
        }
        JSONObject json = JSONObject.parseObject(jsonText);
        if (json == null) {
            return null;
        }
        JSONArray suggestions = json.getJSONArray("suggestions");
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        return suggestions.toJavaList(String.class);
    }

    private int calRate(int earned, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(earned * 100f / total);
    }
}