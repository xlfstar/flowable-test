<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="流程名称">
            <el-input
              v-model="query.name"
              placeholder="请输入流程名称"
              clearable
              @keyup.enter="load"
              @clear="load"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Upload" @click="openDeploy">部署流程</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="name" label="流程名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="openDesigner(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="key" label="流程Key" min-width="140" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="category" label="分类" min-width="120" show-overflow-tooltip />
        <el-table-column prop="deployTime" label="部署时间" width="170" />
        <el-table-column label="部署状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.deploymentId ? 'success' : 'info'" size="small">
              {{ row.deploymentId ? '已部署' : '未部署' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.suspended ? 'warning' : 'success'" size="small">
              {{ row.suspended ? '已挂起' : '已激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDesigner(row)">设计流程</el-button>
            <el-button link type="primary" @click="viewXml(row)">XML</el-button>
            <el-button link type="warning" @click="toggleSuspend(row)">
              {{ row.suspended ? '激活' : '挂起' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="deployVisible" title="部署流程" width="520px">
      <el-form label-width="90px">
        <el-form-item label="部署名称">
          <el-input v-model="deployForm.name" placeholder="不填则使用文件名" />
        </el-form-item>
        <el-form-item label="BPMN文件">
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".bpmn,.xml,.bpmn20.xml,.zip"
            :on-change="onFileChange"
            :on-exceed="onExceed"
            :file-list="fileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .bpmn / .bpmn20.xml / .xml / .zip</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployVisible = false">取消</el-button>
        <el-button type="primary" :loading="deploying" @click="submitDeploy">确定部署</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewerVisible" :title="viewerTitle" width="900px" top="6vh">
      <div style="height: 60vh">
        <ProcessViewer v-if="viewerVisible" :xml="viewerXml" :active-ids="[]" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile, type UploadFiles } from 'element-plus'
import { Refresh, Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import ProcessViewer from '@/components/ProcessViewer.vue'
import {
  activateProcess,
  deleteDeployment,
  deployProcess,
  getProcessXml,
  pageProcessDefinitions,
  suspendProcess
} from '@/api/process'
import type { ProcessDefinition } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<ProcessDefinition[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, name: '' })

const deployVisible = ref(false)
const deploying = ref(false)
const deployForm = reactive({ name: '', file: null as File | null })
const fileList = ref<UploadFiles>([])

const viewerVisible = ref(false)
const viewerTitle = ref('')
const viewerXml = ref('')

async function load() {
  loading.value = true
  try {
    const res = await pageProcessDefinitions({ page: query.page, size: query.size, name: query.name })
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

function openDeploy() {
  deployForm.name = ''
  deployForm.file = null
  fileList.value = []
  deployVisible.value = true
}

function onFileChange(file: UploadFile, files: UploadFiles) {
  deployForm.file = file.raw as File
  fileList.value = files.slice(-1)
}

function onExceed(files: File[]) {
  fileList.value = []
  const file = files[0]
  if (file) {
    deployForm.file = file
    fileList.value = [{ name: file.name, raw: file as unknown as UploadFile['raw'] } as UploadFile]
  }
}

async function submitDeploy() {
  if (!deployForm.file) {
    ElMessage.warning('请选择要部署的 BPMN 文件')
    return
  }
  deploying.value = true
  try {
    const res = await deployProcess(deployForm.file, deployForm.name || undefined)
    ElMessage.success(`部署成功: ${res.data.name} (v${res.data.version})`)
    deployVisible.value = false
    load()
  } finally {
    deploying.value = false
  }
}

function openDesigner(row: ProcessDefinition) {
  router.push({ path: '/process/designer', query: { definitionId: row.id } })
}

async function viewXml(row: ProcessDefinition) {
  const res = await getProcessXml(row.id)
  viewerTitle.value = `${row.name} - BPMN XML`
  viewerXml.value = res.data
  viewerVisible.value = true
}

async function toggleSuspend(row: ProcessDefinition) {
  if (row.suspended) {
    await activateProcess(row.id)
    ElMessage.success('已激活')
  } else {
    await suspendProcess(row.id)
    ElMessage.success('已挂起')
  }
  load()
}

async function remove(row: ProcessDefinition) {
  await ElMessageBox.confirm(`确定删除流程定义「${row.name}」及其部署吗?`, '提示', {
    type: 'warning'
  })
  await deleteDeployment(row.deploymentId, true)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
