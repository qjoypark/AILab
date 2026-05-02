<template>
  <div class="lab-room-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验室管理</span>
          <el-button v-if="canEditLabRoom" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增实验室
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="编号/名称/楼宇" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.roomType" placeholder="全部" clearable style="width: 140px">
            <el-option label="教学" :value="1" />
            <el-option label="科研" :value="2" />
            <el-option label="公共平台" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="canEditLabRoom" label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="停用" :value="0" />
            <el-option label="启用" :value="1" />
            <el-option label="维护中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="labRoomList" border stripe v-loading="loading">
        <el-table-column prop="roomCode" label="实验室编号" width="140" />
        <el-table-column prop="roomName" label="实验室名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="building" label="楼宇" width="140" show-overflow-tooltip />
        <el-table-column prop="roomNo" label="房间号" width="110" />
        <el-table-column prop="capacity" label="容量" width="90" />
        <el-table-column prop="roomType" label="类型" width="110">
          <template #default="{ row }">
            <el-tag>{{ roomTypeName(row.roomType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="safetyLevel" label="安全等级" width="110">
          <template #default="{ row }">
            <el-tag :type="safetyLevelTag(row.safetyLevel)">{{ safetyLevelName(row.safetyLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理人员" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ managerNames(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-if="canEditLabRoom" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="canEditManagers" link type="primary" @click="handleManagers(row)">管理人员</el-button>
            <el-button v-if="canEditLabRoom" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLabRooms"
        @current-change="loadLabRooms"
      />
    </el-card>

    <el-dialog v-model="roomDialogVisible" :title="roomDialogTitle" width="760px" @close="handleRoomDialogClose">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实验室编号" prop="roomCode">
              <el-input v-model="roomForm.roomCode" placeholder="如 A101" :disabled="roomDialogMode === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实验室名称" prop="roomName">
              <el-input v-model="roomForm.roomName" :disabled="roomDialogMode === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="楼宇">
              <el-input v-model="roomForm.building" :disabled="roomDialogMode === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="楼层">
              <el-input v-model="roomForm.floor" :disabled="roomDialogMode === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房间号">
              <el-input v-model="roomForm.roomNo" :disabled="roomDialogMode === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="容量">
              <el-input-number v-model="roomForm.capacity" :min="0" :disabled="roomDialogMode === 'view'" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="roomForm.roomType" :disabled="roomDialogMode === 'view'" style="width: 100%">
                <el-option label="教学" :value="1" />
                <el-option label="科研" :value="2" />
                <el-option label="公共平台" :value="3" />
                <el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="roomForm.status" :disabled="roomDialogMode === 'view'" style="width: 100%">
                <el-option label="停用" :value="0" />
                <el-option label="启用" :value="1" />
                <el-option label="维护中" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="安全等级">
          <el-select v-model="roomForm.safetyLevel" :disabled="roomDialogMode === 'view'" style="width: 100%">
            <el-option label="普通" :value="1" />
            <el-option label="重点" :value="2" />
            <el-option label="高风险" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="主要设备">
          <el-input v-model="roomForm.equipmentSummary" type="textarea" :rows="3" :disabled="roomDialogMode === 'view'" />
        </el-form-item>
        <el-form-item label="注意事项">
          <el-input v-model="roomForm.notice" type="textarea" :rows="3" :disabled="roomDialogMode === 'view'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="roomDialogVisible = false">{{ roomDialogMode === 'view' ? '关闭' : '取消' }}</el-button>
        <el-button v-if="roomDialogMode !== 'view'" type="primary" @click="handleRoomSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="managerDialogVisible" title="配置实验室管理人员" width="760px">
      <div class="manager-toolbar">
        <span>{{ currentRoom?.roomName }}</span>
      </div>

      <el-alert
        title="管理人员必须从系统用户名单中选择；保存时以后端用户表为准，不再允许手工填写姓名。"
        type="info"
        show-icon
        :closable="false"
        class="manager-tip"
      />

      <div class="manager-selector">
        <el-select
          v-model="selectedManagerIds"
          multiple
          filterable
          remote
          reserve-keyword
          clearable
          :remote-method="searchManagerUsers"
          :loading="managerUserLoading"
          placeholder="搜索用户姓名、用户名或部门"
          style="width: 100%"
        >
          <el-option
            v-for="user in managerUserOptions"
            :key="user.id"
            :label="formatUserOption(user)"
            :value="user.id"
          />
        </el-select>
        <el-button type="primary" @click="handleAddSelectedManagers">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>

      <el-radio-group v-model="primaryManagerId" class="primary-radio-group">
        <el-table :data="managerRows" border>
          <el-table-column prop="managerId" label="用户ID" width="120" />
          <el-table-column prop="managerName" label="姓名" min-width="160" />
          <el-table-column label="主负责人" width="120">
            <template #default="{ row }">
              <el-radio :value="row.managerId">是</el-radio>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ $index }">
              <el-button link type="danger" @click="handleRemoveManager($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-radio-group>

      <template #footer>
        <el-button @click="managerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleManagerSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { labApi } from '@/api/lab'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { LabRoom, LabRoomForm, LabRoomManager, LabRoomQuery } from '@/types/lab'
import type { User } from '@/types/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const labRoomList = ref<LabRoom[]>([])
const roomFormRef = ref<FormInstance>()
const roomDialogVisible = ref(false)
const managerDialogVisible = ref(false)
const roomDialogMode = ref<'add' | 'edit' | 'view'>('add')
const currentRoom = ref<LabRoom>()
const managerRows = ref<LabRoomManager[]>([])
const primaryManagerId = ref<number>()
const selectedManagerIds = ref<number[]>([])
const managerUserOptions = ref<User[]>([])
const managerUserLoading = ref(false)

const canEditLabRoom = computed(() => userStore.hasAnyPermission(['lab-room:create', 'lab-room:update', 'lab-room:delete']))
const canEditManagers = computed(() => userStore.hasAnyPermission(['lab-room:manager:update']))

const queryForm = reactive<LabRoomQuery>({
  page: 1,
  size: 10,
  status: undefined,
  roomType: undefined,
  keyword: ''
})

const roomForm = reactive<LabRoomForm>({
  roomCode: '',
  roomName: '',
  building: '',
  floor: '',
  roomNo: '',
  capacity: undefined,
  roomType: 1,
  safetyLevel: 1,
  equipmentSummary: '',
  notice: '',
  status: 1
})

const roomRules: FormRules = {
  roomCode: [{ required: true, message: '请输入实验室编号', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }]
}

const roomDialogTitle = computed(() => {
  if (roomDialogMode.value === 'view') return '实验室详情'
  return roomDialogMode.value === 'edit' ? '编辑实验室' : '新增实验室'
})

const loadLabRooms = async () => {
  loading.value = true
  try {
    const result = await labApi.getLabRoomPage(queryForm)
    labRoomList.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadLabRooms()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  queryForm.roomType = undefined
  handleSearch()
}

const resetRoomForm = () => {
  Object.assign(roomForm, {
    id: undefined,
    roomCode: '',
    roomName: '',
    building: '',
    floor: '',
    roomNo: '',
    capacity: undefined,
    roomType: 1,
    safetyLevel: 1,
    equipmentSummary: '',
    notice: '',
    status: 1
  })
}

const assignRoomForm = (row: LabRoom) => {
  Object.assign(roomForm, {
    id: row.id,
    roomCode: row.roomCode,
    roomName: row.roomName,
    building: row.building ?? '',
    floor: row.floor ?? '',
    roomNo: row.roomNo ?? '',
    capacity: row.capacity,
    roomType: row.roomType ?? 1,
    safetyLevel: row.safetyLevel ?? 1,
    equipmentSummary: row.equipmentSummary ?? '',
    notice: row.notice ?? '',
    status: row.status ?? 1
  })
}

const handleAdd = () => {
  roomDialogMode.value = 'add'
  resetRoomForm()
  roomDialogVisible.value = true
}

const handleEdit = (row: LabRoom) => {
  roomDialogMode.value = 'edit'
  assignRoomForm(row)
  roomDialogVisible.value = true
}

const handleView = (row: LabRoom) => {
  roomDialogMode.value = 'view'
  assignRoomForm(row)
  roomDialogVisible.value = true
}

const handleRoomSubmit = async () => {
  if (!roomFormRef.value) return
  const valid = await roomFormRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (roomForm.id) {
      await labApi.updateLabRoom(roomForm.id, roomForm)
      ElMessage.success('实验室已更新')
    } else {
      await labApi.createLabRoom(roomForm)
      ElMessage.success('实验室已创建')
    }
    roomDialogVisible.value = false
    loadLabRooms()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: LabRoom) => {
  try {
    await ElMessageBox.confirm(`确定删除实验室“${row.roomName}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await labApi.deleteLabRoom(row.id)
    ElMessage.success('实验室已删除')
    loadLabRooms()
  } catch {
    // 用户取消删除时不需要提示。
  }
}

const handleManagers = async (row: LabRoom) => {
  currentRoom.value = row
  const managers = await labApi.getLabRoomManagers(row.id)
  managerRows.value = managers.map(manager => ({ ...manager }))
  const primaryManager = managerRows.value.find(manager => manager.isPrimary === 1)
  primaryManagerId.value = primaryManager?.managerId ?? managerRows.value[0]?.managerId
  selectedManagerIds.value = []
  managerUserOptions.value = managersToUsers(managerRows.value)
  await searchManagerUsers('')
  managerDialogVisible.value = true
}

const searchManagerUsers = async (keyword: string) => {
  managerUserLoading.value = true
  try {
    const result = await userApi.getSelectableUsers({
      keyword: keyword?.trim() || '',
      page: 1,
      size: 50,
      status: 1
    })
    const merged = new Map<number, User>()
    ;[...managersToUsers(managerRows.value), ...result.list].forEach(user => merged.set(user.id, user))
    managerUserOptions.value = Array.from(merged.values())
  } finally {
    managerUserLoading.value = false
  }
}

const managersToUsers = (managers: LabRoomManager[]): User[] => {
  return managers.map(manager => ({
    id: manager.managerId,
    username: `user${manager.managerId}`,
    realName: manager.managerName,
    userType: 2,
    department: '',
    status: 1
  }))
}

const formatUserOption = (user: User) => {
  const dept = user.department ? ` / ${user.department}` : ''
  return `${user.realName || user.username}（${user.username}）${dept}`
}

const handleAddSelectedManagers = () => {
  if (selectedManagerIds.value.length === 0) {
    ElMessage.warning('请先从系统用户名单中选择管理人员')
    return
  }

  const existingIds = new Set(managerRows.value.map(manager => manager.managerId))
  const userMap = new Map(managerUserOptions.value.map(user => [user.id, user]))
  selectedManagerIds.value.forEach(userId => {
    if (existingIds.has(userId)) return

    const user = userMap.get(userId)
    if (!user) return

    managerRows.value.push({
      managerId: user.id,
      managerName: user.realName || user.username,
      isPrimary: managerRows.value.length === 0 ? 1 : 0,
      status: 1
    })
    existingIds.add(userId)
  })

  if (!primaryManagerId.value && managerRows.value.length > 0) {
    primaryManagerId.value = managerRows.value[0].managerId
  }
  selectedManagerIds.value = []
}

const handleRemoveManager = (index: number) => {
  const removed = managerRows.value[index]
  managerRows.value.splice(index, 1)
  if (removed?.managerId === primaryManagerId.value) {
    primaryManagerId.value = managerRows.value[0]?.managerId
  }
}

const handleManagerSubmit = async () => {
  if (!currentRoom.value) return
  if (managerRows.value.length === 0) {
    ElMessage.warning('请至少选择一名实验室管理人员')
    return
  }
  if (!primaryManagerId.value) {
    ElMessage.warning('请选择一名主负责人')
    return
  }

  submitting.value = true
  try {
    const payload = managerRows.value.map(row => ({
      managerId: row.managerId,
      managerName: row.managerName,
      isPrimary: row.managerId === primaryManagerId.value ? 1 : 0,
      status: 1
    }))
    await labApi.saveLabRoomManagers(currentRoom.value.id, payload)
    ElMessage.success('管理人员已保存')
    managerDialogVisible.value = false
    loadLabRooms()
  } finally {
    submitting.value = false
  }
}

const handleRoomDialogClose = () => {
  roomFormRef.value?.resetFields()
}

const roomTypeName = (value?: number) => {
  const names: Record<number, string> = { 1: '教学', 2: '科研', 3: '公共平台', 4: '其他' }
  return names[value ?? 1] ?? '其他'
}

const safetyLevelName = (value?: number) => {
  const names: Record<number, string> = { 1: '普通', 2: '重点', 3: '高风险' }
  return names[value ?? 1] ?? '普通'
}

const safetyLevelTag = (value?: number) => {
  if (value === 3) return 'danger'
  if (value === 2) return 'warning'
  return 'success'
}

const statusName = (value?: number) => {
  const names: Record<number, string> = { 0: '停用', 1: '启用', 2: '维护中' }
  return names[value ?? 1] ?? '启用'
}

const statusTag = (value?: number) => {
  if (value === 0) return 'info'
  if (value === 2) return 'warning'
  return 'success'
}

const managerNames = (row: LabRoom) => {
  const managers = row.managers ?? []
  if (managers.length === 0) {
    return '-'
  }
  return managers
    .map(manager => manager.isPrimary === 1 ? `${manager.managerName}（主）` : manager.managerName)
    .join('、')
}

onMounted(() => {
  loadLabRooms()
})
</script>

<style scoped>
.lab-room-list {
  padding: 20px;
}

.card-header,
.manager-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.manager-toolbar {
  margin-bottom: 16px;
}

.manager-tip {
  margin-bottom: 12px;
}

.manager-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.primary-radio-group {
  display: block;
  width: 100%;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
