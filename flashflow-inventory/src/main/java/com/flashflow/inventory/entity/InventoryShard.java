package com.flashflow.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存分片表
 */
@Data
@TableName("inventory_shard")
public class InventoryShard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    /** 分片索引（0 ~ shard_count-1） */
    private Integer shardIndex;

    /** 分片库存 */
    private Integer shardStock;

    /** 已冻结（预扣）数量 */
    private Integer frozenStock;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
