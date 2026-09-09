package com.atguigu.exam.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class EmbeddingStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStoreConfig.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${pinecone.api-key:}")
    private String pineconeApiKey;

    @Value("${pinecone.index:edu-ai-index}")
    private String pineconeIndex;

    @Value("${pinecone.namespace:edu-namespace}")
    private String pineconeNamespace;

    @Value("${pinecone.cloud:AWS}")
    private String pineconeCloud;

    @Value("${pinecone.region:us-east-1}")
    private String pineconeRegion;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if (StringUtils.hasText(pineconeApiKey)) {
            log.info("使用 Pinecone 向量存储 index={}, namespace={}", pineconeIndex, pineconeNamespace);
            return PineconeEmbeddingStore.builder()
                    .apiKey(pineconeApiKey)
                    .index(pineconeIndex)
                    .nameSpace(pineconeNamespace)
                    .createIndex(PineconeServerlessIndexConfig.builder()
                            .cloud(pineconeCloud)
                            .region(pineconeRegion)
                            .dimension(embeddingModel.dimension())
                            .build())
                    .build();
        }
        // 降级：内存向量存储，避免缺少 Pinecone key 导致启动失败
        log.warn("未配置 pinecone.api-key，向量存储回退到内存存储（知识库检索不可用，AI 客服仅支持通用问答）");
        return new InMemoryEmbeddingStore<>();
    }

}