package com.flashflow.auth.controller;

import com.flashflow.common.ai.dto.ChatRequest;
import com.flashflow.common.ai.dto.ChatResponse;
import com.flashflow.common.ai.service.AIChatService;
import com.flashflow.common.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/api/flashflow/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

    /**
     * AI 对话（支持 RAG 检索增强）
     */
    @PostMapping("/chat")
    public R<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : "default";
        ChatResponse response = aiChatService.chat(request.getMessage(), sessionId);
        return R.ok(response);
    }

    /**
     * 清除指定会话记忆
     */
    @DeleteMapping("/memory/{sessionId}")
    public R<Void> clearMemory(@PathVariable String sessionId) {
        aiChatService.clearMemory(sessionId);
        return R.ok();
    }
}
