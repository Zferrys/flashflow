package com.flashflow.common.ai.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库管理：初始化文档 → 向量化入库 → 语义检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 应用启动时初始化知识库
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化 AI 知识库...");
        List<KnowledgeDocument> docs = loadStaticDocuments();
        int count = 0;
        for (KnowledgeDocument doc : docs) {
            TextSegment segment = TextSegment.from(doc.getContent());
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
            count++;
        }
        log.info("AI 知识库初始化完成，共入库 {} 篇文档", count);
    }

    /**
     * 语义检索：根据用户问题匹配最相关的知识文档
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(TextSegment.from(query)).content();
        return embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .minScore(0.5)
                        .build()
        ).matches();
    }

    /**
     * 刷新知识库（管理员手动触发或定时任务调用）
     */
    public void refresh() {
        log.info("刷新 AI 知识库...");
        // 简单实现：清空重建。后续可改为增量更新
        embeddingStore.removeAll();
        init();
    }

    /**
     * 加载静态帮助文档
     */
    private List<KnowledgeDocument> loadStaticDocuments() {
        List<KnowledgeDocument> docs = new ArrayList<>();

        docs.add(new KnowledgeDocument(
                "seckill-intro",
                "秒杀流程说明",
                "FlashFlow 秒杀流程：1. 用户在活动页面选择商品点击抢购；"
                        + "2. 系统校验用户登录状态和限购次数（每人每场限购1件）；"
                        + "3. 校验通过后，通过 Redis Lua 脚本原子扣减库存；"
                        + "4. 扣减成功后生成预订单，用户需在30分钟内完成支付；"
                        + "5. 超时未支付自动释放库存，恢复限购次数。",
                "help"
        ));

        docs.add(new KnowledgeDocument(
                "coupon-rules",
                "优惠券使用规则",
                "优惠券使用规则：1. 每种优惠券每人限领1张，先到先得；"
                        + "2. 优惠券有有效期，过期自动失效；"
                        + "3. 订单金额需满足优惠券最低消费门槛才能使用；"
                        + "4. 优惠券不可叠加使用，每笔订单限用1张；"
                        + "5. 已使用的优惠券不可退还，退款时优惠金额不予退回；"
                        + "6. 优惠券可在「我的优惠券」页面查看和管理。",
                "help"
        ));

        docs.add(new KnowledgeDocument(
                "refund-policy",
                "退款政策",
                "退款政策：1. 未支付订单30分钟后自动取消；"
                        + "2. 已支付未发货订单可申请退款，审核通过后1-3个工作日到账；"
                        + "3. 已发货订单需先拒收或退货，仓库确认收货后退款；"
                        + "4. 秒杀商品不支持无理由退货（质量问题除外）；"
                        + "5. 退款金额退回原支付方式（支付宝）。",
                "help"
        ));

        docs.add(new KnowledgeDocument(
                "order-status",
                "订单状态说明",
                "订单状态流转：待支付 → 已支付 → 已发货 → 已签收。"
                        + "取消订单可在「待支付」或「已支付」状态下发起。"
                        + "退款需在「已支付」状态下申请，「已发货」状态需先拒收。"
                        + "订单超时自动取消规则：待支付订单30分钟后自动取消，恢复库存。",
                "help"
        ));

        return docs;
    }
}
