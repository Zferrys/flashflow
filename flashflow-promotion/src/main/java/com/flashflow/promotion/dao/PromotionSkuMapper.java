package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.PromotionSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 活动商品 Mapper
 */
public interface PromotionSkuMapper extends BaseMapper<PromotionSku> {

    @Select("SELECT * FROM promotion_sku WHERE activity_id = #{activityId} ORDER BY sort ASC")
    List<PromotionSku> selectByActivityId(Long activityId);

    /** 原子递增已售数量 */
    @Update("UPDATE promotion_sku SET sold_count = sold_count + #{delta} WHERE id = #{id}")
    int incrementSoldCount(@Param("id") Long id, @Param("delta") int delta);
}
