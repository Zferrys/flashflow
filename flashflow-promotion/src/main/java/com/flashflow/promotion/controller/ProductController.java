package com.flashflow.promotion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.common.domain.R;
import com.flashflow.promotion.dao.ProductSkuMapper;
import com.flashflow.promotion.dao.ProductSpuMapper;
import com.flashflow.promotion.entity.ProductSku;
import com.flashflow.promotion.entity.ProductSpu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/flashflow/promotion/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;

    // ========== SPU 管理 ==========
    @Operation(summary = "商品SPU分页")
    @GetMapping("/spu/page")
    public R<IPage<ProductSpu>> spuPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<ProductSpu> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) w.like(ProductSpu::getSpuName, keyword);
        w.orderByDesc(ProductSpu::getCreateTime);
        return R.ok(spuMapper.selectPage(new Page<>(page, size), w));
    }

    @Operation(summary = "商品SPU详情")
    @GetMapping("/spu/{id}")
    public R<ProductSpu> spuDetail(@PathVariable Long id) {
        return R.ok(spuMapper.selectById(id));
    }

    @Operation(summary = "创建SPU")
    @PostMapping("/spu")
    public R<Void> createSpu(@RequestBody ProductSpu spu) {
        spuMapper.insert(spu);
        return R.ok();
    }

    @Operation(summary = "修改SPU")
    @PutMapping("/spu")
    public R<Void> updateSpu(@RequestBody ProductSpu spu) {
        spuMapper.updateById(spu);
        return R.ok();
    }

    // ========== SKU 管理 ==========
    @Operation(summary = "SPU下的SKU列表")
    @GetMapping("/sku/{spuId}")
    public R<java.util.List<ProductSku>> skuList(@PathVariable Long spuId) {
        return R.ok(skuMapper.selectBySpuId(spuId));
    }

    @Operation(summary = "创建SKU")
    @PostMapping("/sku")
    public R<Void> createSku(@RequestBody ProductSku sku) {
        skuMapper.insert(sku);
        return R.ok();
    }

    @Operation(summary = "修改SKU")
    @PutMapping("/sku")
    public R<Void> updateSku(@RequestBody ProductSku sku) {
        skuMapper.updateById(sku);
        return R.ok();
    }

    @Operation(summary = "删除SKU")
    @DeleteMapping("/sku/{id}")
    public R<Void> deleteSku(@PathVariable Long id) {
        skuMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "所有上架商品（商城用）")
    @GetMapping("/spu/active")
    public R<java.util.List<ProductSpu>> activeSpus() {
        LambdaQueryWrapper<ProductSpu> w = new LambdaQueryWrapper<>();
        w.eq(ProductSpu::getStatus, 1).orderByDesc(ProductSpu::getCreateTime);
        return R.ok(spuMapper.selectList(w));
    }

    @Operation(summary = "所有上架SKU（商城用）")
    @GetMapping("/sku/active")
    public R<java.util.List<ProductSku>> activeSkus() {
        LambdaQueryWrapper<ProductSku> w = new LambdaQueryWrapper<>();
        w.eq(ProductSku::getStatus, 1).orderByDesc(ProductSku::getCreateTime);
        return R.ok(skuMapper.selectList(w));
    }
}
