<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon><Share /></el-icon>
        <span>Flowable 工作流</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#001529"
          text-color="#c0c4cc"
          active-text-color="#ffffff"
        >
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tag v-if="userStore.user?.deptName" size="small" effect="plain">
            {{ userStore.user.deptName }}
          </el-tag>
          <el-tag v-if="userStore.user?.positionName" size="small" type="success" effect="plain">
            {{ userStore.user.positionName }}
          </el-tag>
          <el-tag size="small" type="info" effect="plain">{{ roleLabel }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
              <span class="name">{{ userStore.user?.realName || userStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface MenuItem {
  path: string
  title: string
  icon: string
}

const menus: MenuItem[] = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/process/definition', title: '流程定义', icon: 'Files' },
  { path: '/process/start', title: '发起流程', icon: 'Promotion' },
  { path: '/process/designer', title: '流程设计器', icon: 'EditPen' },
  { path: '/process/instance', title: '流程实例', icon: 'DataLine' },
  { path: '/org/structure', title: '组织架构', icon: 'OfficeBuilding' },
  { path: '/task/todo', title: '我的待办', icon: 'Bell' },
  { path: '/task/done', title: '我的已办', icon: 'CircleCheck' },
  { path: '/form/list', title: '自定义表单', icon: 'Grid' },
  { path: '/leave/apply', title: '请假申请', icon: 'Calendar' },
  { path: '/leave/list', title: '我的申请', icon: 'Tickets' }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '')
const avatarText = computed(() => {
  const name = userStore.user?.realName || userStore.user?.username || ''
  return name.slice(0, 1)
})

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
const roleLabel = computed(() => roleLabels[userStore.user?.role || ''] || userStore.user?.role || '')

async function onCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗?', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
}

.aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  background-color: #409eff;
}

.name {
  color: #303133;
}

.main {
  padding: 0;
  overflow: auto;
}
</style>
