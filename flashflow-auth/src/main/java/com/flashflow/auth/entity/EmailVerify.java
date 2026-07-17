package com.flashflow.auth.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("email_verify")
public class EmailVerify {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String code;
    private Integer type;
    private Integer status;
    private LocalDateTime expireTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
