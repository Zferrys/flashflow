package com.flashflow.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /** AI 回复内容 */
    private String reply;

    /** 引用的知识来源（RAG 检索到的文档标题） */
    private List<String> sources;

    /** 是否为 RAG 增强回答 */
    private boolean ragEnhanced;

    public static ChatResponse of(String reply) {
        return new ChatResponse(reply, null, false);
    }

    public static ChatResponse rag(String reply, List<String> sources) {
        return new ChatResponse(reply, sources, true);
    }
}
