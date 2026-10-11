package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.entity.WrongQuestion;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.mapper.WrongQuestionMapper;
import com.atguigu.exam.service.AnswerRecordService;
import com.atguigu.exam.service.CategoryService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.service.WrongQuestionService;
import com.atguigu.exam.vo.WrongQuestionStatsVo;
import com.atguigu.exam.vo.WrongQuestionVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 错题本服务实现
 *
 * 采集规则：
 * - 答错 is_correct=0 → 状态 UNMASTERED（未掌握），wrong_count+1，correct_streak 归零
 * - 部分正确 is_correct=2 → 状态 LEARNING（半掌握），同上
 * - 答对 is_correct=1 不在本模块处理（掌握度状态机在后续模块完善）
 * 幂等：以 last_exam_record_id 判定本场考试是否已采集过该题，判卷重跑/重复触发不会重复计数
 */
@Slf4j
@Service
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion> implements WrongQuestionService {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @Autowired
    private AnswerRecordService answerRecordService;

    @Override
    public void syncFromGrading(Long userId, Integer examRecordId,
                                List<AnswerRecord> answerRecords, Map<Long, Question> questionMap) {
        if (userId == null || examRecordId == null || ObjectUtils.isEmpty(answerRecords) || questionMap == null) {
            return;
        }
        List<WrongQuestion> toInsert = new ArrayList<>();
        List<WrongQuestion> toUpdate = new ArrayList<>();
        int masteredCount = 0;
        for (AnswerRecord ar : answerRecords) {
            if (ar.getQuestionId() == null || ar.getIsCorrect() == null) {
                continue;
            }
            long questionId = ar.getQuestionId().longValue();
            WrongQuestion existing = getOne(new LambdaQueryWrapper<WrongQuestion>()
                    .eq(WrongQuestion::getUserId, userId)
                    .eq(WrongQuestion::getQuestionId, questionId), false);
            // 幂等：本场考试已同步过该题则跳过
            if (existing != null && existing.getLastExamRecordId() != null
                    && existing.getLastExamRecordId() == examRecordId.longValue()) {
                continue;
            }
            int isCorrect = ar.getIsCorrect();
            if (isCorrect == 1) {
                // 答对：仅对已收录的错题累计"连续答对"回血，未收录的答对题不入库
                if (existing == null) {
                    continue;
                }
                int streak = (existing.getCorrectStreak() == null ? 0 : existing.getCorrectStreak()) + 1;
                existing.setCorrectStreak(streak);
                // 达到阈值自动掌握；已达 MASTERED 的题保持掌握状态
                if (streak >= MASTER_STREAK && !"MASTERED".equals(existing.getStatus())) {
                    existing.setStatus("MASTERED");
                    masteredCount++;
                }
                existing.setLastExamRecordId(examRecordId.longValue());
                toUpdate.add(existing);
                continue;
            }
            // 答错(0)/部分正确(2)：状态回落（已掌握后再次答错同样回退），连续答对归零
            Question question = questionMap.get(questionId);
            Long categoryId = question == null ? null : question.getCategoryId();
            String status = isCorrect == 2 ? "LEARNING" : "UNMASTERED";
            if (existing == null) {
                // 先物理清理同题残留（含历史逻辑删除行），避免唯一键 uk_user_question 冲突导致收录失败
                baseMapper.purgeByUserAndQuestion(userId, questionId);
                WrongQuestion wrong = new WrongQuestion();
                wrong.setUserId(userId);
                wrong.setQuestionId(questionId);
                wrong.setCategoryId(categoryId);
                wrong.setWrongCount(1);
                wrong.setCorrectStreak(0);
                wrong.setStatus(status);
                wrong.setLastWrongTime(LocalDateTime.now());
                wrong.setLastExamRecordId(examRecordId.longValue());
                toInsert.add(wrong);
            } else {
                existing.setWrongCount((existing.getWrongCount() == null ? 0 : existing.getWrongCount()) + 1);
                existing.setCorrectStreak(0);
                existing.setStatus(status);
                existing.setLastWrongTime(LocalDateTime.now());
                existing.setLastExamRecordId(examRecordId.longValue());
                if (existing.getCategoryId() == null && categoryId != null) {
                    existing.setCategoryId(categoryId);
                }
                toUpdate.add(existing);
            }
        }
        // 下一场考试答对同一题会再次命中更新（last_exam_record_id 已刷新，幂等不受影响）
        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }
        if (!toInsert.isEmpty() || !toUpdate.isEmpty() || masteredCount > 0) {
            log.info("错题本同步完成 examRecordId={} userId={} 新增{}条 更新{}条 新掌握{}条",
                    examRecordId, userId, toInsert.size(), toUpdate.size(), masteredCount);
        }
    }

    @Override
    public void toggleMaster(Long userId, Long id) {
        WrongQuestion wrong = getOwned(userId, id);
        if ("MASTERED".equals(wrong.getStatus())) {
            // 取消掌握：回落未掌握并清零连续答对，避免"取消后仍满足自动掌握条件"
            wrong.setStatus("UNMASTERED");
            wrong.setCorrectStreak(0);
        } else {
            wrong.setStatus("MASTERED");
            wrong.setCorrectStreak(MASTER_STREAK);
        }
        updateById(wrong);
    }

    @Override
    public void removeWrongQuestion(Long userId, Long id) {
        getOwned(userId, id);
        // 物理删除：避免逻辑删除残留行让该题无法被重新收录（唯一键不含 is_deleted）
        baseMapper.purgeOwnedById(id, userId);
    }

    /**
     * 取指定错题并校验归属（仅本人可操作）
     */
    private WrongQuestion getOwned(Long userId, Long id) {
        WrongQuestion wrong = getById(id);
        if (wrong == null) {
            throw new RuntimeException("错题记录不存在或已被移除！");
        }
        if (!wrong.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人的错题！");
        }
        return wrong;
    }

    @Override
    public Page<WrongQuestionVo> pageWrongQuestions(Long userId, long page, long size, Long categoryId, String status) {
        Page<WrongQuestion> pageBean = new Page<>(page, size);
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId);
        wrapper.eq(categoryId != null, WrongQuestion::getCategoryId, categoryId);
        wrapper.eq(StringUtils.hasText(status), WrongQuestion::getStatus, status);
        wrapper.orderByDesc(WrongQuestion::getLastWrongTime);
        page(pageBean, wrapper);

        Page<WrongQuestionVo> result = new Page<>(page, size);
        result.setTotal(pageBean.getTotal());
        result.setRecords(assembleVos(pageBean.getRecords()));
        return result;
    }

    /**
     * 批量装配错题详情：避免逐题查询（N+1），整页仅 5 次批量查询
     */
    private List<WrongQuestionVo> assembleVos(List<WrongQuestion> records) {
        if (ObjectUtils.isEmpty(records)) {
            return new ArrayList<>();
        }
        List<Long> questionIds = records.stream()
                .map(WrongQuestion::getQuestionId).filter(Objects::nonNull).distinct().toList();

        // 题目（题干/题型/难度）
        Map<Long, Question> questionMap = questionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
        // 选项（按 sort 排序）
        Map<Long, List<QuestionChoice>> choiceMap = questionIds.isEmpty() ? Map.of()
                : questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>()
                        .in(QuestionChoice::getQuestionId, questionIds)
                        .orderByAsc(QuestionChoice::getSort)).stream()
                .collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        // 标准答案
        Map<Long, String> answerMap = questionIds.isEmpty() ? Map.of()
                : questionAnswerMapper.selectList(new LambdaQueryWrapper<QuestionAnswer>()
                        .in(QuestionAnswer::getQuestionId, questionIds)).stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, QuestionAnswer::getAnswer, (a, b) -> a));
        // 分类名称
        Set<Long> categoryIds = records.stream().map(WrongQuestion::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        // 最近一次作答（按 考试记录ID + 题目ID 定位）
        List<Long> examRecordIds = records.stream().map(WrongQuestion::getLastExamRecordId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, AnswerRecord> lastAnswerMap = new HashMap<>();
        if (!examRecordIds.isEmpty() && !questionIds.isEmpty()) {
            List<AnswerRecord> lastAnswers = answerRecordService.list(new LambdaQueryWrapper<AnswerRecord>()
                    .in(AnswerRecord::getExamRecordId, examRecordIds)
                    .in(AnswerRecord::getQuestionId, questionIds));
            for (AnswerRecord ar : lastAnswers) {
                lastAnswerMap.put(ar.getExamRecordId() + "_" + ar.getQuestionId(), ar);
            }
        }

        List<WrongQuestionVo> vos = new ArrayList<>();
        for (WrongQuestion wq : records) {
            WrongQuestionVo vo = new WrongQuestionVo();
            vo.setId(wq.getId());
            vo.setQuestionId(wq.getQuestionId());
            vo.setCategoryId(wq.getCategoryId());
            vo.setCategoryName(wq.getCategoryId() == null ? null : categoryNameMap.get(wq.getCategoryId()));
            vo.setWrongCount(wq.getWrongCount());
            vo.setCorrectStreak(wq.getCorrectStreak());
            vo.setStatus(wq.getStatus());
            vo.setLastWrongTime(wq.getLastWrongTime());

            Question question = questionMap.get(wq.getQuestionId());
            if (question != null) {
                vo.setTitle(question.getTitle());
                vo.setType(question.getType());
                vo.setMulti(question.getMulti());
                vo.setDifficulty(question.getDifficulty());
                vo.setAnalysis(question.getAnalysis());
            }
            vo.setChoices(choiceMap.get(wq.getQuestionId()));
            vo.setCorrectAnswer(answerMap.get(wq.getQuestionId()));

            if (wq.getLastExamRecordId() != null) {
                AnswerRecord lastAnswer = lastAnswerMap.get(wq.getLastExamRecordId() + "_" + wq.getQuestionId());
                if (lastAnswer != null) {
                    vo.setUserAnswer(lastAnswer.getUserAnswer());
                    vo.setAiCorrection(lastAnswer.getAiCorrection());
                }
            }
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public WrongQuestionStatsVo stats(Long userId) {
        List<WrongQuestion> all = list(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId));
        WrongQuestionStatsVo vo = new WrongQuestionStatsVo();
        vo.setTotal(all.size());
        vo.setUnmastered((int) all.stream().filter(w -> "UNMASTERED".equals(w.getStatus())).count());
        vo.setLearning((int) all.stream().filter(w -> "LEARNING".equals(w.getStatus())).count());
        vo.setMastered((int) all.stream().filter(w -> "MASTERED".equals(w.getStatus())).count());

        // 按知识点聚合（分类ID可空归入"未分类"）
        Set<Long> categoryIds = all.stream().map(WrongQuestion::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of()
                : categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        Map<Long, WrongQuestionStatsVo.CategoryStat> statMap = new HashMap<>();
        Set<Long> seenKeys = new HashSet<>();
        for (WrongQuestion wq : all) {
            Long key = wq.getCategoryId() == null ? -1L : wq.getCategoryId();
            seenKeys.add(key);
            WrongQuestionStatsVo.CategoryStat stat = statMap.computeIfAbsent(key, k -> {
                WrongQuestionStatsVo.CategoryStat s = new WrongQuestionStatsVo.CategoryStat();
                s.setCategoryId(wq.getCategoryId());
                s.setCategoryName(wq.getCategoryId() == null ? "未分类" : categoryNameMap.get(wq.getCategoryId()));
                s.setTotal(0);
                s.setUnmastered(0);
                s.setLearning(0);
                s.setMastered(0);
                return s;
            });
            stat.setTotal(stat.getTotal() + 1);
            switch (wq.getStatus() == null ? "" : wq.getStatus()) {
                case "MASTERED" -> stat.setMastered(stat.getMastered() + 1);
                case "LEARNING" -> stat.setLearning(stat.getLearning() + 1);
                default -> stat.setUnmastered(stat.getUnmastered() + 1);
            }
        }
        List<WrongQuestionStatsVo.CategoryStat> categories = new ArrayList<>(statMap.values());
        categories.sort((a, b) -> b.getTotal() - a.getTotal());
        vo.setCategories(categories);
        return vo;
    }
}