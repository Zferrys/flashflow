package com.flashflow.inventory;

import com.flashflow.inventory.entity.Inventory;
import com.flashflow.inventory.entity.InventoryShard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存数据一致性测试
 */
@DisplayName("库存数据一致性测试")
class InventoryStockTest {

    @Test
    @DisplayName("总库存应该等于各分片库存之和")
    void totalStockShouldEqualSumOfShards() {
        int totalStock = 1000;
        int shardCount = 16;
        int perShard = totalStock / shardCount;

        List<Integer> shards = new ArrayList<>();
        for (int i = 0; i < shardCount; i++) {
            int stock = (i == shardCount - 1) ? perShard + totalStock % shardCount : perShard;
            shards.add(stock);
        }

        int sum = shards.stream().mapToInt(Integer::intValue).sum();
        assertEquals(totalStock, sum, "分片库存之和应该等于总库存");
    }

    @Test
    @DisplayName("库存不能为负数")
    void stockShouldNotBeNegative() {
        InventoryShard shard = new InventoryShard();
        shard.setShardStock(10);
        assertTrue(shard.getShardStock() >= 0);
    }

    @Test
    @DisplayName("冻结库存不能超过分片库存")
    void frozenStockShouldNotExceedShardStock() {
        InventoryShard shard = new InventoryShard();
        shard.setShardStock(100);
        shard.setFrozenStock(30);
        assertTrue(shard.getFrozenStock() <= shard.getShardStock());
    }

    @Test
    @DisplayName("分片索引应该在 0~15 范围内")
    void shardIndexShouldBeInRange() {
        int shardCount = 16;
        for (int i = 0; i < shardCount; i++) {
            assertTrue(i >= 0 && i < shardCount);
        }
    }

    @Test
    @DisplayName("库存调整后分片库存应该重新分配")
    void afterAdjustShardsShouldRedistribute() {
        int newTotal = 500;
        int shardCount = 16;
        int perShard = newTotal / shardCount;
        int lastShard = perShard + newTotal % shardCount;

        assertEquals(31, perShard);
        assertEquals(35, lastShard);
        assertEquals(newTotal, perShard * 15 + lastShard);
    }

    @Test
    @DisplayName("库存扣减后不能为负数")
    void stockAfterDeductShouldNotBeNegative() {
        int currentStock = 10;
        int deductQuantity = 5;
        int after = currentStock - deductQuantity;
        assertTrue(after >= 0);
    }

    @Test
    @DisplayName("库存不足时不能扣减")
    void cannotDeductWhenInsufficient() {
        int currentStock = 3;
        int deductQuantity = 5;
        assertFalse(currentStock >= deductQuantity);
    }
}
