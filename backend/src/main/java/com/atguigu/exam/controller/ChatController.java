package com.atguigu.exam.controller;

import com.atguigu.exam.agent.XiaohuAgent;
import com.atguigu.exam.common.Result;
import com.atguigu.exam.entity.ChatMessages;
import com.atguigu.exam.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@CrossOrigin
@RequestMapping("/api/chat")
@Tag(name = "智能客服模块")
public class ChatController {

    @Autowired
    private XiaohuAgent xiaohuAgent;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Operation(summary = "与智能客服聊天")
    @PostMapping
    public Result<String> chat(@RequestBody ChatMessages chatMessages) {
        Long userId = getUserIdFromToken();

        String result = xiaohuAgent.chat(userId, chatMessages.getContent());
        if (ObjectUtils.isEmpty(result)) {
            return Result.error("智能客服回复为空");
        }
        return Result.success(result, "智能客服回复成功");
    }

    @Operation(summary = "获取聊天记录列表")
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getChatHistory() {
        Long userId = getUserIdFromToken();

        Criteria criteria = Criteria.where("messageId").is(userId);
        Query query = new Query(criteria);
        List<ChatMessages> chatMessagesList = mongoTemplate.find(query, ChatMessages.class);

        List<Map<String, Object>> records = chatMessagesList.stream().map(msg -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", msg.getId().toHexString());
            record.put("title", extractTitle(msg.getContent()));
            record.put("content", msg.getContent());
            record.put("createTime", msg.getCreateTime() != null ? msg.getCreateTime() : System.currentTimeMillis());
            return record;
        }).toList();

        return Result.success(records);
    }

    @Operation(summary = "删除指定聊天记录")
    @DeleteMapping("/history/{id}")
    public Result<String> deleteChatRecord(@PathVariable String id) {
        Long userId = getUserIdFromToken();

        ObjectId objectId = new ObjectId(id);
        Criteria criteria = Criteria.where("_id").is(objectId).and("messageId").is(userId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);

        if (chatMessages == null) {
            return Result.error("聊天记录不存在或无权删除");
        }

        mongoTemplate.remove(query, ChatMessages.class);
        return Result.success("删除成功");
    }

    @Operation(summary = "清空所有聊天记录")
    @DeleteMapping("/history/clear")
    public Result<String> clearAllChatRecords() {
        Long userId = getUserIdFromToken();

        Criteria criteria = Criteria.where("messageId").is(userId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);

        return Result.success("清空成功");
    }

    /**
     * 从 JWT Token 中获取用户 ID
     */
    private Long getUserIdFromToken() {
        String token = getTokenFromRequest();

        if (token != null) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            log.info("从 JWT Token 中解析到用户 ID: {}", userId);
            return userId;
        }

        log.warn("无法获取用户 ID，使用默认值：1");
        return 1L;
    }

    /**
     * 从 HttpServletRequest 中获取 token
     */
    private String getTokenFromRequest() {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * 从聊天内容中提取标题（优先取第一条 USER 消息，其次第一条非空 AI 消息，跳过 SYSTEM/TOOL 消息）
     */
    private String extractTitle(String content) {
        if (ObjectUtils.isEmpty(content)) {
            return "新对话";
        }
        try {
            if (content.startsWith("[")) {
                com.fasterxml.jackson.databind.JsonNode jsonNode =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);
                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    String title = extractTitleFromMessages(jsonNode);
                    if (!ObjectUtils.isEmpty(title)) {
                        return title.length() > 50 ? title.substring(0, 50) + "..." : title;
                    }
                }
            }
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        } catch (Exception e) {
            return "新对话";
        }
    }

    private String extractTitleFromMessages(com.fasterxml.jackson.databind.JsonNode messages) {
        for (com.fasterxml.jackson.databind.JsonNode message : messages) {
            String type = message.path("type").asText();
            if ("USER".equals(type)) {
                String text = extractMessageText(message);
                if (!ObjectUtils.isEmpty(text)) {
                    return text;
                }
            }
        }
        for (com.fasterxml.jackson.databind.JsonNode message : messages) {
            String type = message.path("type").asText();
            if ("SYSTEM".equals(type)) {
                continue;
            }
            String text = extractMessageText(message);
            if (!ObjectUtils.isEmpty(text)) {
                return text;
            }
        }
        return "";
    }

    /**
     * 兼容 LangChain4j JSON 格式的消息文本提取：
     * USER {type:"USER", contents:[{type:"TEXT",text:"..."}]}
     * AI   {type:"AI",   text:"..."}
     */
    private String extractMessageText(com.fasterxml.jackson.databind.JsonNode message) {
        if (message == null) {
            return "";
        }
        // USER 消息的字段是 contents（复数），AI 消息用 text
        com.fasterxml.jackson.databind.JsonNode contentsNode = message.path("contents");
        if (!contentsNode.isArray()) {
            contentsNode = message.path("content");
        }
        if (contentsNode.isArray() && contentsNode.size() > 0) {
            StringBuilder sb = new StringBuilder();
            contentsNode.forEach(segment -> {
                com.fasterxml.jackson.databind.JsonNode text = segment.path("text");
                if (text.isTextual()) {
                    sb.append(text.asText());
                }
            });
            return sb.toString();
        }
        com.fasterxml.jackson.databind.JsonNode text = message.path("text");
        if (text.isTextual()) {
            return text.asText();
        }
        return "";
    }
}