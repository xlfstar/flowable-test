<template>
  <div ref="canvasRef" class="bpmn-viewer"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

const props = defineProps<{
  xml: string
  activeIds?: string[]
  completedIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'element-click', payload: { id: string; type: string; name: string }): void
}>()

const canvasRef = ref<HTMLDivElement>()
let viewer: any = null

async function render() {
  if (!canvasRef.value || !props.xml) return
  if (!viewer) {
    viewer = new NavigatedViewer({ container: canvasRef.value })
    const eventBus: any = viewer.get('eventBus')
    eventBus.on('element.click', (event: any) => {
      const element = event.element
      if (!element?.id) return
      emit('element-click', {
        id: element.id,
        type: element.type,
        name: element.businessObject?.name || ''
      })
    })
  }
  try {
    await viewer.importXML(props.xml)
    const canvas = viewer.get('canvas')
    canvas.zoom('fit-viewport', 'auto')
    applyHighlight()
  } catch (e) {
    console.error('渲染流程图失败', e)
  }
}

function applyHighlight() {
  if (!viewer) return
  const canvas = viewer.get('canvas')
  const elementRegistry = viewer.get('elementRegistry')
  elementRegistry.getAll().forEach((el: any) => {
    canvas.removeMarker(el, 'highlight')
    canvas.removeMarker(el, 'completed')
  })
  const activeIds = new Set(props.activeIds || [])
  ;(props.completedIds || [])
    .filter((id) => !activeIds.has(id))
    .forEach((id) => {
      const element = elementRegistry.get(id)
      if (element) {
        canvas.addMarker(element, 'completed')
      }
    })
  activeIds.forEach((id) => {
    const element = elementRegistry.get(id)
    if (element) {
      canvas.addMarker(element, 'highlight')
    }
  })
}

watch(
  () => [props.xml, props.activeIds, props.completedIds],
  () => render(),
  { deep: true }
)

onMounted(render)

onBeforeUnmount(() => {
  viewer?.destroy()
  viewer = null
})
</script>

<style>
.bpmn-viewer {
  width: 100%;
  height: 100%;
  min-height: 360px;
  background-color: #fff;
  background-image: linear-gradient(90deg, #f5f5f5 1px, transparent 1px),
    linear-gradient(#f5f5f5 1px, transparent 1px);
  background-size: 20px 20px;
}

.bpmn-viewer .highlight:not(.djs-connection) .djs-visual > :nth-child(1) {
  stroke: #f56c6c !important;
  stroke-width: 3px !important;
  fill: #fef0f0 !important;
}

.bpmn-viewer .highlight.djs-connection .djs-visual > :nth-child(1) {
  stroke: #f56c6c !important;
  stroke-width: 3px !important;
}

.bpmn-viewer .completed:not(.djs-connection) .djs-visual > :nth-child(1) {
  stroke: #67c23a !important;
  fill: #f0f9eb !important;
}

.bpmn-viewer .completed.djs-connection .djs-visual > :nth-child(1) {
  stroke: #67c23a !important;
}

.bpmn-viewer .djs-element {
  cursor: pointer;
}
</style>
