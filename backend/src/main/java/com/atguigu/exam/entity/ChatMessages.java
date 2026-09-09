package com.atguigu.exam.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessages {

    @Id
    private ObjectId id;

    /**
     * 用户 ID，用于聚合同一用户的所有对话记录，作为 LangChain4j 的 @MemoryId
     */
    private int messageId;

    /**
     * LangChain4j 序列化的聊天消息 JSON
     */
    private String content;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

}