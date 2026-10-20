<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="任务名称">
            <el-input v-model="query.name" placeholder="请输入任务名称" clearable @keyup.enter="load" @clear="load" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="name" label="任务名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="assignee" label="办理人" width="110">
          <template #default="{ row }">{{ row.assigneeName || row.assignee || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="完成时间" width="170" />
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">{{ formatDuration(row.durationInMillis) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="已办任务详情" width="900px" top="6vh">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务名称">{{ detail.task.name }}</el-descriptions-item>
          <el-descriptions-item label="流程名称">{{ detail.task.processDefinitionName }}</el-descriptions-item>
          <el-descriptions-item label="办理人">{{ detail.task.assigneeName || detail.task.assignee }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ detail.task.endTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">流程进度</div>
        <div class="chart-box">
          <ProcessViewer v-if="detailVisible && detailXml" :xml="detailXml" :active-ids="detail.activeActivityIds" />
        </div>

        <div class="section-title">审批记录</div>
        <el-timeline v-if="detail.comments.length">
          <el-timeline-item
            v-for="c in detail.comments"
            :key="c.id"
            :timestamp="c.time"
            placement="top"
          >
            <b>{{ c.userName || c.userId }}</b>: {{ c.message }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审批记录" :image-size="60" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import ProcessViewer from '@/components/ProcessViewer.vue'
import { getTaskDetail, pageDone } from '@/api/task'
import { getProcessXml } from '@/api/process'
import type { TaskDetail, TaskInfo } from '@/types'

const loading = ref(false)
const list = ref<TaskInfo[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, name: '' })

const detailVisible = ref(false)
const detail = ref<TaskDetail | null>(null)
const detailXml = ref('')

async function load() {
  loading.value = true
  try {
    const res = await pageDone({ page: query.page, size: query.size, name: query.name })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.name = ''
  query.page = 1
  load()
}

async function openDetail(row: TaskInfo) {
  // 已办任务使用历史数据, 通过任务ID查询详情接口不可用, 这里展示流程实例信息
  const res = await getTaskDetail(row.id)
  detail.value = res.data
  detailXml.value = (await getProcessXml(res.data.task.processDefinitionId)).data
  detailVisible.value = true
}

function formatDuration(millis?: number) {
  if (!millis && millis !== 0) return '-'
  const seconds = Math.floor(millis / 1000)
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分${seconds % 60}秒`
  const hours = Math.floor(minutes / 60)
  return `${hours}时${minutes % 60}分`
}

onMounted(load)
</script>

<style scoped lang="scss">
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  height: 320px;
  border: 1px solid #ebeef5;
}
</style>
