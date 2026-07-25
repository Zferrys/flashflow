package com.flashflow.common.ai.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 知识文档实体
 */
@Data
@AllArgsConstructor
public class KnowledgeDocument {

    /** 文档唯一标识 */
    private String id;

    /** 文档标题 */
    private String title;

    /** 文档内容（会被向量化检索） */
    private String content;

    /** 分类：help（帮助文档）、product（商品）、activity（活动）、coupon（优惠券） */
    private String category;
}
