package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.CreditNotEnoughException;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.entity.UserPaper;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.service.AiGeneratedPaperService;
import com.atguigu.exam.service.CategoryService;
import com.atguigu.exam.service.CreditBillingService;
import com.atguigu.exam.service.PaperGenerateAiService;
import com.atguigu.exam.service.PaperQuestionService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.service.UserPaperService;
import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.QuestionImportVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 生成试卷落库服务实现类
 * <p>
 * 整套试卷事务落库：
 * 分类（按 topic 动态建/复用）→ questions → question_choices → question_answers
 * → paper(DRAFT) → paper_question → user_paper（归属当前用户，仅本人可见）
 */
@Slf4j
@Service
public class AiGeneratedPaperServiceImpl implements AiGeneratedPaperService {

    /** AI 生成整套试卷计费：每套扣积分，与本落库事务绑定（生成失败自动回滚不扣分） */
    public static final int PAPER_GEN_COST = 20;

    @Autowired
    private PaperGenerateAiService paperGenerateAiService;

    @Autowired
    private CreditBillingService creditBillingService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperQuestionService paperQuestionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserPaperService userPaperService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Paper generateAndSave(Long userId, AiGenerateRequestVo request) {
        // 0. 计费：生成一套试卷扣 PAPER_GEN_COST 积分（条件原子扣减）
        // 扣费与落库在同一事务，出题/落库失败时整体回滚，积分自动返还
        if (!creditBillingService.deductIfEnough(userId, PAPER_GEN_COST)) {
            throw new CreditNotEnoughException("生成一套试卷需要 " + PAPER_GEN_COST + " 积分");
        }
        creditBillingService.record(userId, -PAPER_GEN_COST, "ai-paper", "AI生成试卷（智能客服）",
                creditBillingService.currentBalance(userId));

        // 1. 大模型出题
        List<QuestionImportVo> questionImports = paperGenerateAiService.generatePaperQuestions(request);
        if (questionImports == null || questionImports.isEmpty()) {
            throw new RuntimeException("AI 生成试卷失败：没有生成任何题目！");
        }
        log.info("AI 生成了 {} 道题，topic={}", questionImports.size(), request.getTopic());

        // 2. 动态创建（或复用）分类，绑定 topic
        Category category = getOrCreateCategory(request.getTopic());

        // 3. 题目入库：questions + choices + answers（绕过标题去重，AI 卷允许重复知识点）
        List<Question> questionList = new ArrayList<>(questionImports.size());
        for (QuestionImportVo questionImport : questionImports) {
            Question question = convertToQuestion(questionImport, category.getId());
            questionService.save(question);
            saveChoicesAndAnswer(question);
            questionList.add(question);
        }

        // 4. 整卷信息
        Paper paper = new Paper();
        paper.setName(buildPaperName(request.getTopic()));
        paper.setDescription("AI 智能生成试卷：主题「" + request.getTopic() + "」，难度 "
                + difficultyText(request.getDifficulty()) + "，仅供生成者本人练习。");
        paper.setStatus("DRAFT");
        paper.setDuration(60);
        BigDecimal totalScore = questionList.stream()
                .map(q -> BigDecimal.valueOf(q.getScore() == null ? 5 : q.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(questionList.size());
        paperService.save(paper);

        // 5. 试卷-题目关联
        List<PaperQuestion> paperQuestions = new ArrayList<>(questionList.size());
        for (Question question : questionList) {
            paperQuestions.add(new PaperQuestion(paper.getId().intValue(), question.getId(),
                    BigDecimal.valueOf(question.getScore() == null ? 5 : question.getScore())));
        }
        paperQuestionService.saveBatch(paperQuestions);

        // 6. 用户-试卷归属（AI 生成卷仅生成者本人可见）
        UserPaper userPaper = new UserPaper();
        userPaper.setUserId(userId);
        userPaper.setPaperId(paper.getId());
        userPaper.setRelationType(UserPaperServiceImpl.RELATION_AI_GENERATED);
        userPaperService.save(userPaper);

        log.info("AI 生成试卷落库成功：paperId={}, 名称={}, {} 道题, 总分 {}", paper.getId(), paper.getName(),
                paper.getQuestionCount(), paper.getTotalScore());
        return paper;
    }

    /**
     * 将 AI 生成的题目 VO 转成 Question 实体（并构造答案对象与选项）
     * 注意：不使用 BeanUtils，避免 List<ChoiceImportDto>/String answer 与 Question 的 List<QuestionChoice>/QuestionAnswer 类型不匹配。
     */
    private Question convertToQuestion(QuestionImportVo importVo, Long categoryId) {
        Question question = new Question();
        question.setTitle(importVo.getTitle());
        question.setType(importVo.getType());
        question.setMulti(Boolean.TRUE.equals(importVo.getMulti()));
        question.setCategoryId(categoryId);
        question.setDifficulty(importVo.getDifficulty() == null ? "MEDIUM" : importVo.getDifficulty());
        question.setScore(importVo.getScore() == null ? 5 : importVo.getScore());
        question.setAnalysis(importVo.getAnalysis());
        // CHOICE：显式转换选项 List<ChoiceImportDto> -> List<QuestionChoice>
        if ("CHOICE".equals(question.getType())) {
            List<QuestionChoice> choices = new ArrayList<>();
            if (importVo.getChoices() != null) {
                for (QuestionImportVo.ChoiceImportDto dto : importVo.getChoices()) {
                    QuestionChoice choice = new QuestionChoice();
                    choice.setContent(dto.getContent());
                    choice.setIsCorrect(Boolean.TRUE.equals(dto.getIsCorrect()));
                    choices.add(choice);
                }
            }
            question.setChoices(choices);
        }
        // 构造答案对象（JUDGE/TEXT 的答案与 keywords）
        QuestionAnswer answer = new QuestionAnswer();
        answer.setAnswer(importVo.getAnswer());
        answer.setKeywords(importVo.getKeywords());
        question.setAnswer(answer);
        return question;
    }

    /**
     * 保存题目的选项与答案：
     * CHOICE 由选项 isCorrect 推导答案字母（A/B/C/D，多选逗号分隔）；JUDGE/TEXT 用 AI 返回的 answer
     */
    private void saveChoicesAndAnswer(Question question) {
        QuestionAnswer answer = question.getAnswer();
        answer.setQuestionId(question.getId());
        if ("CHOICE".equals(question.getType())) {
            // answer 不用 AI 返回，改用 setChoices 存储（拷贝到 choices 字段，供下文遍历）
            List<QuestionChoice> choices = question.getChoices();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice choice = choices.get(i);
                choice.setQuestionId(question.getId());
                choice.setSort(i);
                questionChoiceMapper.insert(choice);
                if (Boolean.TRUE.equals(choice.getIsCorrect())) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append((char) ('A' + i));
                }
            }
            answer.setAnswer(sb.toString());
        } else {
            // JUDGE 答案转为大写（TRUE/FALSE）
            if ("JUDGE".equals(question.getType()) && answer.getAnswer() != null) {
                answer.setAnswer(answer.getAnswer().trim().toUpperCase());
            }
            if (answer.getAnswer() == null) {
                answer.setAnswer("");
            }
        }
        questionAnswerMapper.insert(answer);
    }

    /**
     * 根据 topic 复用或新建顶级分类
     */
    private Category getOrCreateCategory(String topic) {
        String cleanTopic = cleanTopic(topic);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, cleanTopic);
        Category exist = categoryService.getOne(wrapper, false);
        if (exist != null && exist.getId() != null) {
            return exist;
        }
        Category category = new Category();
        category.setName(cleanTopic);
        category.setParentId(0L);
        category.setSort(99);
        categoryService.save(category);
        return category;
    }

    private String buildPaperName(String topic) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmm");
        String clean = cleanTopic(topic);
        if (clean.isEmpty()) {
            clean = "AI 试卷";
        }
        return "AI卷-" + clean + "-" + sdf.format(new Date());
    }

    /**
     * 清洗主题文本：去除 markdown 强调符（**、_、#、`、~）并压缩空白
     */
    private String cleanTopic(String topic) {
        if (topic == null) {
            topic = "";
        }
        return topic.replaceAll("[*_#`~]+", "").replaceAll("\\s+", " ").trim();
    }

    private String difficultyText(String difficulty) {
        if (difficulty == null) {
            return "中等";
        }
        return switch (difficulty) {
            case "EASY" -> "简单";
            case "HARD" -> "困难";
            default -> "中等";
        };
    }
}