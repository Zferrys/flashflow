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
    @jakarta.validation.constraints.NotBlank(message = "活动类型不能为空")
    private String activityType;

    @jakarta.validation.constraints.NotBlank(message = "活动名称不能为空")
    private String name;

    @jakarta.validation.constraints.NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @jakarta.validation.constraints.NotNull(message = "结束时间不能为空")
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
