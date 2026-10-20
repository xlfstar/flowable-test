<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="运行中" name="running" />
        <el-tab-pane label="我发起的" name="mine" />
      </el-tabs>

      <div class="table-toolbar">
        <el-form v-if="activeTab === 'running'" :inline="true" @submit.prevent>
          <el-form-item label="流程Key">
            <el-input v-model="query.key" placeholder="流程定义Key" clearable @keyup.enter="load" @clear="load" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
        <div v-else />
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="processDefinitionName" label="流程名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="businessKey" label="业务Key" width="120" />
        <el-table-column prop="startUserName" label="发起人" width="110" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.ended ? 'info' : 'success'" size="small">
              {{ row.ended ? '已结束' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="170">
          <template #default="{ row }">{{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="!row.ended" link type="danger" @click="remove(row)">撤销</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteProcessInstance,
  pageMyProcessInstances,
  pageProcessInstances
} from '@/api/process'
import type { ProcessInstance } from '@/types'

const router = useRouter()
const activeTab = ref('running')
const loading = ref(false)
const list = ref<ProcessInstance[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, key: '' })

async function load() {
  loading.value = true
  try {
    const res =
      activeTab.value === 'running'
        ? await pageProcessInstances({ page: query.page, size: query.size, key: query.key })
        : await pageMyProcessInstances({ page: query.page, size: query.size })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.page = 1
  load()
}

function reset() {
  query.key = ''
  query.page = 1
  load()
}

function openDetail(row: ProcessInstance) {
  router.push({ name: 'ProcessInstanceDetail', params: { id: row.id } })
}

async function remove(row: ProcessInstance) {
  await ElMessageBox.confirm('确定撤销该流程实例吗?', '提示', { type: 'warning' })
  await deleteProcessInstance(row.id, '用户撤销')
  ElMessage.success('已撤销')
  load()
}

onMounted(load)
</script>
