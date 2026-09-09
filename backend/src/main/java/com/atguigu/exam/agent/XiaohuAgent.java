package com.atguigu.exam.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetrieverXiaoHuPincone"
)
public interface XiaohuAgent {

    @SystemMessage(XiaohuSystemPrompt.SYSTEM)
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);

}