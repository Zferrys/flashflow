package com.flashflow.promotion.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销活动
 */
@Data
@TableName("promotion_activity")
public class PromotionActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动类型 FLASH_SALE / PRE_SALE / GROUP_BUY */
    private String activityType;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态 0草稿 1待预热 2进行中 3已结束 4已关闭 */
    private Integer status;

    private String remark;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
