<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.title" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card" @click="router.push(card.to)">
          <div class="stat-icon" :style="{ backgroundColor: card.color }">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome-card" shadow="never">
      <template #header>
        <span>欢迎, {{ userStore.user?.realName || userStore.user?.username }}</span>
      </template>
      <p>这是一个基于 <b>Spring Boot 3 + Flowable 7 + MySQL + Vue3 + Element Plus + bpmn-js</b> 的工作流示例项目。</p>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">{{ userStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleLabel }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ userStore.user?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ userStore.user?.positionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程引擎">Flowable 7</el-descriptions-item>
        <el-descriptions-item label="示例流程">请假审批流程</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { pageTodo, pageDone } from '@/api/task'
import { pageProcessDefinitions, pageProcessInstances } from '@/api/process'

const router = useRouter()
const userStore = useUserStore()

const todoCount = ref(0)
const doneCount = ref(0)
const definitionCount = ref(0)
const instanceCount = ref(0)

const roleLabels: Record<string, string> = {
  admin: '管理员',
  executive: '高管',
  manager: '部门经理',
  hr: '人事',
  employee: '员工',
  sales: '业务员',
  finance: '财务',
  consultant_manager: '顾问主管',
  consultant: '顾问',
  accounting_manager: '会计主管',
  accountant: '会计'
}
const roleLabel = computed(() => roleLabels[userStore.user?.role || ''] || userStore.user?.role)

const cards = computed(() => [
  { title: '我的待办', value: todoCount.value, icon: 'Bell', color: '#409eff', to: '/task/todo' },
  { title: '我的已办', value: doneCount.value, icon: 'CircleCheck', color: '#67c23a', to: '/task/done' },
  { title: '流程定义', value: definitionCount.value, icon: 'Files', color: '#e6a23c', to: '/process/definition' },
  { title: '运行中实例', value: instanceCount.value, icon: 'DataLine', color: '#f56c6c', to: '/process/instance' }
])

async function loadStats() {
  const [todo, done, defs, instances] = await Promise.allSettled([
    pageTodo({ page: 1, size: 1 }),
    pageDone({ page: 1, size: 1 }),
    pageProcessDefinitions({ page: 1, size: 1 }),
    pageProcessInstances({ page: 1, size: 1 })
  ])
  if (todo.status === 'fulfilled') todoCount.value = todo.value.data.total
  if (done.status === 'fulfilled') doneCount.value = done.value.data.total
  if (defs.status === 'fulfilled') definitionCount.value = defs.value.data.total
  if (instances.status === 'fulfilled') instanceCount.value = instances.value.data.total
}

onMounted(loadStats)
</script>

<style scoped lang="scss">
.stat-card {
  cursor: pointer;
  margin-bottom: 16px;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-title {
  color: #909399;
  font-size: 13px;
}

.welcome-card {
  margin-top: 8px;
}
</style>
