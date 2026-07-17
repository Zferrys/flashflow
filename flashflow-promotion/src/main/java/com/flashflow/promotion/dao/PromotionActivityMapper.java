package com.flashflow.promotion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.PromotionActivity;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动 Mapper
 */
public interface PromotionActivityMapper extends BaseMapper<PromotionActivity> {

    @Select("SELECT * FROM promotion_activity WHERE status = 2 AND start_time <= #{now} AND end_time > #{now}")
    List<PromotionActivity> selectActive(LocalDateTime now);

    @Select("SELECT * FROM promotion_activity WHERE status = 1 AND start_time <= #{now}")
    List<PromotionActivity> selectReadyToWarm(LocalDateTime now);
}
