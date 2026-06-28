<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed z-[100] glass-popover p-5 w-80"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @click.stop
    >
      <h3 class="text-lg font-bold text-[#F59E0B] mb-2">{{ title }}</h3>
      <p class="text-sm text-gray-600 leading-relaxed">{{ content }}</p>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '' },
  content: { type: String, default: '' },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 }),
  },
})
</script>
