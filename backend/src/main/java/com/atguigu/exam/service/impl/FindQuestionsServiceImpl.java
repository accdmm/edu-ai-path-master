package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.Category;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.FindQuestionsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FindQuestionsServiceImpl implements FindQuestionsService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public List<String> findQuestions(String name, String difficult) {
        log.info("查询题目 - 分类：{}, 难度：{}", name, difficult);

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, name);
        Category categories = categoryMapper.selectOne(queryWrapper);
        if (ObjectUtils.isEmpty(categories)) {
            log.warn("未找到分类：{}", name);
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Question> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Question::getCategoryId, categories.getId());

        if (ObjectUtils.isNotEmpty(difficult)) {
            queryWrapper1.eq(Question::getDifficulty, normalizeDifficulty(difficult));
        }

        List<Question> questions = questionMapper.selectList(queryWrapper1);
        if (ObjectUtils.isEmpty(questions)) {
            log.info("分类 '{}' 下没有题目", name);
            return new ArrayList<>();
        }

        return questions.stream().map(Question::getTitle).toList();
    }

    /**
     * 将中文难度映射为数据库枚举值（EASY/MEDIUM/HARD）
     */
    private String normalizeDifficulty(String difficult) {
        return switch (difficult.trim()) {
            case "简单", "EASY", "easy" -> "EASY";
            case "中等", "MEDIUM", "medium" -> "MEDIUM";
            case "困难", "HARD", "hard" -> "HARD";
            default -> difficult.trim().toUpperCase();
        };
    }
}