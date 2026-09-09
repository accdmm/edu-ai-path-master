package com.atguigu.exam.service;

import com.atguigu.exam.vo.AiGenerateRequestVo;
import com.atguigu.exam.vo.QuestionImportVo;

import java.util.List;

/**
 * AI 试卷生成服务接口 - 调用大模型生成整套试卷题目
 *
 * 与 KimiAiServiceImpl（走 Moonshot）不同，本服务注入 DashScope 的 qwenChatModel，
 * 无需额外配置 KIMI_API_KEY，复用用户已配置的 DASHSCOPE_API_KEY。
 */
public interface PaperGenerateAiService {

    /**
     * 调用大模型生成整套试卷的题目列表
     *
     * @param request 出题入参（topic / count / types / difficulty / requirements）
     * @return 结构化题目列表，落库时按 QuestionImportVo 转换为 Question
     */
    List<QuestionImportVo> generatePaperQuestions(AiGenerateRequestVo request);
}