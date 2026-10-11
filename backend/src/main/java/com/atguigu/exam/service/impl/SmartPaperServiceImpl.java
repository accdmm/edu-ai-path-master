package com.atguigu.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.exam.common.CreditNotEnoughException;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.UserPaper;
import com.atguigu.exam.entity.WrongQuestion;
import com.atguigu.exam.service.CategoryService;
import com.atguigu.exam.service.CreditBillingService;
import com.atguigu.exam.service.PaperQuestionService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.service.SmartPaperService;
import com.atguigu.exam.service.UserPaperService;
import com.atguigu.exam.service.WrongQuestionService;
import com.atguigu.exam.vo.SmartPaperVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 智能组卷服务实现
 *
 * 组卷流程：薄弱点诊断（错题按知识点聚合）→ 候选题库（薄弱知识点优先 + 其他补充）
 * → LLM 编排选题（题型覆盖 / 难度梯度 / 薄弱点加权）→ 校验与规则兜底 → 事务扣费落库
 */
@Slf4j
@Service
public class SmartPaperServiceImpl implements SmartPaperService {

    /** 智能组卷计费：每次固定消耗积分（无免费额度；生成失败不扣费） */
    public static final int SMART_PAPER_COST = 10;

    private static final List<String> TYPES = List.of("CHOICE", "JUDGE", "TEXT");
    private static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private static final int DEFAULT_QUESTIONS = 10;
    private static final int MIN_QUESTIONS = 5;
    private static final int MAX_QUESTIONS = 20;
    private static final int DEFAULT_DURATION = 30;
    private static final int DEFAULT_SCORE = 5;

    /** 薄弱知识点取前 N 个（错题数倒序） */
    private static final int MAX_WEAK_POINTS = 3;
    /** 候选题库上限（控制 LLM 提示词长度） */
    private static final int MAX_CANDIDATES = 40;

    @Qualifier("paperChatModel")
    @Autowired
    private ChatLanguageModel paperChatModel;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private WrongQuestionService wrongQuestionService;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperQuestionService paperQuestionService;

    @Autowired
    private UserPaperService userPaperService;

    @Autowired
    private CreditBillingService creditBillingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public Result<SmartPaperVo> generate(Long userId, Integer questionCount) {
        int count = normalizeCount(questionCount);
        //0. 预检查积分：不足直接快速失败，避免白调 LLM
        if (creditBillingService.currentBalance(userId) < SMART_PAPER_COST) {
            return Result.error(4002, "积分不足，AI 智能组卷每次需要 " + SMART_PAPER_COST + " 积分，可购买邀请码获取积分");
        }
        //1. 薄弱点诊断 + 候选题库
        List<SmartPaperVo.WeakPoint> weakPoints = analyzeWeakPoints(userId);
        List<Question> candidates = buildCandidates(weakPoints, count);
        if (ObjectUtils.isEmpty(candidates)) {
            return Result.error("题库暂无可用题目，无法组卷");
        }
        //2. AI 编排选题（异常/非法输出时规则兜底，保证题量与题型覆盖）
        AiSelection selection = aiSelect(candidates, weakPoints, count);

        //3. 事务：扣积分 + 组卷落库（失败整体回滚，积分自动返还）
        SmartPaperVo vo = new TransactionTemplate(transactionManager).execute(status -> {
            if (!creditBillingService.deductIfEnough(userId, SMART_PAPER_COST)) {
                throw new CreditNotEnoughException("积分不足，AI 智能组卷每次需要 " + SMART_PAPER_COST + " 积分");
            }
            creditBillingService.record(userId, -SMART_PAPER_COST, "smart-paper", "AI智能组卷",
                    creditBillingService.currentBalance(userId));
            return persistPaper(userId, candidates, selection, count);
        });
        if (vo == null) {
            return Result.error("组卷失败，请稍后重试");
        }
        vo.setCost(SMART_PAPER_COST);
        vo.setActiveCredits(creditBillingService.currentBalance(userId));
        log.info("AI 智能组卷完成：userId={} paperId={} 题量={} 薄弱点={}",
                userId, vo.getPaperId(), vo.getQuestionCount(),
                weakPoints.stream().map(SmartPaperVo.WeakPoint::getCategoryName).toList());
        return Result.success(vo);
    }

    @Override
    public List<SmartPaperVo.WeakPoint> weakPoints(Long userId) {
        return analyzeWeakPoints(userId);
    }

    // ---------------- 薄弱点诊断 ----------------

    private List<SmartPaperVo.WeakPoint> analyzeWeakPoints(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionService.list(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .in(WrongQuestion::getStatus, "UNMASTERED", "LEARNING"));
        if (ObjectUtils.isEmpty(wrongs)) {
            return new ArrayList<>();
        }
        Map<Long, Long> countByCategory = wrongs.stream()
                .map(WrongQuestion::getCategoryId).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        if (countByCategory.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> nameMap = categoryService.listByIds(countByCategory.keySet()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        return countByCategory.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(MAX_WEAK_POINTS)
                .map(entry -> {
                    SmartPaperVo.WeakPoint point = new SmartPaperVo.WeakPoint();
                    point.setCategoryId(entry.getKey());
                    point.setCategoryName(nameMap.getOrDefault(entry.getKey(), "未分类"));
                    point.setWrongCount(entry.getValue().intValue());
                    return point;
                })
                .collect(Collectors.toList());
    }

    // ---------------- 候选题库 ----------------

    /**
     * 候选题库：薄弱知识点（含子分类）优先，池子不足时用其他知识点题目补充，
     * 保证"错题不作为唯一来源、题库题目共同组成"，且题量永远够用（受题库总量限制）
     */
    private List<Question> buildCandidates(List<SmartPaperVo.WeakPoint> weakPoints, int count) {
        int cap = Math.min(Math.max(count * 3, 30), MAX_CANDIDATES);
        List<Question> candidates = new ArrayList<>();
        Set<Long> pickedIds = new HashSet<>();
        if (!ObjectUtils.isEmpty(weakPoints)) {
            Set<Long> weakScope = expandCategoryScope(weakPoints.stream()
                    .map(SmartPaperVo.WeakPoint::getCategoryId).toList());
            List<Question> weakPool = questionService.list(new LambdaQueryWrapper<Question>()
                    .in(Question::getCategoryId, weakScope));
            Collections.shuffle(weakPool);
            for (Question question : weakPool) {
                if (candidates.size() >= cap) {
                    break;
                }
                if (pickedIds.add(question.getId())) {
                    candidates.add(question);
                }
            }
        }
        // 补充其他知识点题目（薄弱池不足 count*2 时），避免"错题/薄弱题不够导致题量过少"
        if (candidates.size() < Math.min(count * 2, cap)) {
            List<Question> others = questionService.list(new LambdaQueryWrapper<Question>()
                    .notIn(!pickedIds.isEmpty(), Question::getId, pickedIds));
            Collections.shuffle(others);
            for (Question question : others) {
                if (candidates.size() >= cap) {
                    break;
                }
                if (pickedIds.add(question.getId())) {
                    candidates.add(question);
                }
            }
        }
        return candidates;
    }

    /**
     * 展开知识点范围：选中分类 + 其全部子分类
     */
    private Set<Long> expandCategoryScope(List<Long> categoryIds) {
        List<Category> all = categoryService.list();
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (Category category : all) {
            Long parent = category.getParentId() == null ? 0L : category.getParentId();
            childrenByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(category.getId());
        }
        Set<Long> scope = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        for (Long id : categoryIds) {
            if (id != null) {
                queue.add(id);
            }
        }
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (scope.add(current)) {
                List<Long> children = childrenByParent.get(current);
                if (children != null) {
                    queue.addAll(children);
                }
            }
        }
        return scope;
    }

    // ---------------- AI 编排选题 ----------------

    /** AI 选题结果：题目ID列表 + 组卷思路 */
    private record AiSelection(List<Long> questionIds, String summary) {
    }

    /**
     * AI 从候选题库中编排选题；输出非法/数量不足时规则兜底（先补缺失题型，再顺序补足），
     * 保证题量充足、题型尽量覆盖，且绝不编造题目（ID 必须命中候选集）
     */
    private AiSelection aiSelect(List<Question> candidates, List<SmartPaperVo.WeakPoint> weakPoints, int count) {
        Map<Long, Question> candidateMap = candidates.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        try {
            String content = paperChatModel.chat(buildSelectionPrompt(candidates, weakPoints, count));
            JSONObject json = parseJson(content);
            if (json != null) {
                JSONArray array = json.getJSONArray("questionIds");
                if (array != null && !array.isEmpty()) {
                    LinkedHashSet<Long> valid = new LinkedHashSet<>();
                    for (int i = 0; i < array.size(); i++) {
                        Long id = array.getLong(i);
                        if (id != null && candidateMap.containsKey(id)) {
                            valid.add(id);
                        }
                    }
                    if (!valid.isEmpty()) {
                        List<Long> filled = fillWithDiversity(new ArrayList<>(valid), candidates, count);
                        String summary = json.getString("summary");
                        return new AiSelection(filled, summary == null ? "" : summary.trim());
                    }
                }
            }
            log.warn("AI 组卷输出无法解析或为空，使用规则兜底。content={}", content);
        } catch (Exception e) {
            log.warn("AI 组卷调用失败，使用规则兜底：{}", e.getMessage());
        }
        return new AiSelection(fillWithDiversity(new ArrayList<>(), candidates, count), "");
    }

    /**
     * 规则补足：优先补齐缺失题型（保证题型覆盖），再按薄弱优先顺序补足题量
     */
    private List<Long> fillWithDiversity(List<Long> ids, List<Question> candidates, int count) {
        LinkedHashSet<Long> picked = new LinkedHashSet<>(ids);
        Map<Long, Question> candidateMap = candidates.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        Set<String> typesPresent = picked.stream().map(candidateMap::get).filter(Objects::nonNull)
                .map(Question::getType).collect(Collectors.toSet());
        for (String type : TYPES) {
            if (picked.size() >= count || typesPresent.contains(type)) {
                continue;
            }
            for (Question question : candidates) {
                if (type.equals(question.getType()) && picked.add(question.getId())) {
                    break;
                }
            }
        }
        for (Question question : candidates) {
            if (picked.size() >= count) {
                break;
            }
            picked.add(question.getId());
        }
        List<Long> result = new ArrayList<>(picked);
        return result.size() > count ? new ArrayList<>(result.subList(0, count)) : result;
    }

    private String buildSelectionPrompt(List<Question> candidates, List<SmartPaperVo.WeakPoint> weakPoints, int count) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深技术组卷老师，请根据学生的薄弱知识点，从候选题库中选出恰好 ")
                .append(count).append(" 道题，组成一份个性化巩固练习卷。\n\n");
        prompt.append("【学生薄弱知识点】（按薄弱程度从高到低）\n");
        if (ObjectUtils.isEmpty(weakPoints)) {
            prompt.append("暂无明显薄弱点数据，请做知识点覆盖面广、难度均衡的综合练习组卷。\n\n");
        } else {
            int i = 1;
            for (SmartPaperVo.WeakPoint point : weakPoints) {
                prompt.append(i++).append(". ").append(point.getCategoryName())
                        .append("（未掌握/半掌握错题 ").append(point.getWrongCount()).append(" 道）\n");
            }
            prompt.append("\n");
        }
        prompt.append("【候选题库】（只能从以下题目中选择，禁止编造 ID）\n");
        for (Question question : candidates) {
            String title = question.getTitle() == null ? "" : question.getTitle();
            if (title.length() > 50) {
                title = title.substring(0, 50) + "…";
            }
            prompt.append("[").append(question.getId()).append("] ")
                    .append(question.getType()).append("/").append(effectiveDifficulty(question)).append(" ")
                    .append(title.replace('\n', ' ')).append("\n");
        }
        prompt.append("\n【组卷要求】\n");
        prompt.append("1. 恰好选择 ").append(count).append(" 道题；越薄弱的知识点分配越多题目\n");
        prompt.append("2. 题型尽量覆盖选择题(CHOICE)、判断题(JUDGE)、简答题(TEXT)（候选充足时）\n");
        prompt.append("3. 难度梯度合理：简单(EASY)打底、中等(MEDIUM)为主、困难(HARD)拔高\n");
        prompt.append("4. 只能使用候选题库中出现的题目 ID，不得编造\n");
        prompt.append("5. 严格输出 JSON，不要任何多余文字：{\"questionIds\":[1,2,3],\"summary\":\"一句话说明组卷思路\"}");
        return prompt.toString();
    }

    private JSONObject parseJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content.trim();
        try {
            if (text.startsWith("```")) {
                int start = text.indexOf("{");
                int end = text.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    text = text.substring(start, end + 1);
                }
            }
            return JSONObject.parseObject(text);
        } catch (Exception e) {
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");
            if (start >= 0 && end > start) {
                try {
                    return JSONObject.parseObject(text.substring(start, end + 1));
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    // ---------------- 组卷落库 ----------------

    private SmartPaperVo persistPaper(Long userId, List<Question> candidates, AiSelection selection, int count) {
        Map<Long, Question> candidateMap = candidates.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        List<Question> questions = selection.questionIds().stream()
                .map(candidateMap::get).filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(q -> DIFFICULTIES.indexOf(effectiveDifficulty(q))))
                .collect(Collectors.toList());
        if (questions.isEmpty()) {
            throw new RuntimeException("组卷失败：未选出任何题目");
        }
        // 错题来源统计（被选中的题目中属于用户未掌握/半掌握错题的数量）
        Set<Long> wrongIds = wrongQuestionService.list(new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .in(WrongQuestion::getStatus, "UNMASTERED", "LEARNING")
                        .in(WrongQuestion::getQuestionId, questions.stream().map(Question::getId).toList()))
                .stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        int wrongCount = (int) questions.stream().filter(q -> wrongIds.contains(q.getId())).count();

        Paper paper = new Paper();
        paper.setName("AI智能组卷-" + new SimpleDateFormat("MMddHHmm").format(new Date()));
        paper.setDescription(buildDescription(candidates, selection, questions.size()));
        paper.setStatus("DRAFT");
        paper.setDuration(DEFAULT_DURATION);
        BigDecimal totalScore = questions.stream()
                .map(q -> BigDecimal.valueOf(scoreOf(q)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(questions.size());
        paperService.save(paper);
        for (Question question : questions) {
            paperQuestionService.save(new PaperQuestion(paper.getId().intValue(), question.getId(),
                    BigDecimal.valueOf(scoreOf(question))));
        }
        UserPaper userPaper = new UserPaper();
        userPaper.setUserId(userId);
        userPaper.setPaperId(paper.getId());
        userPaper.setRelationType(UserPaperServiceImpl.RELATION_AI_GENERATED);
        userPaperService.save(userPaper);

        SmartPaperVo vo = new SmartPaperVo();
        vo.setPaperId(paper.getId());
        vo.setName(paper.getName());
        vo.setQuestionCount(questions.size());
        vo.setTotalScore(totalScore);
        vo.setDuration(DEFAULT_DURATION);
        vo.setWrongQuestionCount(wrongCount);
        vo.setBankQuestionCount(questions.size() - wrongCount);
        for (String type : TYPES) {
            int n = (int) questions.stream().filter(q -> type.equals(q.getType())).count();
            if (n > 0) {
                vo.getTypeBreakdown().put(type, n);
            }
        }
        for (String difficulty : DIFFICULTIES) {
            int n = (int) questions.stream().filter(q -> difficulty.equals(effectiveDifficulty(q))).count();
            if (n > 0) {
                vo.getDifficultyBreakdown().put(difficulty, n);
            }
        }
        return vo;
    }

    /**
     * 试卷简介：AI 组卷思路 + 实际组成，考试开始页可见（保证组卷过程透明）
     */
    private String buildDescription(List<Question> candidates, AiSelection selection, int questionCount) {
        StringBuilder description = new StringBuilder("AI 根据你的薄弱知识点智能组卷（题库选题，共 ")
                .append(questionCount).append(" 题）。");
        if (!selection.summary().isBlank()) {
            description.append("组卷思路：").append(selection.summary());
        }
        return description.toString();
    }

    private int normalizeCount(Integer questionCount) {
        if (questionCount == null) {
            return DEFAULT_QUESTIONS;
        }
        return Math.max(MIN_QUESTIONS, Math.min(MAX_QUESTIONS, questionCount));
    }

    /** 难度统一（空值/未知按 MEDIUM 处理） */
    private String effectiveDifficulty(Question question) {
        String difficulty = question.getDifficulty();
        return DIFFICULTIES.contains(difficulty) ? difficulty : "MEDIUM";
    }

    private int scoreOf(Question question) {
        return question.getScore() == null ? DEFAULT_SCORE : question.getScore();
    }
}