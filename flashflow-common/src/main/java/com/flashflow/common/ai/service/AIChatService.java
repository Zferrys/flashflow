package com.flashflow.common.ai.service;

import com.flashflow.common.ai.dto.ChatResponse;
import com.flashflow.common.ai.knowledge.KnowledgeBaseService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 聊天服务：RAG 检索 + DeepSeek 生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private static final String SYSTEM_PROMPT_NO_RAG =
            "你是 FlashFlow 闪购平台的 AI 助手。你可以帮助用户了解秒杀流程、优惠券规则、"
                    + "退款政策、订单状态等平台相关问题。请用中文回答，简洁专业。";

    private final OpenAiChatModel chatModel;
    private final KnowledgeBaseService knowledgeBaseService;

    /** 会话记忆（按 sessionId 隔离） */
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    /**
     * 对话入口：RAG 检索增强 → DeepSeek 生成回复
     */
    public ChatResponse chat(String message, String sessionId) {
        // 1. RAG 语义检索
        List<EmbeddingMatch<TextSegment>> matches = knowledgeBaseService.search(message, 3);

        // 2. 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();

        // 系统提示（如有 RAG 结果则注入知识）
        String systemPrompt = buildSystemPrompt(matches);
        messages.add(SystemMessage.from(systemPrompt));

        // 3. 多轮对话历史
        ChatMemory memory = getOrCreateMemory(sessionId);
        messages.addAll(memory.messages());

        // 4. 当前用户消息
        messages.add(UserMessage.from(message));

        // 5. 调用 DeepSeek 生成
        dev.langchain4j.model.chat.response.ChatResponse lcResponse = chatModel.chat(messages);
        String reply = lcResponse.aiMessage().text();

        // 6. 保存对话到记忆
        memory.add(UserMessage.from(message));
        memory.add(AiMessage.from(reply));

        // 7. 构建响应
        if (!matches.isEmpty()) {
            List<String> sources = matches.stream()
                    .map(m -> m.embedded().text())
                    .map(t -> t.length() > 50 ? t.substring(0, 50) + "..." : t)
                    .toList();
            log.debug("RAG 命中 {} 条，来源: {}", matches.size(), sources);
            return ChatResponse.rag(reply, sources);
        }
        return ChatResponse.of(reply);
    }

    /**
     * 清除指定会话记忆
     */
    public void clearMemory(String sessionId) {
        sessionMemories.remove(sessionId);
        log.debug("会话 {} 记忆已清除", sessionId);
    }

    /**
     * 构建系统提示：如有 RAG 结果则注入检索到的知识
     */
    private String buildSystemPrompt(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return SYSTEM_PROMPT_NO_RAG;
        }
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT_NO_RAG);
        sb.append("\n\n请参考以下平台知识回答用户问题：\n---\n");
        for (int i = 0; i < matches.size(); i++) {
            String text = matches.get(i).embedded().text();
            sb.append("[知识").append(i + 1).append("] ").append(text).append("\n");
        }
        sb.append("---\n如果以上知识与用户问题无关，请忽略它们，用自己的知识回答。");
        return sb.toString();
    }

    /**
     * 获取或创建会话记忆（默认保留最近 20 轮消息）
     */
    private ChatMemory getOrCreateMemory(String sessionId) {
        return sessionMemories.computeIfAbsent(sessionId,
                k -> MessageWindowChatMemory.withMaxMessages(20));
    }
}
