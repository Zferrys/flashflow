package com.flashflow.promotion.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.flashflow.promotion.entity.PromotionActivity;
import com.flashflow.promotion.entity.PromotionSku;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Caffeine）
 *
 * 作用：Redis 击穿保护——Redis 不可用时，秒杀活动/SKU 信息从本地缓存读取，
 * 不回源 DB，防止 DB 被打崩。
 *
 * 只缓存读多写少的数据：活动信息、SKU 信息。
 * 库存数据（实时变动）不缓存。
 */
@Configuration
public class LocalCacheConfig {

    /** 活动信息本地缓存：最大 1000 个，写入后 5 分钟过期 */
    @Bean
    public Cache<Long, PromotionActivity> activityLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /** SKU 信息本地缓存：最大 5000 个，写入后 5 分钟过期 */
    @Bean
    public Cache<String, PromotionSku> skuLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
