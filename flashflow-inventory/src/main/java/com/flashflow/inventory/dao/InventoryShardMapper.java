package com.flashflow.inventory.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.inventory.entity.InventoryShard;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存分片 Mapper
 */
public interface InventoryShardMapper extends BaseMapper<InventoryShard> {

    @Select("SELECT * FROM inventory_shard WHERE sku_id = #{skuId} ORDER BY shard_index ASC")
    List<InventoryShard> selectBySkuId(@Param("skuId") Long skuId);
}
