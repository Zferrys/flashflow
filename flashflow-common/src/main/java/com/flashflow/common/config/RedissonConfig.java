package com.flashflow.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 通用配置（单机 / 哨兵 二选一）
 *
 * 单机模式：spring.data.redis.host + port
 * 哨兵模式：spring.redis.sentinel.nodes + master（优先）
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /** 哨兵模式 master 名称（配置后自动启用哨兵模式） */
    @Value("${spring.redis.sentinel.master:}")
    private String sentinelMaster;

    /** 哨兵节点列表，逗号分隔，如 "127.0.0.1:26379,127.0.0.1:26380" */
    @Value("${spring.redis.sentinel.nodes:}")
    private String sentinelNodes;

    /** 连接池配置 */
    private static final int CONN_POOL_SIZE = 64;
    private static final int CONN_MIN_IDLE = 16;
    private static final int CONN_TIMEOUT = 5000;
    private static final int IDLE_TIMEOUT = 10000;
    private static final int RETRY_ATTEMPTS = 3;
    private static final int RETRY_INTERVAL = 1500;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        if (!sentinelMaster.isEmpty() && !sentinelNodes.isEmpty()) {
            // ── 哨兵模式 ──
            String[] nodes = sentinelNodes.split(",");
            config.useSentinelServers()
                    .setMasterName(sentinelMaster)
                    .addSentinelAddress(toAddresses(nodes))
                    .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                    .setDatabase(0)
                    .setMasterConnectionPoolSize(CONN_POOL_SIZE)
                    .setSlaveConnectionPoolSize(CONN_POOL_SIZE)
                    .setMasterConnectionMinimumIdleSize(CONN_MIN_IDLE)
                    .setSlaveConnectionMinimumIdleSize(CONN_MIN_IDLE)
                    .setConnectTimeout(CONN_TIMEOUT)
                    .setIdleConnectionTimeout(IDLE_TIMEOUT)
                    .setRetryAttempts(RETRY_ATTEMPTS)
                    .setRetryInterval(RETRY_INTERVAL);
        } else {
            // ── 单机模式 ──
            String address = "redis://" + redisHost + ":" + redisPort;
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                    .setConnectionPoolSize(CONN_POOL_SIZE)
                    .setConnectionMinimumIdleSize(CONN_MIN_IDLE)
                    .setIdleConnectionTimeout(IDLE_TIMEOUT)
                    .setConnectTimeout(CONN_TIMEOUT)
                    .setTimeout(3000)
                    .setRetryAttempts(RETRY_ATTEMPTS)
                    .setRetryInterval(RETRY_INTERVAL);
        }
        return Redisson.create(config);
    }

    private static String[] toAddresses(String[] hostPorts) {
        String[] result = new String[hostPorts.length];
        for (int i = 0; i < hostPorts.length; i++) {
            result[i] = "redis://" + hostPorts[i].trim();
        }
        return result;
    }
}
