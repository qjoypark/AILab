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

      <el-form :model="queryForm" inline>
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" placeholder="用户名 / 姓名" clearable />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="queryForm.userType" placeholder="请选择" clearable>
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

      <el-table :data="userList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
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
      <el-checkbox-group v-model="selectedRoleIds">
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

const queryForm = reactive<UserQuery>({
  keyword: '',
  userType: undefined,
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
  userList.value = usersWithRoles
}

const loadRoleList = async () => {
  const res = await userApi.getRoleList()
  roleList.value = res.list
}

const loadUserList = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserList(queryForm)
    total.value = res.total
    await fillUserRoles(res.list)
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
  queryForm.userType = undefined
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
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.role-tag {
  font-weight: 500;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
