package com.flashflow.promotion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flashflow.promotion.entity.ProductSku;
import com.flashflow.promotion.entity.ProductSpu;

import java.util.List;

/**
 * 商品管理服务接口
 */
public interface ProductService {

    /** SPU 分页 */
    IPage<ProductSpu> spuPage(Page<ProductSpu> page, String keyword);

    /** SPU 详情 */
    ProductSpu spuDetail(Long id);

    /** 创建 SPU */
    void createSpu(ProductSpu spu);

    /** 修改 SPU */
    void updateSpu(ProductSpu spu);

    /** SKU 列表 */
    List<ProductSku> skuList(Long spuId);

    /** 创建 SKU */
    void createSku(ProductSku sku);

    /** 修改 SKU */
    void updateSku(ProductSku sku);

    /** 删除 SKU */
    void deleteSku(Long id);

    /** 所有上架 SPU */
    List<ProductSpu> activeSpus();

    /** 所有上架 SKU */
    List<ProductSku> activeSkus();
}
