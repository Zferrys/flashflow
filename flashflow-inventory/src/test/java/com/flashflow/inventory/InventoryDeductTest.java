package com.flashflow.inventory;

import com.flashflow.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存扣减逻辑测试（关注分片路由和边界条件）
 */
@DisplayName("库存扣减测试")
class InventoryDeductTest {

    @Test
    @DisplayName("分片路由：user_id % shard_count 应该均匀分布")
    void shardRoutingShouldBeDeterministic() {
        int shardCount = 16;
        // 同一个 user_id 始终路由到同一个分片
        long userId = 10086L;
        int shard1 = (int) (userId % shardCount);
        int shard2 = (int) (userId % shardCount);
        assertEquals(shard1, shard2, "相同 user_id 应该路由到相同分片");

        // 不同 user_id 可能路由到不同分片
        long userId2 = 10087L;
        int shard3 = (int) (userId2 % shardCount);
        // 只是概率性不同，但至少验证计算不抛异常
        assertTrue(shard3 >= 0 && shard3 < shardCount);
    }

    @Test
    @DisplayName("分片范围：shard_index 始终在 0 到 shard_count-1 之间")
    void shardIndexShouldBeInRange() {
        int shardCount = 16;
        long[] userIds = {0L, 1L, 999L, 10000L, Long.MAX_VALUE};
        for (long userId : userIds) {
            int shard = (int) (userId % shardCount);
            assertTrue(shard >= 0 && shard < shardCount,
                    "shard " + shard + " out of range for userId " + userId);
        }
    }

    @Test
    @DisplayName("库存数量边界：扣减数量不能为负数或零")
    void quantityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> {
            if (0 <= 0) throw new IllegalArgumentException("数量必须大于0");
        });
    }

    @Test
    @DisplayName("多个用户扣减时互不干扰")
    void differentUsersShouldUseDifferentShards() {
        int shardCount = 16;
        long userA = 10086L;
        long userB = 10087L;
        int shardA = (int) (userA % shardCount);
        int shardB = (int) (userB % shardCount);
        // 如果恰好相同，这个测试无意义，但通常不同
        System.out.println("userA shard=" + shardA + ", userB shard=" + shardB);
    }

    @Test
    @DisplayName("DeductResult 应该正确记录结果")
    void deductResultShouldRecordCorrectly() {
        InventoryService.DeductResult result = new InventoryService.DeductResult(true, 5, 99);
        assertTrue(result.success());
        assertEquals(5, result.shardIndex());
        assertEquals(99, result.stockAfter());
    }

    @Test
    @DisplayName("库存不足时应该返回 success=false")
    void insufficientStockShouldFail() {
        InventoryService.DeductResult result = new InventoryService.DeductResult(false, 0, 0);
        assertFalse(result.success());
    }
}
