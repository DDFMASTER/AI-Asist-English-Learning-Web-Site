<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed z-[100] glass-popover p-5 w-80 max-h-[60vh] overflow-y-auto"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @click.stop
    >
      <!-- 页眉：单词 + 音标 + 收藏按钮 -->
      <div class="flex justify-between items-start mb-3">
        <div>
          <h3 class="text-xl font-bold text-[#2563EB]">{{ word.word }}</h3>
          <p v-if="word.phonetic" class="text-xs text-gray-400">{{ word.phonetic }}</p>
          <!-- 词形还原标注 -->
          <p v-if="word.lemmaFrom && word.lemmaTo" class="text-xs text-amber-500 mt-0.5">
            <Icon icon="ph:arrows-split-bold" class="inline text-[10px] mr-0.5" />
            原形: {{ word.lemmaTo }}
          </p>
        </div>
        <button
          class="text-gray-400 hover:text-[#2563EB] transition-colors flex-none"
          @click="handleAddToVocab"
          title="加入生词本"
        >
          <Icon icon="ph:plus-circle-bold" class="text-2xl" />
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="flex items-center justify-center py-6">
        <Icon icon="ph:spinner-bold" class="text-lg text-[#2563EB] animate-spin" />
        <span class="text-xs text-gray-400 ml-2">查询中...</span>
      </div>

      <!-- 查询出错 -->
      <div v-else-if="word.error" class="text-center py-4">
        <p class="text-sm text-red-400">{{ word.error }}</p>
      </div>

      <!-- 未找到结果 -->
      <div v-else-if="word.found === false" class="text-center py-4">
        <p class="text-sm text-gray-400">未在当前词库中找到该单词</p>
      </div>

      <!-- 按来源分组的结果 -->
      <div v-else-if="word.results && word.results.length" class="space-y-3 mb-4">
        <div v-for="(group, gIdx) in word.results" :key="gIdx">
          <div class="flex items-center gap-1.5 mb-1">
            <span
              class="text-[10px] font-bold px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded"
            >
              {{ group.source }}
            </span>
          </div>
          <div
            v-for="(entry, eIdx) in group.entries"
            :key="eIdx"
            class="flex items-baseline gap-2 ml-1 mb-0.5"
          >
            <span v-if="entry.phonetic" class="text-[11px] text-gray-400 font-mono flex-none">
              {{ entry.phonetic }}
            </span>
            <span class="text-sm text-gray-700 leading-relaxed">
              {{ entry.translation }}
            </span>
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="pt-3 border-t border-gray-100">
        <button
          class="w-full py-2 bg-gray-50 hover:bg-gray-100 text-[11px] font-bold text-gray-500 rounded-lg transition-colors"
          @click="handleViewDetail"
        >
          查看详细解析与例句
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { Icon } from '@iconify/vue'
import { useReaderStore } from '@/stores/reader'

const props = defineProps({
  visible: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  word: {
    type: Object,
    default: () => ({ word: '', results: [] }),
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 }),
  },
})

const emit = defineEmits(['close', 'add-vocab', 'view-detail'])

const readerStore = useReaderStore()

async function handleAddToVocab() {
  const success = await readerStore.addToVocabulary(props.word.word)
  if (success) {
    emit('add-vocab', props.word)
  }
}

function handleViewDetail() {
  emit('view-detail', props.word)
}
</script>
