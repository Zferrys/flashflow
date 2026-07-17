package com.flashflow.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.promotion.dao.ProductSkuMapper;
import com.flashflow.promotion.dao.ProductSpuMapper;
import com.flashflow.promotion.entity.ProductSku;
import com.flashflow.promotion.entity.ProductSpu;
import com.flashflow.promotion.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;

    @Override
    public IPage<ProductSpu> spuPage(Page<ProductSpu> page, String keyword) {
        LambdaQueryWrapper<ProductSpu> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) w.like(ProductSpu::getSpuName, keyword);
        w.orderByDesc(ProductSpu::getCreateTime);
        return spuMapper.selectPage(page, w);
    }

    @Override
    public ProductSpu spuDetail(Long id) {
        return spuMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSpu(ProductSpu spu) {
        spuMapper.insert(spu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpu(ProductSpu spu) {
        spuMapper.updateById(spu);
    }

    @Override
    public List<ProductSku> skuList(Long spuId) {
        return skuMapper.selectBySpuId(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSku(ProductSku sku) {
        skuMapper.insert(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSku(ProductSku sku) {
        skuMapper.updateById(sku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long id) {
        skuMapper.deleteById(id);
    }

    @Override
    public List<ProductSpu> activeSpus() {
        LambdaQueryWrapper<ProductSpu> w = new LambdaQueryWrapper<>();
        w.eq(ProductSpu::getStatus, 1).orderByDesc(ProductSpu::getCreateTime);
        return spuMapper.selectList(w);
    }

    @Override
    public List<ProductSku> activeSkus() {
        LambdaQueryWrapper<ProductSku> w = new LambdaQueryWrapper<>();
        w.eq(ProductSku::getStatus, 1).orderByDesc(ProductSku::getCreateTime);
        return skuMapper.selectList(w);
    }
}
