package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.PromotionRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 参与记录 Mapper
 */
public interface PromotionRecordMapper extends BaseMapper<PromotionRecord> {

    @Select("SELECT COUNT(*) FROM promotion_record WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int countByUserAndActivity(@Param("userId") Long userId, @Param("activityId") Long activityId);

    @Select("SELECT COALESCE(SUM(quantity), 0) FROM promotion_record WHERE user_id = #{userId} AND activity_id = #{activityId} AND sku_id = #{skuId}")
    int sumQuantityByUserAndSku(@Param("userId") Long userId, @Param("activityId") Long activityId, @Param("skuId") Long skuId);
}
