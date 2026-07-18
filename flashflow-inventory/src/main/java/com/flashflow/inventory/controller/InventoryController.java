package com.flashflow.inventory.controller;

import com.flashflow.common.domain.R;
import com.flashflow.common.context.UserContext;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.inventory.entity.Inventory;
import com.flashflow.inventory.entity.InventoryShard;
import com.flashflow.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 库存控制器
 */
@Tag(name = "库存服务")
@RestController
@RequestMapping("/api/flashflow/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    @Operation(summary = "查询库存")
    @GetMapping("/{skuId}")
    public R<Inventory> getBySkuId(@PathVariable Long skuId) {
        return R.ok(inventoryService.getBySkuId(skuId));
    }

    @Operation(summary = "查询库存分片")
    @GetMapping("/{skuId}/shards")
    public R<List<InventoryShard>> getShards(@PathVariable Long skuId) {
        return R.ok(inventoryService.getShards(skuId));
    }

    /**
     * 扣库存（内部接口，由 MQ 消费者直接调用 Service，不走 HTTP）
     * @deprecated 生产环境调用请走 MQ 消息队列；HTTP 端点仅保留用于调试
     */
    @Operation(summary = "扣库存（内部接口，MQ消费者请直接调 Service）")
    @PostMapping("/deduct")
    public R<InventoryService.DeductResult> deduct(@RequestBody InventoryService.DeductRequest request) {
        return R.ok(inventoryService.deduct(request));
    }

    /**
     * 释放预扣库存（内部接口，由 MQ 消费者直接调用 Service，不走 HTTP）
     * @deprecated 生产环境调用请走 MQ 消息队列；HTTP 端点仅保留用于调试
     */
    @Operation(summary = "释放预扣库存（内部接口，MQ消费者请直接调 Service）")
    @PostMapping("/release")
    public R<Boolean> release(@RequestBody ReleaseRequest request) {
        return R.ok(inventoryService.release(request.skuId(), request.quantity(), request.orderSn()));
    }

    @Operation(summary = "库存调整（管理员）")
    @PutMapping("/adjust")
    public R<Void> adjust(@RequestBody AdjustRequest request) {
        requireAdmin();
        inventoryService.adjust(request.skuId(), request.totalStock());
        return R.ok();
    }

    @Operation(summary = "Redis 预热（管理员）")
    @PostMapping("/warm-up/{skuId}")
    public R<Void> warmUp(@PathVariable Long skuId) {
        requireAdmin();
        inventoryService.warmUp(skuId);
        return R.ok();
    }

    // ========== 请求 DTO ==========

    record ReleaseRequest(Long skuId, int quantity, String orderSn) {}
    record AdjustRequest(Long skuId, int totalStock) {}
}
