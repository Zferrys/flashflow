package com.flashflow.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地事务表（Saga 可靠性保证）
 */
@Data
@TableName("local_transaction")
public class LocalTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private String businessType;

    private String businessKey;

    /** 0INIT 1DONE 2FAIL 3COMPENSATED */
    private Integer status;

    /** 消息体(JSON) */
    private String payload;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime lastRetry;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
