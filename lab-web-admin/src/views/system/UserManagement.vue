<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" class="query-keyword-input" placeholder="用户名 / 姓名" clearable />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select
            v-model="queryForm.userType"
            v-adaptive-select-width="['全部', '管理员', '教师', '学生']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="管理员" :value="1" />
            <el-option label="教师" :value="2" />
            <el-option label="学生" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="userList" border stripe v-loading="loading" class="data-table">
        <el-table-column label="排序" width="130" align="center">
          <template #default="{ $index }">
            <div class="order-actions">
              <el-button
                text
                class="order-arrow-btn"
                :disabled="$index === 0"
                title="上移"
                @click="moveUserUp($index)"
              >
                ↑
              </el-button>
              <el-button
                text
                class="order-arrow-btn"
                :disabled="$index === userList.length - 1"
                title="下移"
                @click="moveUserDown($index)"
              >
                ↓
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column label="用户类型">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag
                v-for="role in row.roles || []"
                :key="`${row.id}-${role.id}`"
                round
                size="small"
                class="role-tag"
                :style="getRoleTagStyle(role.roleCode)"
              >
                {{ role.roleName }}
              </el-tag>
              <el-tag
                v-if="!row.roles || row.roles.length === 0"
                round
                size="small"
                type="info"
              >
                未分配角色
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleAssignRoles(row)">分配角色</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="userForm" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="userForm.userType" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="教师" :value="2" />
            <el-option label="学生" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="userForm.department" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <el-checkbox-group v-model="selectedRoleIds" class="role-checkbox-group">
        <el-checkbox v-for="role in roleList" :key="role.id" :label="role.id">
          {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import type { Role, User, UserForm, UserQuery } from '@/types/user'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const userList = ref<User[]>([])
const roleList = ref<Role[]>([])
const total = ref(0)
const currentUserId = ref<number>()
const selectedRoleIds = ref<number[]>([])
const USER_ORDER_STORAGE_KEY = 'lab-user-management-display-order'
const userDisplayOrderMap = ref<Record<number, number>>({})

const queryForm = reactive<UserQuery>({
  keyword: '',
  userType: -1,
  page: 1,
  size: 10
})

const userForm = reactive<UserForm>({
  username: '',
  password: '',
  realName: '',
  userType: 2,
  department: '',
  phone: '',
  email: '',
  status: 1
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
}

const roleMap = computed(() => {
  const map = new Map<number, Role>()
  roleList.value.forEach(role => map.set(role.id, role))
  return map
})

const normalizeUsername = (username?: string) => (username ?? '').trim().toLowerCase()
const normalizeRoleCode = (roleCode?: string) => (roleCode ?? '').trim().toUpperCase()
const isProtectedRoleCode = (roleCode?: string) => {
  const normalized = normalizeRoleCode(roleCode)
  return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN'
}
const isProtectedUsername = (username?: string) => normalizeUsername(username) === 'admin'
const isProtectedUser = (user: Pick<User, 'username' | 'roles'>) => {
  if (isProtectedUsername(user.username)) {
    return true
  }
  return (user.roles ?? []).some(role => isProtectedRoleCode(role.roleCode))
}

const getRoleTagStyle = (roleCode?: string) => {
  const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#722ed1']
  const normalized = (roleCode ?? '').trim()
  const hash = normalized.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)
  const color = palette[hash % palette.length]
  return {
    backgroundColor: color,
    borderColor: color,
    color: '#ffffff'
  }
}

const mapRoleIdsToRoleObjects = (roleIds: number[]): Role[] => {
  return roleIds.map(roleId => {
    const role = roleMap.value.get(roleId)
    if (role) {
      return role
    }
    return {
      id: roleId,
      roleCode: `ROLE_${roleId}`,
      roleName: `角色#${roleId}`,
      status: 1
    }
  })
}

const fillUserRoles = async (users: User[]) => {
  const usersWithRoles = await Promise.all(
    users.map(async (user) => {
      try {
        const roleIds = await userApi.getUserRoles(user.id)
        return { ...user, roles: mapRoleIdsToRoleObjects(roleIds) }
      } catch (error) {
        console.error(`加载用户 ${user.id} 角色失败:`, error)
        return { ...user, roles: [] }
      }
    })
  )
  return usersWithRoles
}

const loadUserDisplayOrder = () => {
  try {
    const raw = localStorage.getItem(USER_ORDER_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as Record<string, number>
    const normalized: Record<number, number> = {}
    Object.entries(parsed).forEach(([key, value]) => {
      const id = Number(key)
      if (Number.isFinite(id) && Number.isFinite(value)) {
        normalized[id] = value
      }
    })
    userDisplayOrderMap.value = normalized
  } catch (error) {
    console.error('加载用户显示顺序失败:', error)
  }
}

const saveUserDisplayOrder = () => {
  try {
    localStorage.setItem(USER_ORDER_STORAGE_KEY, JSON.stringify(userDisplayOrderMap.value))
  } catch (error) {
    console.error('保存用户显示顺序失败:', error)
  }
}

const applyUserDisplayOrder = (users: User[]) => {
  return [...users].sort((left, right) => {
    const leftOrder = userDisplayOrderMap.value[left.id]
    const rightOrder = userDisplayOrderMap.value[right.id]
    const fallback = Number.MAX_SAFE_INTEGER
    const leftRank = Number.isFinite(leftOrder) ? leftOrder : fallback
    const rightRank = Number.isFinite(rightOrder) ? rightOrder : fallback
    if (leftRank !== rightRank) {
      return leftRank - rightRank
    }
    return left.id - right.id
  })
}

const syncUserDisplayOrder = () => {
  const nextMap = { ...userDisplayOrderMap.value }
  userList.value.forEach((user, index) => {
    nextMap[user.id] = index + 1
  })
  userDisplayOrderMap.value = nextMap
  saveUserDisplayOrder()
}

const moveUserUp = (index: number) => {
  if (index <= 0) return
  const list = [...userList.value]
  ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
  userList.value = list
  syncUserDisplayOrder()
}

const moveUserDown = (index: number) => {
  if (index >= userList.value.length - 1) return
  const list = [...userList.value]
  ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
  userList.value = list
  syncUserDisplayOrder()
}

const loadRoleList = async () => {
  const res = await userApi.getRoleList()
  roleList.value = res.list
}

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserList(queryForm)
    const visibleUsers = res.list.filter(user => !isProtectedUsername(user.username))
    total.value = Math.max(res.total - (res.list.length - visibleUsers.length), 0)
    const usersWithRoles = await fillUserRoles(visibleUsers)
    const filteredUsers = usersWithRoles.filter(user => !isProtectedUser(user))
    userList.value = applyUserDisplayOrder(filteredUsers)
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadUserList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.userType = -1
  queryForm.page = 1
  loadUserList()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    id: undefined,
    username: '',
    password: '',
    realName: '',
    userType: 2,
    department: '',
    phone: '',
    email: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  if (isProtectedUser(row)) {
    ElMessage.warning('系统管理员账号不允许编辑')
    return
  }
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    userType: row.userType,
    department: row.department,
    phone: row.phone,
    email: row.email,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (userForm.id) {
        await userApi.updateUser(userForm.id, userForm)
        ElMessage.success('更新成功')
      } else {
        await userApi.createUser(userForm)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadUserList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleStatusChange = async (row: User) => {
  if (isProtectedUser(row)) {
    ElMessage.warning('系统管理员账号不允许修改状态')
    return
  }
  try {
    await userApi.updateUser(row.id, {
      ...row,
      roleIds: row.roles?.map(role => role.id)
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('状态更新失败:', error)
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleDelete = async (row: User) => {
  if (isProtectedUser(row)) {
    ElMessage.warning('系统管理员账号不允许删除')
    return
  }
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await userApi.deleteUser(row.id)
    ElMessage.success('删除成功')
    await loadUserList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleAssignRoles = async (row: User) => {
  if (isProtectedUser(row)) {
    ElMessage.warning('系统管理员账号不允许分配角色')
    return
  }
  currentUserId.value = row.id
  try {
    selectedRoleIds.value = await userApi.getUserRoles(row.id)
  } catch (error) {
    selectedRoleIds.value = row.roles?.map(role => role.id) ?? []
    console.error('加载用户角色失败:', error)
  }
  roleDialogVisible.value = true
}

const handleRoleSubmit = async () => {
  if (!currentUserId.value) return

  submitting.value = true
  try {
    await userApi.assignRoles(currentUserId.value, selectedRoleIds.value)
    ElMessage.success('分配角色成功')
    roleDialogVisible.value = false
    await loadUserList()
  } catch (error) {
    console.error('分配角色失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(async () => {
  loadUserDisplayOrder()
  try {
    await loadRoleList()
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
  await loadUserList()
})
</script>

<style scoped>
.user-management {
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.query-form {
  margin-bottom: 10px;
  padding: 14px 14px 2px;
  border: 1px solid #e9f0fb;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.data-table {
  margin-top: 4px;
}

.data-table :deep(.el-table__row:hover > td) {
  background: #f7fbff !important;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.role-tag {
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.12);
}

.role-checkbox-group {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.role-checkbox-group :deep(.el-checkbox) {
  margin-right: 0;
  padding: 8px 10px;
  border: 1px solid #e6edf9;
  border-radius: 10px;
  background: #f9fbff;
}

.order-actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.order-arrow-btn {
  min-width: 22px;
  height: 22px;
  padding: 0;
  font-size: 14px;
  line-height: 1;
  color: #475569;
}

.order-arrow-btn:hover {
  color: #1d4ed8;
}

.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
