<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="40" color="#409eff"><Share /></el-icon>
        <h2>Flowable 工作流示例</h2>
        <p>Spring Boot 3 + Flowable 7 + Vue3 + Element Plus</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider>演示账号 (密码 123456)</el-divider>
      <div class="demo-accounts">
        <el-tag
          v-for="account in accounts"
          :key="account.username"
          class="account-tag"
          @click="fill(account.username)"
        >
          {{ account.label }}: {{ account.username }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { label: '业务员', username: 'ywy' },
  { label: '财务', username: 'caiwu' },
  { label: '顾问主管', username: 'gws' },
  { label: '顾问', username: 'gw' },
  { label: '会计主管', username: 'kjs' },
  { label: '会计', username: 'kj' },
  { label: '员工', username: 'zhangsan' },
  { label: '技术经理', username: 'manager' },
  { label: '总经理', username: 'zongjingli' },
  { label: '董事长', username: 'dongshizhang' },
  { label: '人事经理', username: 'hrManager' },
  { label: '人事专员', username: 'hr' },
  { label: '管理员', username: 'admin' }
]

function fill(username: string) {
  form.username = username
  form.password = '123456'
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f4287 0%, #0b1e3d 100%);
}

.login-card {
  width: 420px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;

  h2 {
    margin: 12px 0 6px;
    color: #1f4287;
  }

  p {
    margin: 0;
    color: #909399;
    font-size: 13px;
  }
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.account-tag {
  cursor: pointer;
}

:deep(.el-divider__text) {
  font-size: 12px;
  color: #909399;
}
</style>
