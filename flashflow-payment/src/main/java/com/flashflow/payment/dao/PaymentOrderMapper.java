package com.flashflow.payment.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付订单 Mapper
 */
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM payment_order WHERE order_sn = #{orderSn}")
    PaymentOrder selectByOrderSn(@Param("orderSn") String orderSn);
}
