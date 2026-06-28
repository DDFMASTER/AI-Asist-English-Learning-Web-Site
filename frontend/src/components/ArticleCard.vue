<template>
  <div
    class="card flex gap-6 hover:shadow-lg transition-shadow cursor-pointer group"
    @click="goToReader"
  >
    <!-- 左侧图标 -->
    <div
      class="w-48 h-32 rounded-xl overflow-hidden flex-none relative flex items-center justify-center"
      :class="bgClass"
    >
      <Icon :icon="icon" class="text-4xl opacity-40" :class="iconColorClass" />
    </div>

    <!-- 中间内容 -->
    <div class="flex-1 min-w-0">
      <div class="flex items-center gap-2 mb-2">
        <span :class="difficultyBadgeClass">{{ article.difficulty || 'B1+ 适中' }}</span>
        <span class="text-[11px] text-gray-400 font-medium">{{ article.source }}</span>
      </div>
      <h3 class="text-xl font-bold mb-2 group-hover:text-[#2563EB] transition-colors truncate">
        {{ article.title }}
      </h3>
      <p class="text-gray-500 text-sm line-clamp-2 leading-relaxed mb-4">
        {{ article.abstract || article.description || '暂无简介' }}
      </p>
      <div class="flex items-center gap-6 text-[11px] text-gray-400 font-medium">
        <span class="flex items-center gap-1">
          <Icon icon="ph:clock-bold" /> {{ article.readTime || '10 min read' }}
        </span>
        <span class="flex items-center gap-1">
          <Icon icon="ph:text-t-bold" /> {{ article.wordCount || '850 words' }}
        </span>
        <span class="flex items-center gap-1">
          <Icon icon="ph:translate-bold" /> {{ article.newWords || '12 New Words' }}
        </span>
      </div>
    </div>

    <!-- 右侧箭头 -->
    <div class="flex flex-col justify-center">
      <div
        class="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center text-[#2563EB] hover:bg-[#2563EB] hover:text-white transition-all"
        @click.stop="goToReader"
      >
        <Icon icon="ph:arrow-right-bold" class="text-xl" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'

const props = defineProps({
  article: {
    type: Object,
    required: true,
  },
})

const router = useRouter()

const difficultyMap = {
  'A1': 'badge badge-easy',
  'A2': 'badge badge-easy',
  'A2+': 'badge badge-easy',
  'B1': 'badge badge-medium',
  'B1+': 'badge badge-medium',
  'B2': 'badge badge-hard',
  'C1': 'badge badge-hard',
}

const bgMap = {
  'A1': 'bg-gradient-to-br from-green-100 to-teal-100',
  'A2': 'bg-gradient-to-br from-green-100 to-teal-100',
  'A2+': 'bg-gradient-to-br from-green-100 to-teal-100',
  'B1': 'bg-gradient-to-br from-blue-100 to-purple-100',
  'B1+': 'bg-gradient-to-br from-blue-100 to-purple-100',
  'B2': 'bg-gradient-to-br from-yellow-100 to-orange-100',
  'C1': 'bg-gradient-to-br from-red-100 to-pink-100',
}

const iconMap = {
  'A1': 'ph:tree-bold',
  'A2': 'ph:flask-bold',
  'A2+': 'ph:flask-bold',
  'B1': 'ph:newspaper-bold',
  'B1+': 'ph:newspaper-bold',
  'B2': 'ph:building-bold',
  'C1': 'ph:brain-bold',
}

const iconColorMap = {
  'A1': 'text-green-500',
  'A2': 'text-green-500',
  'A2+': 'text-green-500',
  'B1': 'text-[#2563EB]',
  'B1+': 'text-[#2563EB]',
  'B2': 'text-yellow-500',
  'C1': 'text-red-500',
}

const difficultyBadgeClass = computed(() =>
  difficultyMap[props.article.difficulty] || 'badge badge-medium'
)

const bgClass = computed(() =>
  bgMap[props.article.difficulty] || 'bg-gradient-to-br from-blue-100 to-purple-100'
)

const icon = computed(() =>
  iconMap[props.article.difficulty] || 'ph:newspaper-bold'
)

const iconColorClass = computed(() =>
  iconColorMap[props.article.difficulty] || 'text-[#2563EB]'
)

function goToReader() {
  router.push(`/reader?id=${props.article.id || props.article.article_id}`)
}
</script>
