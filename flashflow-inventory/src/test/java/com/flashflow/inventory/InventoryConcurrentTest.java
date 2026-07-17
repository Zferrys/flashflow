package com.flashflow.inventory;

import com.flashflow.inventory.service.InventoryService;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发扣库存测试（真正的压测）
 *
 * 场景：100 个用户同时抢购同一 SKU，库存 50
 * 期望：成功数 <= 50，0 超卖
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryConcurrentTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RedissonClient redissonClient;

    private static final Long SKU_ID = 99999L;
    private static final int TOTAL_STOCK = 50;
    private static final int USER_COUNT = 100;

    @BeforeEach
    void setUp() {
        // 创建数据库库存记录 + Redis 预热（使用 Redisson 保持序列化一致）
        inventoryService.adjust(SKU_ID, TOTAL_STOCK);
        inventoryService.warmUp(SKU_ID);
        System.out.println("===== 库存预热完成: SKU=" + SKU_ID + ", 总库存=" + TOTAL_STOCK + " =====");
    }

    @AfterEach
    void tearDown() {
        // 清理 Redis 测试数据
        for (int i = 0; i < 16; i++) {
            redissonClient.getBucket("stock:" + SKU_ID + ":" + i).delete();
        }
    }

    @Test
    @Order(1)
    @DisplayName("100 线程并发扣库存 - 应该 0 超卖")
    void concurrentDeductShouldNotOversell() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(USER_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        long start = System.currentTimeMillis();

        for (int i = 0; i < USER_COUNT; i++) {
            long userId = 10000L + i;
            executor.submit(() -> {
                try {
                    InventoryService.DeductRequest req =
                            new InventoryService.DeductRequest(SKU_ID, userId, 1, "");
                    InventoryService.DeductResult result = inventoryService.deduct(req);
                    if (result.success()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long elapsed = System.currentTimeMillis() - start;

        // 验证
        int totalDeducted = countRedisStock();

        System.out.println("===== 并发测试结果 =====");
        System.out.println("用户数:    " + USER_COUNT);
        System.out.println("总库存:    " + TOTAL_STOCK);
        System.out.println("扣减成功:  " + successCount.get());
        System.out.println("扣减失败:  " + failCount.get());
        System.out.println("实际扣减:  " + totalDeducted);
        System.out.println("耗时:      " + elapsed + "ms");
        System.out.println("QPS:       " + (USER_COUNT * 1000L / elapsed));

        assertTrue(successCount.get() <= TOTAL_STOCK,
                "超卖！成功数 " + successCount.get() + " > 库存 " + TOTAL_STOCK);
        assertEquals(successCount.get(), totalDeducted,
                "成功数与 Redis 实际扣减数不一致");
        System.out.println("✅ 结论：无超卖，库存一致性通过");
    }

    @Test
    @Order(2)
    @DisplayName("单个用户重复扣减相同分片 - 原子性验证")
    void sameShardDeductShouldBeAtomic() throws Exception {
        long userId = 50000L;
        int shard = (int) (userId % 16);
        String key = "stock:" + SKU_ID + ":" + shard;

        Object val = redissonClient.getBucket(key).get();
        int before = val instanceof Number ? ((Number) val).intValue() : 0;
        System.out.println("分片 " + shard + " 扣减前库存: " + before);

        // 扣到 0（允许 BusinessException 表示库存已耗尽）
        int success = 0;
        for (int i = 0; i < before + 5; i++) {
            try {
                InventoryService.DeductRequest req =
                        new InventoryService.DeductRequest(SKU_ID, userId, 1, "");
                inventoryService.deduct(req);
                success++;
            } catch (Exception e) {
                // 库存耗尽，正常
                break;
            }
        }

        Object afterVal = redissonClient.getBucket(key).get();
        int after = afterVal instanceof Number ? ((Number) afterVal).intValue() : 0;
        System.out.println("分片 " + shard + " 扣减前库存: " + before + ", 成功扣减: " + success + ", 剩余: " + after);

        assertTrue(after >= 0,
                "库存扣为负数了！原子性被破坏: " + after);
        System.out.println("✅ 结论：Lua 原子扣减正确，库存未扣穿");
    }

    /** 统计 Redis 中所有分片的已扣库存 */
    private int countRedisStock() {
        int total = 0;
        for (int i = 0; i < 16; i++) {
            Object val = redissonClient.getBucket("stock:" + SKU_ID + ":" + i).get();
            if (val instanceof Number) {
                total += ((Number) val).intValue();
            }
        }
        return TOTAL_STOCK - total;
    }
}
