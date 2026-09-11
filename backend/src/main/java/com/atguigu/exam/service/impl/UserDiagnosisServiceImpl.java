package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.AnswerRecord;
import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.ExamRecord;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.AnswerRecordMapper;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.UserDiagnosisService;
import com.atguigu.exam.vo.LearningPathDetailVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户知识点掌握度诊断实现（与学习分析页同口径）
 * 已出分考试 → 答题记录 → 按题目分类聚合得分率，返回按得分率升序（最薄弱在前）
 */
@Slf4j
@Service
public class UserDiagnosisServiceImpl implements UserDiagnosisService {

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<LearningPathDetailVo.DiagnosisItemVo> buildDiagnosis(Long userId) {
        List<ExamRecord> examRecords = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .isNotNull(ExamRecord::getScore));
        if (examRecords.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> examRecordIds = examRecords.stream()
                .map(ExamRecord::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<AnswerRecord> answerRecords = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .in(AnswerRecord::getExamRecordId, examRecordIds));
        if (answerRecords.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Question> questionMap = buildQuestionMap(answerRecords);
        Map<String, int[]> masteryMap = new LinkedHashMap<>();
        Map<Long, Category> categoryCache = new HashMap<>();
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
            String categoryName = category != null && category.getName() != null
                    ? normalizeName(category.getName()) : "未分类";
            int[] agg = masteryMap.computeIfAbsent(categoryName, k -> new int[3]);
            agg[0]++; // answerCount
            agg[1] += record.getScore() == null ? 0 : record.getScore(); // earned
            agg[2] += question.getScore() == null ? 0 : question.getScore(); // max
        }

        List<LearningPathDetailVo.DiagnosisItemVo> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : masteryMap.entrySet()) {
            int[] agg = entry.getValue();
            if (agg[2] <= 0) {
                continue;
            }
            LearningPathDetailVo.DiagnosisItemVo item = new LearningPathDetailVo.DiagnosisItemVo();
            item.setCategoryName(entry.getKey());
            item.setAnswerCount(agg[0]);
            item.setCorrectRate(Math.round(agg[1] * 100f / agg[2]));
            result.add(item);
        }
        result.sort(Comparator.comparingInt(LearningPathDetailVo.DiagnosisItemVo::getCorrectRate));
        return result;
    }

    /**
     * 查询答题记录涉及的全部题目，构建题目ID -> 题目映射
     */
    private Map<Long, Question> buildQuestionMap(List<AnswerRecord> answerRecords) {
        Set<Long> questionIds = answerRecords.stream()
                .map(AnswerRecord::getQuestionId).filter(Objects::nonNull)
                .map(Long::valueOf).collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return new HashMap<>();
        }
        return questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
    }

    /**
     * 清洗展示文本：去除 markdown 强调符并压缩空白，空值回退"未分类"
     */
    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "未分类";
        }
        return name.replaceAll("[*_#`~]+", " ").replaceAll("\\s+", " ").trim();
    }
}
