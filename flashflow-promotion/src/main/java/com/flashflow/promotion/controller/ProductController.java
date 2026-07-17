package com.flashflow.promotion.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.common.domain.ErrorCode;
import com.flashflow.common.domain.R;
import com.flashflow.common.exception.BusinessException;
import com.flashflow.common.context.UserContext;
import com.flashflow.promotion.entity.ProductSku;
import com.flashflow.promotion.entity.ProductSpu;
import com.flashflow.promotion.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理控制器（业务逻辑委托给 ProductService）
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/flashflow/promotion/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // ========== SPU 管理 ==========
    @Operation(summary = "商品SPU分页")
    @GetMapping("/spu/page")
    public R<IPage<ProductSpu>> spuPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        return R.ok(productService.spuPage(new Page<>(page, size), keyword));
    }

    @Operation(summary = "商品SPU详情")
    @GetMapping("/spu/{id}")
    public R<ProductSpu> spuDetail(@PathVariable Long id) {
        return R.ok(productService.spuDetail(id));
    }

    @Operation(summary = "创建SPU（管理员）")
    @PostMapping("/spu")
    public R<Void> createSpu(@RequestBody ProductSpu spu) {
        requireAdmin();
        productService.createSpu(spu);
        return R.ok();
    }

    @Operation(summary = "修改SPU（管理员）")
    @PutMapping("/spu")
    public R<Void> updateSpu(@RequestBody ProductSpu spu) {
        requireAdmin();
        productService.updateSpu(spu);
        return R.ok();
    }

    // ========== SKU 管理 ==========
    @Operation(summary = "SPU下的SKU列表")
    @GetMapping("/sku/{spuId}")
    public R<List<ProductSku>> skuList(@PathVariable Long spuId) {
        return R.ok(productService.skuList(spuId));
    }

    @Operation(summary = "创建SKU（管理员）")
    @PostMapping("/sku")
    public R<Void> createSku(@RequestBody ProductSku sku) {
        requireAdmin();
        productService.createSku(sku);
        return R.ok();
    }

    @Operation(summary = "修改SKU（管理员）")
    @PutMapping("/sku")
    public R<Void> updateSku(@RequestBody ProductSku sku) {
        requireAdmin();
        productService.updateSku(sku);
        return R.ok();
    }

    @Operation(summary = "删除SKU（管理员）")
    @DeleteMapping("/sku/{id}")
    public R<Void> deleteSku(@PathVariable Long id) {
        requireAdmin();
        productService.deleteSku(id);
        return R.ok();
    }

    @Operation(summary = "所有上架商品（商城用）")
    @GetMapping("/spu/active")
    public R<List<ProductSpu>> activeSpus() {
        return R.ok(productService.activeSpus());
    }

    @Operation(summary = "所有上架SKU（商城用）")
    @GetMapping("/sku/active")
    public R<List<ProductSku>> activeSkus() {
        return R.ok(productService.activeSkus());
    }
}
