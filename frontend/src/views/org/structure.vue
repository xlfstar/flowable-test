<template>
  <div class="page-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="请假审批人由组织架构动态解析: 先由申请人所在部门负责人审批; 超过3天再增加上一级部门负责人审批。"
      style="margin-bottom: 16px"
    />

    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card v-loading="loading" shadow="never" class="org-card">
          <template #header>
            <div class="card-header">
              <span>部门架构</span>
              <el-tag size="small" effect="plain">{{ departmentCount }} 个部门</el-tag>
            </div>
          </template>
          <el-tree
            :data="departments"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            :props="{ label: 'deptName', children: 'children' }"
          >
            <template #default="{ data }">
              <div class="dept-node">
                <span class="dept-name">{{ data.deptName }}</span>
                <el-tag v-if="data.leaderName" size="small" type="success" effect="plain">
                  负责人: {{ data.leaderName }}
                </el-tag>
                <el-tag v-else size="small" type="warning" effect="plain">未配置负责人</el-tag>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <el-tab-pane :label="`成员 (${users.length})`" name="users">
              <el-table v-loading="loading" :data="users" border stripe>
                <el-table-column prop="realName" label="姓名" width="110" />
                <el-table-column prop="username" label="账号" width="120" />
                <el-table-column prop="deptName" label="部门" min-width="120">
                  <template #default="{ row }">{{ row.deptName || '-' }}</template>
                </el-table-column>
                <el-table-column prop="positionName" label="岗位" min-width="120">
                  <template #default="{ row }">{{ row.positionName || '-' }}</template>
                </el-table-column>
                <el-table-column label="角色" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain">{{ roleLabel(row.role) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="部门负责人" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.departmentLeader" type="success" size="small">是</el-tag>
                    <span v-else class="muted">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                      {{ row.status === 1 ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`岗位 (${positions.length})`" name="positions">
              <el-table v-loading="loading" :data="positions" border stripe>
                <el-table-column prop="positionCode" label="岗位编码" width="120" />
                <el-table-column prop="positionName" label="岗位名称" min-width="140" />
                <el-table-column prop="positionLevel" label="层级" width="90" align="center" />
                <el-table-column prop="sort" label="排序" width="90" align="center" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                      {{ row.status === 1 ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDepartmentTree, getOrgUsers, getPositions } from '@/api/org'
import type { DepartmentInfo, OrgUserInfo, PositionInfo } from '@/types'

const loading = ref(false)
const activeTab = ref('users')
const departments = ref<DepartmentInfo[]>([])
const positions = ref<PositionInfo[]>([])
const users = ref<OrgUserInfo[]>([])

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

const departmentCount = computed(() => countDepartments(departments.value))

function countDepartments(nodes: DepartmentInfo[]): number {
  return nodes.reduce((total, node) => total + 1 + countDepartments(node.children || []), 0)
}

function roleLabel(role?: string): string {
  if (!role) return '-'
  return roleLabels[role] || role
}

async function load() {
  loading.value = true
  try {
    const [deptRes, positionRes, userRes] = await Promise.allSettled([
      getDepartmentTree(),
      getPositions(),
      getOrgUsers()
    ])
    if (deptRes.status === 'fulfilled') departments.value = deptRes.value.data
    if (positionRes.status === 'fulfilled') positions.value = positionRes.value.data
    if (userRes.status === 'fulfilled') users.value = userRes.value.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.org-card {
  min-height: 460px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dept-node {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dept-name {
  font-weight: 500;
}

.muted {
  color: #c0c4cc;
}
</style>
