<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-form :inline="true" @submit.prevent>
          <el-form-item label="表单名称">
            <el-input v-model="query.name" placeholder="请输入表单名称" clearable @keyup.enter="load" @clear="load" />
          </el-form-item>
          <el-form-item label="表单标识">
            <el-input v-model="query.key" placeholder="请输入表单标识" clearable @keyup.enter="load" @clear="load" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="load">查询</el-button>
            <el-button :icon="Refresh" @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建表单</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="#" width="55" align="center" />
        <el-table-column prop="name" label="表单名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="formKey" label="表单标识" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="字段数" width="90" align="center">
          <template #default="{ row }">{{ fieldCount(row.schemaJson) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑表单' : '新建表单'" width="520px">
      <el-form ref="editRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="表单标识" prop="formKey">
          <el-input v-model="form.formKey" placeholder="如 leaveApprovalForm, 与 BPMN formKey 对应" />
        </el-form-item>
        <el-form-item label="表单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入表单名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="请输入分类(可选)" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注(可选)" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createForm, deleteForm, pageForms, updateForm } from '@/api/form'
import type { FormDefinition } from '@/types'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref<FormDefinition[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, name: '', key: '' })

const dialogVisible = ref(false)
const editRef = ref<FormInstance>()
const form = reactive<FormDefinition>({ id: undefined, formKey: '', name: '', category: '', schemaJson: '[]', remark: '', status: 1 })

const rules: FormRules = {
  formKey: [{ required: true, message: '请输入表单标识', trigger: 'blur' }],
  name: [{ required: true, message: '请输入表单名称', trigger: 'blur' }]
}

function fieldCount(schemaJson?: string): number {
  try {
    const parsed = schemaJson ? JSON.parse(schemaJson) : []
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return 0
  }
}

async function load() {
  loading.value = true
  try {
    const res = await pageForms({ page: query.page, size: query.size, name: query.name, key: query.key })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.name = ''
  query.key = ''
  query.page = 1
  load()
}

function openCreate() {
  Object.assign(form, { id: undefined, formKey: '', name: '', category: '', schemaJson: '[]', remark: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: FormDefinition) {
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDesigner(row: FormDefinition) {
  router.push(`/form/designer/${row.id}`)
}

async function save() {
  if (!editRef.value) return
  await editRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        formKey: form.formKey,
        name: form.name,
        category: form.category,
        schemaJson: form.schemaJson,
        remark: form.remark,
        status: form.status
      }
      if (form.id) {
        await updateForm(form.id, payload)
      } else {
        const res = await createForm(payload)
        ElMessage.success('创建成功, 可继续设计表单字段')
        dialogVisible.value = false
        load()
        router.push(`/form/designer/${res.data.id}`)
        return
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

async function remove(row: FormDefinition) {
  await ElMessageBox.confirm(`确定删除表单「${row.name}」吗?`, '提示', { type: 'warning' })
  await deleteForm(row.id as number)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
