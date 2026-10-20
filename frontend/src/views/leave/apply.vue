<template>
  <div class="page-container">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <span>请假申请</span>
          <el-tag v-if="userStore.user?.deptName" size="small" effect="plain">
            {{ userStore.user.deptName }}
          </el-tag>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="审批人由组织架构自动匹配: 先由您所在部门的负责人审批; 超过3天会再增加上一级部门负责人审批。"
        style="margin-bottom: 20px"
      />

      <el-descriptions :column="3" border size="small" style="margin-bottom: 20px">
        <el-descriptions-item label="申请人">
          {{ userStore.user?.realName || userStore.user?.username }}
        </el-descriptions-item>
        <el-descriptions-item label="部门">{{ userStore.user?.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ userStore.user?.positionName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 620px">
        <el-form-item label="请假日期" prop="range">
          <el-date-picker
            v-model="range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="onRangeChange"
          />
        </el-form-item>
        <el-form-item label="请假天数" prop="days">
          <el-input-number v-model="form.days" :min="1" :max="365" />
          <span class="days-hint">超过 3 天需上级领导审批</span>
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入请假事由" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="approvalChain != null && !approvalChain.valid"
            @click="onSubmit"
          >
            提交申请
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="approval-section">
        <div class="section-title">
          审批路径
          <el-tag v-if="form.days > 3" size="small" type="warning" effect="plain">
            部门领导 + 上级领导
          </el-tag>
          <el-tag v-else size="small" type="success" effect="plain">部门领导</el-tag>
        </div>

        <div v-loading="chainLoading" class="chain-box">
          <div v-if="approvalChain?.valid && approvalChain.approvers.length" class="approver-flow">
            <template v-for="(approver, index) in approvalChain.approvers" :key="approver.userId">
              <div class="approver-card">
                <div class="approver-level">{{ approver.levelName }}</div>
                <div class="approver-name">{{ approver.userName }}</div>
                <div class="approver-meta">
                  {{ approver.deptName }}
                  <span v-if="approver.positionName"> · {{ approver.positionName }}</span>
                </div>
              </div>
              <el-icon v-if="index < approvalChain.approvers.length - 1" class="flow-arrow">
                <Right />
              </el-icon>
            </template>
          </div>
          <el-alert
            v-else-if="approvalChain && !approvalChain.valid"
            type="warning"
            :closable="false"
            show-icon
            :title="approvalChain.message || '当前组织架构未配置完整审批人'"
          />
          <el-empty v-else :image-size="48" description="暂无审批路径" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Right } from '@element-plus/icons-vue'
import { applyLeave } from '@/api/leave'
import { getApprovalChain } from '@/api/org'
import { useUserStore } from '@/stores/user'
import type { ApprovalChainInfo } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const chainLoading = ref(false)
const approvalChain = ref<ApprovalChainInfo | null>(null)
const range = ref<[string, string] | null>(null)

const form = reactive({
  days: 1,
  startDate: '',
  endDate: '',
  reason: ''
})

const rules: FormRules = {
  days: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }]
}

watch(
  () => form.days,
  () => loadApprovalChain()
)

function onRangeChange(value: [string, string] | null) {
  if (value && value.length === 2) {
    form.startDate = value[0]
    form.endDate = value[1]
    form.days = dayjs(value[1]).diff(dayjs(value[0]), 'day') + 1
  } else {
    form.startDate = ''
    form.endDate = ''
  }
}

async function loadApprovalChain() {
  if (!form.days || form.days < 1) {
    approvalChain.value = null
    return
  }
  chainLoading.value = true
  try {
    const res = await getApprovalChain(form.days)
    approvalChain.value = res.data
  } catch (error) {
    approvalChain.value = null
  } finally {
    chainLoading.value = false
  }
}

async function onSubmit() {
  if (!formRef.value) return
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择请假日期')
    return
  }
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await applyLeave({
        days: form.days,
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason
      })
      ElMessage.success('提交成功，已进入部门领导审批')
      router.push('/leave/list')
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  formRef.value?.resetFields()
  form.reason = ''
  form.days = 1
  range.value = null
  form.startDate = ''
  form.endDate = ''
}

onMounted(() => {
  userStore.fetchUserInfo().catch(() => undefined)
  loadApprovalChain()
})
</script>

<style scoped lang="scss">
.form-card {
  max-width: 900px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.days-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.approval-section {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.chain-box {
  min-height: 60px;
}

.approver-flow {
  display: flex;
  align-items: stretch;
  gap: 12px;
  flex-wrap: wrap;
}

.approver-card {
  min-width: 190px;
  padding: 12px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f8fbff;
}

.approver-level {
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
}

.approver-name {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 600;
}

.approver-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.flow-arrow {
  align-self: center;
  color: #c0c4cc;
}
</style>
