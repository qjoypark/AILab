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

      <!-- 角色列表 -->
      <el-table :data="roleList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
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

    <!-- 角色表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="roleForm"
        :rules="rules"
        label-width="100px"
      >
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

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="600px">
      <el-tree
        ref="treeRef"
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { userApi } from '@/api/user'
import type { Role, RoleForm, Permission } from '@/types/user'

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

const loadRoleList = async () => {
  loading.value = true
  try {
    const res = await userApi.getRoleList()
    roleList.value = res.list
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
      loadRoleList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: Role) => {
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await userApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoleList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleAssignPermissions = async (row: Role) => {
  currentRoleId.value = row.id
  permissionDialogVisible.value = true
  
  // 设置已选中的权限
  await loadPermissionTree()
  if (row.permissions && treeRef.value) {
    const permissionIds = row.permissions.map(p => p.id)
    treeRef.value.setCheckedKeys(permissionIds)
  }
}

const handlePermissionSubmit = async () => {
  if (!currentRoleId.value || !treeRef.value) return
  
  submitting.value = true
  try {
    const checkedKeys = treeRef.value.getCheckedKeys() as number[]
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys() as number[]
    const permissionIds = [...checkedKeys, ...halfCheckedKeys]
    
    await userApi.assignPermissions(currentRoleId.value, permissionIds)
    ElMessage.success('分配权限成功')
    permissionDialogVisible.value = false
    loadRoleList()
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
  loadRoleList()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
