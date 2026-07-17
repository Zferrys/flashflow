<template>
  <div v-if="error" style="min-height:200px;display:flex;align-items:center;justify-content:center;padding:40px 20px">
    <div style="text-align:center;max-width:420px">
      <el-result :icon="icon" :title="title" :sub-title="errorMsg || message">
        <template #extra>
          <el-button type="primary" @click="handleRetry">重试</el-button>
          <el-button @click="handleBack">返回</el-button>
        </template>
      </el-result>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

withDefaults(defineProps<{
  icon?: 'error' | 'warning' | 'info'
  title?: string
  message?: string
}>(), {
  icon: 'error',
  title: '加载失败',
  message: '页面数据加载异常，请稍后重试',
})

const emit = defineEmits<{
  retry: []
}>()

const router = useRouter()
const error = ref(false)
const errorMsg = ref('')

function setError(msg?: string) {
  error.value = true
  if (msg) errorMsg.value = msg
}
function handleRetry() { error.value = false; emit('retry') }
function handleBack() { router.back() }
defineExpose({ setError })
</script>
