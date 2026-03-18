<template>
  <div class="profile-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <div class="header-actions">
            <el-button @click="loadProfile" :loading="loading">刷新</el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          </div>
        </div>
      </template>

      <el-skeleton :loading="loading" animated>
        <el-form
          ref="formRef"
          :model="profileForm"
          :rules="rules"
          label-width="110px"
          class="profile-form"
        >
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model.trim="profileForm.username" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角色">
                <div class="role-list">
                  <el-tag
                    v-for="roleCode in currentRoles"
                    :key="roleCode"
                    type="success"
                    size="small"
                  >
                    {{ roleCode }}
                  </el-tag>
                  <span v-if="currentRoles.length === 0">-</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="姓名">
                <el-input v-model="profileForm.realName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="部门">
                <el-input v-model="profileForm.department" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="姓名信息由管理员（或具备用户管理权限人员）维护，个人页面仅支持修改用户名与密码。"
            class="notice-alert"
          />

          <el-divider>账号安全</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input
                  v-model="profileForm.currentPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="修改用户名或密码时必填"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="profileForm.newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="不修改可留空"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="profileForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="再次输入新密码"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const originalUsername = ref('')

const profileForm = reactive({
  username: '',
  realName: '',
  department: '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  newPassword: [
    {
      validator: (_, value: string, callback: (error?: Error) => void) => {
        if (!value) {
          callback()
          return
        }
        if (value.length < 6) {
          callback(new Error('新密码长度不能小于 6 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (_, value: string, callback: (error?: Error) => void) => {
        if (!profileForm.newPassword && !value) {
          callback()
          return
        }
        if (value !== profileForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const currentRoles = computed(() => userStore.userInfo?.roles ?? [])

const applyProfile = (profile?: Partial<UserInfo>) => {
  profileForm.username = profile?.username ?? userStore.userInfo?.username ?? ''
  profileForm.realName = profile?.realName ?? userStore.userInfo?.realName ?? ''
  profileForm.department = profile?.department ?? userStore.userInfo?.department ?? ''
  profileForm.currentPassword = ''
  profileForm.newPassword = ''
  profileForm.confirmPassword = ''
  originalUsername.value = profileForm.username
}

const loadProfile = async () => {
  loading.value = true
  try {
    const profile = await authApi.getCurrentUser()
    applyProfile(profile)
    userStore.setUserInfo({
      ...(userStore.userInfo ?? {
        id: profile.id,
        roles: profile.roles ?? []
      }),
      ...profile
    })
    userStore.setPermissions(profile.permissions ?? [])
  } catch (error) {
    applyProfile()
    console.error('加载个人信息失败:', error)
  } finally {
    loading.value = false
  }
}

const hasPendingChanges = () => {
  const usernameChanged = profileForm.username.trim() !== originalUsername.value
  const passwordChanged = !!profileForm.newPassword
  return usernameChanged || passwordChanged
}

const handleSave = async () => {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    if (!hasPendingChanges()) {
      ElMessage.info('未检测到账号信息变更')
      return
    }

    if (!profileForm.currentPassword) {
      ElMessage.warning('修改用户名或密码需要输入当前密码')
      return
    }

    saving.value = true
    try {
      const profile = await authApi.updateProfile({
        username: profileForm.username.trim(),
        currentPassword: profileForm.currentPassword,
        newPassword: profileForm.newPassword || undefined
      })
      userStore.setUserInfo({
        ...(userStore.userInfo ?? {
          id: profile.id,
          roles: profile.roles ?? []
        }),
        ...profile
      })
      userStore.setPermissions(profile.permissions ?? [])
      applyProfile(profile)
      ElMessage.success('账号信息更新成功')
    } catch (error) {
      console.error('更新账号信息失败:', error)
    } finally {
      saving.value = false
    }
  })
}

loadProfile()
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.profile-form {
  max-width: 920px;
}

.notice-alert {
  margin-bottom: 16px;
}

.role-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
