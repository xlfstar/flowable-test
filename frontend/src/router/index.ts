import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'process/definition',
        name: 'ProcessDefinition',
        component: () => import('@/views/process/definition.vue'),
        meta: { title: '流程定义', icon: 'Files' }
      },
      {
        path: 'process/start',
        name: 'ProcessStart',
        component: () => import('@/views/process/start.vue'),
        meta: { title: '发起流程', icon: 'Promotion' }
      },
      {
        path: 'process/designer',
        name: 'ProcessDesigner',
        component: () => import('@/views/process/designer.vue'),
        meta: { title: '流程设计器', icon: 'EditPen' }
      },
      {
        path: 'process/instance',
        name: 'ProcessInstance',
        component: () => import('@/views/process/instance.vue'),
        meta: { title: '流程实例', icon: 'DataLine' }
      },
      {
        path: 'process/instance/:id',
        name: 'ProcessInstanceDetail',
        component: () => import('@/views/process/instance-detail.vue'),
        meta: { title: '流程实例详情', icon: 'DataLine' }
      },
      {
        path: 'org/structure',
        name: 'OrgStructure',
        component: () => import('@/views/org/structure.vue'),
        meta: { title: '组织架构', icon: 'OfficeBuilding' }
      },
      {
        path: 'task/todo',
        name: 'TodoTask',
        component: () => import('@/views/task/todo.vue'),
        meta: { title: '我的待办', icon: 'Bell' }
      },
      {
        path: 'task/done',
        name: 'DoneTask',
        component: () => import('@/views/task/done.vue'),
        meta: { title: '我的已办', icon: 'CircleCheck' }
      },
      {
        path: 'leave/apply',
        name: 'LeaveApply',
        component: () => import('@/views/leave/apply.vue'),
        meta: { title: '请假申请', icon: 'Calendar' }
      },
      {
        path: 'leave/list',
        name: 'LeaveList',
        component: () => import('@/views/leave/list.vue'),
        meta: { title: '我的申请', icon: 'Tickets' }
      },
      {
        path: 'form/list',
        name: 'FormList',
        component: () => import('@/views/form/list.vue'),
        meta: { title: '自定义表单', icon: 'Grid' }
      },
      {
        path: 'form/designer/:id',
        name: 'FormDesigner',
        component: () => import('@/views/form/designer.vue'),
        meta: { title: '表单设计器', icon: 'EditPen' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
  return true
})

export default router
