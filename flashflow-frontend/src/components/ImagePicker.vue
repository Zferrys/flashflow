<template>
  <div>
    <!-- 当前选中的图片预览 -->
    <div v-if="modelValue" style="margin-bottom: 8px">
      <el-image
        :src="modelValue"
        style="width: 120px; height: 120px; border-radius: 6px; border: 2px solid #409eff"
        fit="cover"
      >
        <template #error>
          <div style="width:120px;height:120px;background:#f5f7fa;display:flex;align-items:center;justify-content:center;color:#ccc;border-radius:6px">加载失败</div>
        </template>
      </el-image>
      <div style="margin-top:4px;font-size:12px;color:#999">{{ modelValue.split('/').pop() }}</div>
    </div>

    <!-- 图片列表 -->
    <el-dialog v-model="visible" title="选择图片" width="640px">
      <div v-if="loading" style="text-align:center;padding:40px">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p style="color:#999;margin-top:8px">加载图片列表...</p>
      </div>
      <div v-else style="display:flex;flex-wrap:wrap;gap:12px">
        <div
          v-for="img in images"
          :key="img"
          class="img-item"
          :class="{ selected: modelValue === img }"
          @click="select(img)"
          style="
            width: calc(20% - 10px);
            border: 2px solid #eee;
            border-radius: 8px;
            padding: 6px;
            cursor: pointer;
            transition: all 0.2s;
            text-align: center;
          "
          @mouseenter="($event.target as HTMLElement).style.borderColor = '#409eff'"
          @mouseleave="($event.target as HTMLElement).style.borderColor = modelValue === img ? '#409eff' : '#eee'"
        >
          <el-image
            :src="img"
            style="width:100%;height:100px;border-radius:4px"
            fit="cover"
          >
            <template #error>
              <div style="width:100%;height:100px;background:#f5f7fa;display:flex;align-items:center;justify-content:center;color:#ccc;font-size:12px">暂无</div>
            </template>
          </el-image>
          <div style="font-size:11px;color:#666;margin-top:4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
            {{ img.split('/').pop() }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 触发按钮 -->
    <el-button size="small" @click="open">
      <el-icon><Picture /></el-icon>
      选择图片
    </el-button>
    <el-button v-if="modelValue" size="small" text type="danger" @click="$emit('update:modelValue', '')">
      清除
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Picture, Loading } from '@element-plus/icons-vue'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const visible = ref(false)
const loading = ref(false)
const selected = ref('')

const images = computed(() => PRODUCT_IMAGES)

// 产品图片库（覆盖 7 个品类，使用命名产品图片而非随机 sku-N 占位图）
const PRODUCT_IMAGES = [
  // 手机
  '/assets/products/iphone15-pro.jpg',
  '/assets/products/iphone15.jpg',
  '/assets/products/xiaomi14.jpg',
  '/assets/products/huawei-mate60.jpg',
  // 笔记本
  '/assets/products/macbook-pro.jpg',
  '/assets/products/macbook-air.jpg',
  '/assets/products/xps15.jpg',
  // 音频
  '/assets/products/airpods-pro.jpg',
  '/assets/products/sony-headphones.jpg',
  // 穿戴
  '/assets/products/apple-watch.jpg',
  // 平板
  '/assets/products/ipad-air.jpg',
  '/assets/products/matepad.jpg',
  // 游戏
  '/assets/products/nintendo-switch.jpg',
  // 配件（通用占位图）
  '/assets/products/default.jpg',
]

function open() {
  selected.value = props.modelValue
  visible.value = true
}

function select(img: string) {
  selected.value = img
}

function confirm() {
  if (selected.value) {
    emit('update:modelValue', selected.value)
  }
  visible.value = false
}
</script>

<style scoped>
.img-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}
.img-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
