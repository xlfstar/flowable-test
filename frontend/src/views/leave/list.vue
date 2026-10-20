<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="load">
              <el-option label="审批中" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="router.push('/leave/apply')">发起请假</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="applyUserName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="120">
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="positionName" label="岗位" width="120">
          <template #default="{ row }">{{ row.positionName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="startDate" label="开始日期" width="130" />
        <el-table-column prop="endDate" label="结束日期" width="130" />
        <el-table-column prop="reason" label="事由" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentActivityName" label="当前节点" width="130">
          <template #default="{ row }">{{ row.currentActivityName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="currentAssigneeName" label="当前审批人" width="110">
          <template #default="{ row }">{{ row.currentAssigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="请假申请详情" width="900px" top="6vh">
      <div v-if="current">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="申请人">{{ current.applyUserName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ current.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ current.positionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请假天数">{{ current.days }} 天</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ current.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ current.endDate }}</el-descriptions-item>
          <el-descriptions-item label="部门领导">{{ current.deptLeaderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上级领导">
            {{ current.upperLeaderName || (current.days > 3 ? '-' : '无需') }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(current.status)" size="small">{{ current.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ current.currentActivityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前审批人">{{ current.currentAssigneeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="事由" :span="3">{{ current.reason }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">流程进度</div>
        <div class="chart-box">
          <ProcessViewer v-if="detailVisible && detailXml" :xml="detailXml" :active-ids="activeIds" />
        </div>

        <div class="section-title">审批记录</div>
        <el-timeline v-if="comments.length">
          <el-timeline-item v-for="c in comments" :key="c.id" :timestamp="c.time" placement="top">
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
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import ProcessViewer from '@/components/ProcessViewer.vue'
import { pageMyLeave } from '@/api/leave'
import { getActiveActivities, getProcessInstance, getProcessXml } from '@/api/process'
import { getComments } from '@/api/task'
import type { CommentInfo, LeaveInfo } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<LeaveInfo[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined })

const detailVisible = ref(false)
const current = ref<LeaveInfo | null>(null)
const detailXml = ref('')
const activeIds = ref<string[]>([])
const comments = ref<CommentInfo[]>([])

function statusType(status: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const res = await pageMyLeave({ page: query.page, size: query.size, status: query.status })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.status = undefined
  query.page = 1
  load()
}

async function openDetail(row: LeaveInfo) {
  current.value = row
  detailVisible.value = true
  activeIds.value = []
  comments.value = []
  detailXml.value = ''
  if (!row.processInstanceId) return
  const instance = (await getProcessInstance(row.processInstanceId)).data
  detailXml.value = (await getProcessXml(instance.processDefinitionId)).data
  comments.value = (await getComments(row.processInstanceId)).data
  if (!instance.ended) {
    activeIds.value = (await getActiveActivities(row.processInstanceId)).data
  }
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
