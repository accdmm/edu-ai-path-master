package com.atguigu.exam.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 阿里云百炼（DashScope）模型配置 - 项目唯一 AI 服务商
 * 手动装配 ChatModel 与 EmbeddingModel，避免 starter 自动配置的 Bean 名冲突
 *
 * - 聊天/出卷/判卷/面试/客服：kimi-k3（DashScope OpenAI 兼容接口）
 * - 向量：qwen3.7-text-embedding-flash（百炼免费模型，QwenEmbeddingModel 原生接口）
 * - 未配置 dashscope.api-key 时启动即失败，尽早暴露配置问题
 */
@Configuration
public class DashScopeConfig {

    private static final Logger log = LoggerFactory.getLogger(DashScopeConfig.class);

    /** 阿里云百炼 DashScope OpenAI 兼容接口地址 */
    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.chat-model:kimi-k3}")
    private String chatModelName;

    @Value("${dashscope.paper-model:}")
    private String paperModelName;

    @Value("${dashscope.embedding-model:qwen3.7-text-embedding-flash}")
    private String embeddingModelName;

    @Bean(name = "qwenChatModel")
    public ChatLanguageModel qwenChatModel() {
        log.info("使用阿里云百炼 DashScope 模型: {}（OpenAI 兼容接口）", chatModelName);
        return buildOpenAiChatModel(chatModelName);
    }

    /**
     * 试卷生成专用模型（默认跟随主聊天模型，可通过 dashscope.paper-model 覆盖）
     */
    @Bean(name = "paperChatModel")
    public ChatLanguageModel paperChatModel() {
        String modelName = StringUtils.hasText(paperModelName) ? paperModelName : chatModelName;
        log.info("使用 DashScope 试卷生成模型: {}（OpenAI 兼容接口）", modelName);
        return buildOpenAiChatModel(modelName);
    }

    private ChatLanguageModel buildOpenAiChatModel(String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(DASHSCOPE_BASE_URL)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(java.time.Duration.ofMinutes(8))
                .maxRetries(1)
                .build();
    }

    @Bean(name = "embeddingModel")
    public EmbeddingModel embeddingModel() {
        log.info("使用阿里云百炼向量模型: {}（免费）", embeddingModelName);
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }
}
