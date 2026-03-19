<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-panel">
        <h1>智慧实验室管理系统</h1>
        <p>统一管理药品、库存、审批与预警，提升实验室运营效率。</p>

        <ul class="feature-list">
          <li><el-icon><CircleCheck /></el-icon> 物资台账与库存数据实时联动</li>
          <li><el-icon><CircleCheck /></el-icon> 领用审批、出入库流程闭环管理</li>
          <li><el-icon><CircleCheck /></el-icon> 多角色权限可配置、可追溯</li>
        </ul>
      </div>
      <img :src="heroImage" alt="实验室插图" class="hero-image" />
    </div>

    <div class="login-right">
      <el-card class="login-card" shadow="never">
        <template #header>
          <div class="card-header">
            <h2>欢迎登录</h2>
            <span>请使用您的账号信息登录系统</span>
          </div>
        </template>

        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0" size="large">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              登录系统
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import heroImage from '@/assets/hero.png'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) {
    return
  }

  await loginFormRef.value.validate(async valid => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      const response = await authApi.login(loginForm)
      userStore.setToken(response.token, response.refreshToken)
      userStore.setUserInfo(response.userInfo)
      userStore.setPermissions(response.permissions ?? response.userInfo?.permissions ?? [])
      userStore.setMenuList(response.menus ?? [])
      ElMessage.success('登录成功')
      await router.replace('/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  background:
    radial-gradient(circle at 20% 10%, #dbeafe 0%, transparent 40%),
    radial-gradient(circle at 90% 90%, #dcfce7 0%, transparent 35%),
    #eff6ff;
}

.login-left {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 22px;
  padding: 48px 64px;
}

.brand-panel h1 {
  font-size: 40px;
  color: #0f172a;
  margin-bottom: 12px;
}

.brand-panel p {
  max-width: 520px;
  font-size: 16px;
  line-height: 1.8;
  color: #475569;
  margin-bottom: 18px;
}

.feature-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #334155;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
}

.feature-list .el-icon {
  color: #10b981;
}

.hero-image {
  width: min(560px, 80%);
  max-height: 320px;
  object-fit: contain;
  filter: drop-shadow(0 16px 26px rgba(59, 130, 246, 0.2));
}

.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.login-card {
  width: 420px;
  border-radius: 18px;
  border: 1px solid #dbe8ff;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-header h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.card-header span {
  color: #64748b;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 42px;
}

@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-left {
    display: none;
  }

  .login-right {
    padding: 20px;
  }
}
</style>
