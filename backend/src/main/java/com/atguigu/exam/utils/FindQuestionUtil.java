package com.atguigu.exam.utils;

import com.atguigu.exam.service.FindQuestionsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FindQuestionUtil {

    @Autowired
    private FindQuestionsService findQuestionsService;

    @Tool(name = "根据分类名称查询题目", value = "根据用户传入的分类名称和题目难度来查询题目列表，若题目列表为空，则显示查询失败。")
    public String findQuestionsByCategory(
            @P(value = "分类名称，必须提供") String name,
            @P(value = "题目难度，允许为空，如：简单、中等、困难") String difficult) {

        log.info("查询题目 - 分类：{}, 难度：{}", name, difficult);

        if (ObjectUtils.isEmpty(name)) {
            log.warn("分类名称为空，无法查询题目");
            return "请提供分类名称才能查询题目";
        }

        try {
            List<String> questions = findQuestionsService.findQuestions(name, difficult);

            if (ObjectUtils.isEmpty(questions)) {
                log.info("未找到分类 '{}' 的题目", name);
                return "抱歉，没有找到分类为'" + name + "'的题目";
            }

            log.info("找到 {} 道题目", questions.size());

            StringBuilder result = new StringBuilder();
            result.append("找到以下题目（共").append(questions.size()).append("道）：\n");
            for (int i = 0; i < questions.size(); i++) {
                result.append(i + 1).append(". ").append(questions.get(i)).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("查询题目失败", e);
            return "查询题目时发生错误：" + e.getMessage();
        }
    }

}