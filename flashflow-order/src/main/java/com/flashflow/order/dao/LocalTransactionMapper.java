package com.flashflow.order.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.order.entity.LocalTransaction;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 本地事务表 Mapper（Saga 可靠性保证）
 */
public interface LocalTransactionMapper extends BaseMapper<LocalTransaction> {

    @Select("SELECT * FROM local_transaction WHERE status = 0 AND retry_count < max_retry ORDER BY create_time ASC LIMIT 100")
    List<LocalTransaction> selectPendingRetry();
}
