package com.atguigu.exam.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 百炼（DashScope / 通义千问）模型配置
 * 手动装配 ChatModel 与 EmbeddingModel，避免 starter 自动配置的 Bean 名冲突
 *
 * 降级策略（无 DashScope API key 时）：
 * - ChatModel 回退到 Kimi（OpenAI 兼容接口，使用 kimi.api-key 配置）
 * - EmbeddingModel 使用本地 BGE 模型（仅用于提供合法 bean 维度，真实 RAG 需要 DashScope key）
 */
@Configuration
public class DashScopeConfig {

    private static final Logger log = LoggerFactory.getLogger(DashScopeConfig.class);

    /** 阿里云百炼 DashScope OpenAI 兼容接口地址 */
    private static final String DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.chat-model:qwen-max}")
    private String chatModelName;

    @Value("${dashscope.paper-model:}")
    private String paperModelName;

    @Value("${dashscope.embedding-model:text-embedding-v3}")
    private String embeddingModelName;

    @Value("${kimi.api.uri:}")
    private String kimiUri;

    @Value("${kimi.api.api-key:}")
    private String kimiApiKey;

    @Value("${kimi.api.model:moonshot-v1-32k}")
    private String kimiModel;

    @Bean(name = "qwenChatModel")
    public ChatLanguageModel qwenChatModel() {
        if (StringUtils.hasText(apiKey)) {
            log.info("使用阿里云百炼 DashScope 模型: {}（OpenAI 兼容接口）", chatModelName);
            return buildOpenAiChatModel(chatModelName);
        }
        // 降级：回退到 Kimi（Moonshot）OpenAI 兼容接口
        log.warn("未配置 dashscope.api-key，AI 客服回退到 Kimi(Moonshot) 模型: {}", kimiModel);
        String baseUrl = kimiUri;
        int idx = baseUrl.indexOf("/chat/completions");
        if (idx > 0) {
            baseUrl = baseUrl.substring(0, idx);
        }
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(kimiApiKey)
                .modelName(kimiModel)
                .timeout(java.time.Duration.ofMinutes(3))
                .maxRetries(1)
                .build();
    }

    /**
     * 试卷生成专用模型（默认跟随主聊天模型 kimi-k3，可通过 dashscope.paper-model 覆盖）
     */
    @Bean(name = "paperChatModel")
    public ChatLanguageModel paperChatModel() {
        if (StringUtils.hasText(apiKey)) {
            String modelName = StringUtils.hasText(paperModelName) ? paperModelName : chatModelName;
            log.info("使用 DashScope 试卷生成模型: {}（OpenAI 兼容接口）", modelName);
            return buildOpenAiChatModel(modelName);
        }
        return qwenChatModel();
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
        if (StringUtils.hasText(apiKey)) {
            log.info("使用 DashScope 通义千问向量模型: {}", embeddingModelName);
            return QwenEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .modelName(embeddingModelName)
                    .build();
        }
        // 降级：本地 BGE 模型，避免缺少 DashScope key 导致启动失败
        log.warn("未配置 dashscope.api-key，向量模型回退到本地 BGE 模型（仅用于降级，RAG 检索需要 DashScope key）");
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }
}