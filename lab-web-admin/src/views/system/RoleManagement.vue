<template>
  <div class="role-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="roleList" border stripe v-loading="loading" class="data-table">
        <el-table-column label="排序" width="130" align="center">
          <template #default="{ $index }">
            <div class="order-actions">
              <el-button
                text
                class="order-arrow-btn"
                :disabled="$index === 0"
                title="上移"
                @click="moveRoleUp($index)"
              >
                ↑
              </el-button>
              <el-button
                text
                class="order-arrow-btn"
                :disabled="$index === roleList.length - 1"
                title="下移"
                @click="moveRoleDown($index)"
              >
                ↓
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleAssignPermissions(row)">分配权限</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="roleForm" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" :disabled="!!roleForm.id" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
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

    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="720px">
      <el-alert
        title="权限按模块分区展示；模块下任一动作有权限，则前端显示模块。"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="permission-toolbar">
        <span>当前角色：{{ currentRoleCode || '-' }}</span>
        <el-button
          type="primary"
          plain
          size="small"
          @click="applyPermissionTemplate"
        >
          套用推荐模板
        </el-button>
      </div>
      <el-tree
        ref="treeRef"
        class="permission-tree"
        :data="permissionTree"
        :props="{ label: 'permissionName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { userApi } from '@/api/user'
import type { Permission, Role, RoleForm } from '@/types/user'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const treeRef = ref<InstanceType<typeof ElTree>>()
const roleList = ref<Role[]>([])
const permissionTree = ref<Permission[]>([])
const currentRoleId = ref<number>()
const currentRoleCode = ref('')
const ROLE_ORDER_STORAGE_KEY = 'lab-role-management-display-order'
const roleDisplayOrderMap = ref<Record<number, number>>({})

const normalizeRoleCode = (roleCode?: string) => (roleCode ?? '').trim().toUpperCase()

const isProtectedRole = (roleCode?: string) => {
  const normalized = normalizeRoleCode(roleCode)
  return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN'
}

const permissionTemplateMap: Record<string, string[]> = {
  ADMIN: [
    'system:user:list',
    'system:user:create',
    'system:user:update',
    'system:user:delete',
    'system:user:assign-role',
    'system:role:list',
    'system:role:create',
    'system:role:update',
    'system:role:delete',
    'system:role:assign-permission',
    'material:list',
    'material:create',
    'material:update',
    'material:delete',
    'inventory:stock:list',
    'inventory:stock-in:list',
    'inventory:stock-in:create',
    'inventory:stock-in:confirm',
    'inventory:stock-in:delete',
    'inventory:stock-out:list',
    'inventory:stock-out:create',
    'inventory:stock-out:confirm',
    'inventory:stock-out:delete',
    'inventory:stock-check:list',
    'inventory:stock-check:create',
    'inventory:stock-check:record',
    'inventory:stock-check:complete',
    'application:list',
    'application:approve',
    'hazardous:usage:list',
    'hazardous:ledger:view',
    'alert:list'
  ],
  CENTER_ADMIN: [
    'system:user:list',
    'system:user:create',
    'system:user:update',
    'system:user:delete',
    'system:user:assign-role',
    'material:list',
    'material:create',
    'material:update',
    'inventory:stock:list',
    'inventory:stock-in:list',
    'inventory:stock-in:create',
    'inventory:stock-in:confirm',
    'inventory:stock-out:list',
    'inventory:stock-out:create',
    'inventory:stock-out:confirm',
    'inventory:stock-check:list',
    'inventory:stock-check:create',
    'inventory:stock-check:record',
    'inventory:stock-check:complete',
    'application:list',
    'application:approve',
    'hazardous:usage:list',
    'hazardous:ledger:view',
    'alert:list'
  ],
  LAB_MANAGER: [
    'material:list',
    'material:create',
    'material:update',
    'inventory:stock:list',
    'inventory:stock-in:list',
    'inventory:stock-in:create',
    'inventory:stock-out:list',
    'inventory:stock-out:create',
    'inventory:stock-check:list',
    'inventory:stock-check:create',
    'inventory:stock-check:record',
    'application:list',
    'application:approve',
    'hazardous:usage:list',
    'hazardous:ledger:view',
    'alert:list'
  ],
  EQUIPMENT_ADMIN: [
    'material:list',
    'material:create',
    'material:update',
    'material:delete',
    'inventory:stock:list',
    'inventory:stock-in:list',
    'inventory:stock-in:create',
    'inventory:stock-in:confirm',
    'inventory:stock-out:list',
    'inventory:stock-out:create',
    'inventory:stock-out:confirm',
    'inventory:stock-check:list',
    'inventory:stock-check:create',
    'inventory:stock-check:record',
    'inventory:stock-check:complete',
    'application:list',
    'application:approve',
    'hazardous:usage:list',
    'hazardous:ledger:view',
    'alert:list'
  ],
  TEACHER: [
    'material:list',
    'inventory:stock:list',
    'application:list',
    'application:approve',
    'hazardous:usage:list',
    'hazardous:ledger:view'
  ],
  STUDENT: [
    'material:list',
    'inventory:stock:list',
    'application:list',
    'application:approve'
  ]
}

const roleForm = reactive<RoleForm>({
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const collectPermissionCodeIdMap = (nodes: Permission[], codeIdMap = new Map<string, number>()) => {
  nodes.forEach((node) => {
    if (node.permissionCode) {
      codeIdMap.set(node.permissionCode, node.id)
    }
    if (node.children && node.children.length > 0) {
      collectPermissionCodeIdMap(node.children, codeIdMap)
    }
  })
  return codeIdMap
}

const loadRoleDisplayOrder = () => {
  try {
    const raw = localStorage.getItem(ROLE_ORDER_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw) as Record<string, number>
    const normalized: Record<number, number> = {}
    Object.entries(parsed).forEach(([key, value]) => {
      const id = Number(key)
      if (Number.isFinite(id) && Number.isFinite(value)) {
        normalized[id] = value
      }
    })
    roleDisplayOrderMap.value = normalized
  } catch (error) {
    console.error('加载角色显示顺序失败:', error)
  }
}

const saveRoleDisplayOrder = () => {
  try {
    localStorage.setItem(ROLE_ORDER_STORAGE_KEY, JSON.stringify(roleDisplayOrderMap.value))
  } catch (error) {
    console.error('保存角色显示顺序失败:', error)
  }
}

const applyRoleDisplayOrder = (roles: Role[]) => {
  return [...roles].sort((left, right) => {
    const leftOrder = roleDisplayOrderMap.value[left.id]
    const rightOrder = roleDisplayOrderMap.value[right.id]
    const fallback = Number.MAX_SAFE_INTEGER
    const leftRank = Number.isFinite(leftOrder) ? leftOrder : fallback
    const rightRank = Number.isFinite(rightOrder) ? rightOrder : fallback
    if (leftRank !== rightRank) {
      return leftRank - rightRank
    }
    return left.id - right.id
  })
}

const syncRoleDisplayOrder = () => {
  const nextMap = { ...roleDisplayOrderMap.value }
  roleList.value.forEach((role, index) => {
    nextMap[role.id] = index + 1
  })
  roleDisplayOrderMap.value = nextMap
  saveRoleDisplayOrder()
}

const moveRoleUp = (index: number) => {
  if (index <= 0) return
  const list = [...roleList.value]
  ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
  roleList.value = list
  syncRoleDisplayOrder()
}

const moveRoleDown = (index: number) => {
  if (index >= roleList.value.length - 1) return
  const list = [...roleList.value]
  ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
  roleList.value = list
  syncRoleDisplayOrder()
}

const loadRoleList = async () => {
  loading.value = true
  try {
    const res = await userApi.getRoleList()
    const visibleRoles = res.list.filter(role => !isProtectedRole(role.roleCode))
    roleList.value = applyRoleDisplayOrder(visibleRoles)
  } catch (error) {
    console.error('加载角色列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadPermissionTree = async () => {
  try {
    permissionTree.value = await userApi.getPermissionTree()
  } catch (error) {
    console.error('加载权限树失败:', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增角色'
  Object.assign(roleForm, {
    id: undefined,
    roleName: '',
    roleCode: '',
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: Role) => {
  dialogTitle.value = '编辑角色'
  Object.assign(roleForm, {
    id: row.id,
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description,
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
      if (roleForm.id) {
        await userApi.updateRole(roleForm.id, roleForm)
        ElMessage.success('更新成功')
      } else {
        await userApi.createRole(roleForm)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadRoleList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: Role) => {
  if (isProtectedRole(row.roleCode)) {
    ElMessage.warning('系统管理员角色不允许删除')
    return
  }
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await userApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    await loadRoleList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const applyPermissionTemplate = () => {
  if (!currentRoleCode.value || !treeRef.value) {
    ElMessage.warning('请先选择角色')
    return
  }

  const templateCodes = permissionTemplateMap[currentRoleCode.value]
  if (!templateCodes || templateCodes.length === 0) {
    ElMessage.warning(`角色 ${currentRoleCode.value} 暂无预置模板`)
    return
  }

  const codeIdMap = collectPermissionCodeIdMap(permissionTree.value)
  const selectedIds: number[] = []
  const missingCodes: string[] = []

  templateCodes.forEach((code) => {
    const id = codeIdMap.get(code)
    if (id) {
      selectedIds.push(id)
    } else {
      missingCodes.push(code)
    }
  })

  treeRef.value.setCheckedKeys(selectedIds)

  if (missingCodes.length > 0) {
    ElMessage.warning(`模板已套用，部分权限未找到：${missingCodes.join(', ')}`)
    return
  }
  ElMessage.success(`已套用 ${currentRoleCode.value} 推荐模板`)
}

const handleAssignPermissions = async (row: Role) => {
  currentRoleId.value = row.id
  currentRoleCode.value = row.roleCode
  permissionDialogVisible.value = true
  await loadPermissionTree()

  if (treeRef.value) {
    treeRef.value.setCheckedKeys([])
    try {
      const permissionIds = await userApi.getRolePermissions(row.id)
      treeRef.value.setCheckedKeys(permissionIds)
    } catch (error) {
      console.error('加载角色已分配权限失败:', error)
    }
  }
}

const handlePermissionSubmit = async () => {
  if (!currentRoleId.value || !treeRef.value) return

  submitting.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys() as number[]
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys() as number[]
    const permissionIds = Array.from(new Set([...checkedKeys, ...halfCheckedKeys]))

    await userApi.assignPermissions(currentRoleId.value, permissionIds)
    ElMessage.success('分配权限成功')
    permissionDialogVisible.value = false
    await loadRoleList()
  } catch (error) {
    console.error('分配权限失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadRoleDisplayOrder()
  loadRoleList()
})
</script>

<style scoped>
.role-management {
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.data-table :deep(.el-table__row:hover > td) {
  background: #f7fbff !important;
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

.permission-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 12px 0 10px;
  padding: 10px 12px;
  border: 1px dashed #bfdbfe;
  border-radius: 10px;
  background: #f8fbff;
  color: #475569;
}

.permission-tree {
  max-height: 420px;
  overflow: auto;
  padding: 10px 8px;
  border: 1px solid #e6edf9;
  border-radius: 10px;
  background: #fff;
}

.permission-tree :deep(.el-tree-node__content) {
  border-radius: 8px;
}

.permission-tree :deep(.el-tree-node__content:hover) {
  background: #f3f8ff;
}
</style>
