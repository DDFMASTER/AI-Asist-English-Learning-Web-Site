<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed z-[60] glass-popover p-5 w-80 max-h-[calc(100vh-160px)] overflow-y-auto"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @click.stop
    >
      <!-- 页眉 -->
      <div class="flex justify-between items-start mb-3">
        <div class="flex items-center gap-2">
          <Icon icon="ph:translate-bold" class="text-lg text-[#2563EB]" />
          <h3 class="text-sm font-bold text-gray-700">段落翻译</h3>
        </div>
        <button
          class="text-gray-400 hover:text-gray-600 transition-colors flex-none"
          @click="$emit('close')"
        >
          <Icon icon="ph:x-bold" class="text-lg" />
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="translationData?.loading" class="flex items-center justify-center py-8">
        <Icon icon="ph:spinner-bold" class="text-lg text-[#2563EB] animate-spin" />
        <span class="text-xs text-gray-400 ml-2">AI 正在翻译...</span>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="translationData?.error" class="text-center py-6">
        <Icon icon="ph:warning-circle-bold" class="text-2xl text-amber-400 mx-auto mb-2" />
        <p class="text-xs text-gray-400">{{ translationData.error }}</p>
      </div>

      <!-- 翻译结果 -->
      <div v-else-if="translationData?.zh" class="space-y-3">
        <!-- 原文（折叠显示） -->
        <div v-if="originalText">
          <p class="text-[10px] font-bold text-gray-400 mb-1">原文</p>
          <p class="text-xs text-gray-500 leading-relaxed" :class="{ 'line-clamp-3': !showFullOriginal }">
            {{ originalText }}
          </p>
          <button
            v-if="originalText.length > 150"
            class="text-[10px] text-[#2563EB] hover:text-blue-700 mt-0.5"
            @click="showFullOriginal = !showFullOriginal"
          >
            {{ showFullOriginal ? '收起' : '展开全文' }}
          </button>
        </div>

        <!-- 译文 -->
        <div class="p-3 bg-blue-50/50 rounded-xl">
          <p class="text-xs text-gray-400 mb-1">译文</p>
          <p class="text-sm text-gray-800 leading-relaxed">{{ translationData.zh }}</p>
        </div>

        <p class="text-[9px] text-gray-300 text-center pt-1">
          内容由 AI 生成，仅供参考
        </p>
      </div>

      <!-- 空状态（没有翻译也没有加载中） -->
      <div v-else class="text-center py-6">
        <p class="text-xs text-gray-400">暂无翻译数据</p>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Icon } from '@iconify/vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  originalText: { type: String, default: '' },
  translationData: {
    type: Object,
    default: () => null,
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 }),
  },
})

defineEmits(['close'])

const showFullOriginal = ref(false)

// 当翻译数据变化（切换段落）时重置展开状态
watch(() => props.translationData, () => {
  showFullOriginal.value = false
})
</script>
