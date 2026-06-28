<template>
  <div
    :class="['toggle-switch', { active: modelValue }]"
    @click="toggle"
  ></div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

function toggle() {
  const newVal = !props.modelValue
  emit('update:modelValue', newVal)
  emit('change', newVal)
}
</script>
