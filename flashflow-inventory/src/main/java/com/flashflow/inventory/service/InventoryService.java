package com.flashflow.inventory.service;

import com.flashflow.inventory.entity.Inventory;
import com.flashflow.inventory.entity.InventoryShard;

import java.util.List;

/**
 * 库存服务接口
 */
public interface InventoryService {

    /** 查询库存 */
    Inventory getBySkuId(Long skuId);

    /** 查询库存分片列表 */
    List<InventoryShard> getShards(Long skuId);

    /**
     * 扣库存（核心方法）
     * @return 扣减结果 DTO
     */
    DeductResult deduct(DeductRequest request);

    /** 释放预扣库存 */
    boolean release(Long skuId, int quantity, String orderSn);

    /** 库存调整（管理员） */
    void adjust(Long skuId, int totalStock);

    /** Redis 预热：将 DB 库存加载到 Redis */
    void warmUp(Long skuId);

    // ========== 内部 DTO ==========

    record DeductRequest(Long skuId, Long userId, int quantity, String orderSn) {}
    record DeductResult(boolean success, int shardIndex, int stockAfter) {}
}
