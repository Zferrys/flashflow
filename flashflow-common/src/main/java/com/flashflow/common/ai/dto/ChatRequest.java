package com.flashflow.common.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** 用户消息 */
    @NotBlank(message = "消息不能为空")
    private String message;

    /** 会话ID（用于多轮对话记忆） */
    private String sessionId;
}
