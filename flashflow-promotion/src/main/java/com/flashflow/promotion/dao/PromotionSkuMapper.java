package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.PromotionSku;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动商品 Mapper
 */
public interface PromotionSkuMapper extends BaseMapper<PromotionSku> {

    @Select("SELECT * FROM promotion_sku WHERE activity_id = #{activityId} ORDER BY sort ASC")
    List<PromotionSku> selectByActivityId(Long activityId);
}
