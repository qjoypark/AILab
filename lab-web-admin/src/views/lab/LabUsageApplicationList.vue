<template>
  <div class="lab-usage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实验室使用申请</span>
          <el-button v-if="canCreate" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建申请
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="申请单号/实验室/用途" clearable />
        </el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="queryForm.labRoomId" placeholder="全部" clearable filterable style="width: 220px">
            <el-option
              v-for="room in labRoomOptions"
              :key="room.id"
              :label="formatRoomOption(room)"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="审批中" :value="2" />
            <el-option label="审批通过" :value="3" />
            <el-option label="审批拒绝" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用时段">
          <el-date-picker
            v-model="usageDateRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="applicationList" border stripe v-loading="loading">
        <el-table-column prop="applicationNo" label="申请单号" width="170" />
        <el-table-column prop="labRoomName" label="实验室" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatRoomName(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请教师" width="120" />
        <el-table-column prop="usagePurpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="使用时段" min-width="280">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="当前审批" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-space wrap v-if="row.currentApproverNames?.length">
              <el-tag v-for="name in row.currentApproverNames" :key="`${row.id}-${name}`" type="info" effect="plain">
                {{ name }}
              </el-tag>
            </el-space>
            <span v-else>{{ row.currentPendingStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-button v-if="canApprove(row)" link type="success" @click="handleApprove(row)">审批</el-button>
            <el-button v-if="canCancel(row)" link type="warning" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadApplications"
        @current-change="loadApplications"
      />
    </el-card>

    <el-dialog v-model="formDialogVisible" title="新建实验室使用申请" width="920px" @close="handleFormDialogClose">
      <el-form ref="formRef" :model="applicationForm" :rules="formRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实验室" prop="labRoomId">
              <el-select v-model="applicationForm.labRoomId" placeholder="请选择实验室" filterable style="width: 100%">
                <el-option
                  v-for="room in labRoomOptions"
                  :key="room.id"
                  :label="formatRoomOption(room)"
                  :value="room.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用类型">
              <el-select v-model="applicationForm.usageType" style="width: 100%">
                <el-option label="教学" :value="1" />
                <el-option label="科研" :value="2" />
                <el-option label="竞赛" :value="3" />
                <el-option label="培训" :value="4" />
                <el-option label="其他" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="使用时段">
          <el-date-picker
            v-model="formTimeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-alert
          v-if="overlapApplications.length > 0"
          type="warning"
          show-icon
          :closable="false"
          class="overlap-alert"
          title="该实验室在所选时段已有其他审批中或已通过的使用申请。系统允许共享使用，请确认现场协调安排。"
        />
        <el-table
          v-if="overlapApplications.length > 0"
          :data="overlapApplications"
          border
          size="small"
          v-loading="overlapLoading"
          class="overlap-table"
        >
          <el-table-column prop="applicationNo" label="已有申请单" width="160" />
          <el-table-column prop="applicantName" label="申请教师" width="120" />
          <el-table-column label="已有时段" min-width="240">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="usagePurpose" label="用途" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ statusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程/项目">
              <el-input v-model="applicationForm.projectName" placeholder="课程名、课题名，可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计人数">
              <el-input-number v-model="applicationForm.expectedAttendeeCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="使用用途" prop="usagePurpose">
          <el-input v-model="applicationForm.usagePurpose" type="textarea" :rows="3" placeholder="请说明使用目的、课程或实验内容" />
        </el-form-item>
        <el-form-item label="特殊设备需求">
          <el-input v-model="applicationForm.specialEquipment" type="textarea" :rows="2" placeholder="如需特殊设备、耗材或安全支持，请在这里说明" />
        </el-form-item>
        <el-form-item label="安全承诺">
          <el-checkbox v-model="safetyConfirmed">我已阅读实验室安全要求，并承诺按规范使用实验室</el-checkbox>
        </el-form-item>

        <el-divider>共同使用教师</el-divider>
        <el-alert
          title="申请人会自动计入使用教师；如需添加其他共同使用教师，请从系统人员名单中选择。"
          type="info"
          show-icon
          :closable="false"
          class="participant-alert"
        />
        <div class="participant-selector">
          <el-select
            v-model="selectedParticipantIds"
            multiple
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="searchParticipantUsers"
            :loading="participantUserLoading"
            placeholder="搜索教师姓名、用户名或部门"
            style="width: 100%"
          >
            <el-option
              v-for="user in participantUserOptions"
              :key="user.id"
              :label="formatUserOption(user)"
              :value="user.id"
            />
          </el-select>
          <el-button type="primary" @click="handleAddSelectedParticipants">
            <el-icon><Plus /></el-icon>
            添加
          </el-button>
        </div>
        <el-table :data="participantRows" border>
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="realName" label="姓名" min-width="160" />
          <el-table-column prop="deptName" label="部门" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="90">
            <template #default="{ $index }">
              <el-button link type="danger" @click="handleRemoveParticipant($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="备注" class="remark-item">
          <el-input v-model="applicationForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="实验室使用申请详情" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ currentApplication?.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="申请教师">{{ currentApplication?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="实验室">{{ currentApplication ? formatRoomName(currentApplication) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(currentApplication?.status)">{{ statusName(currentApplication?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="使用时段" :span="2">
          {{ formatDateTime(currentApplication?.startTime) }} 至 {{ formatDateTime(currentApplication?.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="课程/项目">{{ currentApplication?.projectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计人数">{{ currentApplication?.expectedAttendeeCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用用途" :span="2">{{ currentApplication?.usagePurpose }}</el-descriptions-item>
        <el-descriptions-item label="特殊设备" :span="2">{{ currentApplication?.specialEquipment || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>共同使用教师</el-divider>
      <el-table :data="currentApplication?.participants ?? []" border>
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="realName" label="姓名" width="160" />
        <el-table-column prop="deptName" label="部门" />
      </el-table>

      <el-divider>审批记录</el-divider>
      <el-timeline v-if="currentApplication?.approvalRecords?.length">
        <el-timeline-item
          v-for="record in currentApplication.approvalRecords"
          :key="record.id"
          :timestamp="formatDateTime(record.approvalTime)"
          placement="top"
        >
          <el-card>
            <p><strong>审批人：</strong>{{ record.approverName }}</p>
            <p>
              <strong>审批结果：</strong>
              <el-tag v-if="record.approvalResult === 1" type="success">通过</el-tag>
              <el-tag v-else type="danger">拒绝</el-tag>
            </p>
            <p v-if="record.approvalOpinion"><strong>审批意见：</strong>{{ record.approvalOpinion }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" />
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentApplication && canExportPdf(currentApplication)"
          type="primary"
          :loading="pdfGenerating"
          @click="handleGeneratePdf"
        >
          <el-icon><Printer /></el-icon>
          生成电子申请单PDF
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="approvalDialogVisible" title="实验室使用审批" width="620px" @close="handleApprovalDialogClose">
      <el-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" label-width="100px">
        <el-form-item label="审批结果" prop="approvalResult">
          <el-radio-group v-model="approvalForm.approvalResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.approvalOpinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApproval" :loading="submitting">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Printer } from '@element-plus/icons-vue'
import { labApi } from '@/api/lab'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type {
  LabRoom,
  LabUsageApplication,
  LabUsageApplicationForm,
  LabUsageApplicationQuery,
  LabUsageParticipant
} from '@/types/lab'
import type { User } from '@/types/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const overlapLoading = ref(false)
const pdfGenerating = ref(false)
const total = ref(0)
const applicationList = ref<LabUsageApplication[]>([])
const overlapApplications = ref<LabUsageApplication[]>([])
const labRoomOptions = ref<LabRoom[]>([])
const usageDateRange = ref<string[]>([])
const formTimeRange = ref<string[]>([])
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const approvalDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const approvalFormRef = ref<FormInstance>()
const currentApplication = ref<LabUsageApplication>()
const participantRows = ref<LabUsageParticipant[]>([])
const selectedParticipantIds = ref<number[]>([])
const participantUserOptions = ref<User[]>([])
const participantUserLoading = ref(false)
const safetyConfirmed = ref(false)

const canCreate = computed(() => userStore.hasAnyPermission(['lab-usage:create']))
const canApprovePermission = computed(() => userStore.hasAnyPermission(['lab-usage:approve']))
const currentUserId = computed(() => Number(userStore.userInfo?.id ?? 0))

const queryForm = reactive<LabUsageApplicationQuery>({
  page: 1,
  size: 10,
  status: undefined,
  labRoomId: undefined,
  keyword: '',
  startTime: undefined,
  endTime: undefined
})

const applicationForm = reactive<LabUsageApplicationForm>({
  labRoomId: undefined,
  usageType: 1,
  usagePurpose: '',
  projectName: '',
  expectedAttendeeCount: undefined,
  startTime: undefined,
  endTime: undefined,
  specialEquipment: '',
  safetyCommitment: 0,
  remark: '',
  participants: []
})

const approvalForm = reactive({
  approvalResult: 1,
  approvalOpinion: ''
})

const formRules: FormRules = {
  labRoomId: [{ required: true, message: '请选择实验室', trigger: 'change' }],
  usagePurpose: [{ required: true, message: '请填写使用用途', trigger: 'blur' }]
}

const approvalRules: FormRules = {
  approvalResult: [{ required: true, message: '请选择审批结果', trigger: 'change' }]
}

watch(
  () => [applicationForm.labRoomId, formTimeRange.value?.[0], formTimeRange.value?.[1]],
  () => {
    loadOverlapApplications()
  }
)

const loadLabRooms = async () => {
  const result = await labApi.getLabRoomPage({ page: 1, size: 200, status: 1 })
  labRoomOptions.value = result.list
}

const loadApplications = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = usageDateRange.value ?? []
    queryForm.startTime = startTime
    queryForm.endTime = endTime
    const result = await labApi.getLabUsageApplicationPage(queryForm)
    applicationList.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

const loadOverlapApplications = async () => {
  const [startTime, endTime] = formTimeRange.value ?? []
  if (!applicationForm.labRoomId || !startTime || !endTime) {
    overlapApplications.value = []
    return
  }

  overlapLoading.value = true
  try {
    overlapApplications.value = await labApi.getLabUsageOverlaps({
      labRoomId: applicationForm.labRoomId,
      startTime,
      endTime
    })
  } finally {
    overlapLoading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadApplications()
}

const handleReset = () => {
  queryForm.page = 1
  queryForm.status = undefined
  queryForm.labRoomId = undefined
  queryForm.keyword = ''
  usageDateRange.value = []
  loadApplications()
}

const handleAdd = async () => {
  resetApplicationForm()
  participantUserOptions.value = []
  await searchParticipantUsers('')
  formDialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  const [startTime, endTime] = formTimeRange.value ?? []
  if (!startTime || !endTime) {
    ElMessage.warning('请选择使用时段')
    return
  }
  if (!safetyConfirmed.value) {
    ElMessage.warning('请先确认安全承诺')
    return
  }

  submitting.value = true
  try {
    await labApi.createLabUsageApplication({
      ...applicationForm,
      startTime,
      endTime,
      safetyCommitment: 1,
      participants: participantRows.value.map(item => ({
        userId: item.userId,
        realName: item.realName,
        deptName: item.deptName
      }))
    })
    ElMessage.success('实验室使用申请已提交')
    formDialogVisible.value = false
    loadApplications()
  } finally {
    submitting.value = false
  }
}

const handleView = async (row: LabUsageApplication) => {
  currentApplication.value = await labApi.getLabUsageApplicationById(row.id)
  detailDialogVisible.value = true
}

const handleGeneratePdf = async () => {
  if (!currentApplication.value?.id) {
    ElMessage.warning('未找到实验室使用申请信息')
    return
  }

  pdfGenerating.value = true
  try {
    const blob = await labApi.exportLabUsageApplicationPdf(currentApplication.value.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    const code = currentApplication.value.applicationNo || String(currentApplication.value.id)
    link.href = url
    link.download = `实验室使用申请单_${code}.pdf`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('电子申请单生成成功')
  } catch (error) {
    console.error('生成实验室使用申请单PDF失败:', error)
  } finally {
    pdfGenerating.value = false
  }
}

const handleApprove = async (row: LabUsageApplication) => {
  currentApplication.value = await labApi.getLabUsageApplicationById(row.id)
  approvalForm.approvalResult = 1
  approvalForm.approvalOpinion = ''
  approvalDialogVisible.value = true
}

const handleSubmitApproval = async () => {
  if (!approvalFormRef.value || !currentApplication.value) return
  const valid = await approvalFormRef.value.validate()
  if (!valid || !currentApplication.value) return

  submitting.value = true
  try {
    await labApi.approveLabUsageApplication(currentApplication.value.id, approvalForm)
    ElMessage.success('审批已提交')
    approvalDialogVisible.value = false
    loadApplications()
  } finally {
    submitting.value = false
  }
}

const handleCancel = async (row: LabUsageApplication) => {
  try {
    await ElMessageBox.confirm('确认取消这条实验室使用申请吗？', '提示', { type: 'warning' })
    await labApi.cancelLabUsageApplication(row.id)
    ElMessage.success('已取消')
    loadApplications()
  } catch {
    // 用户取消操作时不提示。
  }
}

const canApprove = (row: LabUsageApplication) => {
  if (!canApprovePermission.value || row.status !== 2) return false
  return userStore.isAdmin() || (row.currentApproverIds ?? []).includes(currentUserId.value)
}

const canCancel = (row: LabUsageApplication) => {
  return [1, 2].includes(row.status) && (userStore.isAdmin() || row.applicantId === currentUserId.value)
}

const canExportPdf = (row: LabUsageApplication) => {
  return row.status === 3 || row.approvalStatus === 2
}

const searchParticipantUsers = async (keyword: string) => {
  participantUserLoading.value = true
  try {
    const result = await userApi.getSelectableUsers({
      keyword: keyword?.trim() || '',
      page: 1,
      size: 50,
      userType: 2,
      status: 1
    })
    const merged = new Map<number, User>()
    ;[...participantsToUsers(participantRows.value), ...result.list].forEach(user => merged.set(user.id, user))
    participantUserOptions.value = Array.from(merged.values())
  } finally {
    participantUserLoading.value = false
  }
}

const handleAddSelectedParticipants = () => {
  if (selectedParticipantIds.value.length === 0) {
    ElMessage.warning('请先从系统用户名单中选择共同使用教师')
    return
  }

  const existingIds = new Set([
    currentUserId.value,
    ...participantRows.value.map(participant => participant.userId)
  ])
  const userMap = new Map(participantUserOptions.value.map(user => [user.id, user]))
  selectedParticipantIds.value.forEach(userId => {
    if (existingIds.has(userId)) return

    const user = userMap.get(userId)
    if (!user) return

    participantRows.value.push({
      userId: user.id,
      realName: user.realName || user.username,
      deptName: user.department,
      createdTime: undefined
    })
    existingIds.add(userId)
  })
  selectedParticipantIds.value = []
}

const handleRemoveParticipant = (index: number) => {
  participantRows.value.splice(index, 1)
}

const handleFormDialogClose = () => {
  formRef.value?.resetFields()
}

const handleApprovalDialogClose = () => {
  approvalFormRef.value?.resetFields()
}

const resetApplicationForm = () => {
  applicationForm.labRoomId = undefined
  applicationForm.usageType = 1
  applicationForm.usagePurpose = ''
  applicationForm.projectName = ''
  applicationForm.expectedAttendeeCount = undefined
  applicationForm.startTime = undefined
  applicationForm.endTime = undefined
  applicationForm.specialEquipment = ''
  applicationForm.safetyCommitment = 0
  applicationForm.remark = ''
  applicationForm.participants = []
  participantRows.value = []
  selectedParticipantIds.value = []
  overlapApplications.value = []
  formTimeRange.value = []
  safetyConfirmed.value = false
}

const participantsToUsers = (participants: LabUsageParticipant[]): User[] => {
  return participants.map(participant => ({
    id: participant.userId,
    username: `user${participant.userId}`,
    realName: participant.realName,
    userType: 2,
    department: participant.deptName,
    status: 1
  }))
}

const formatUserOption = (user: User) => {
  const dept = user.department ? ` / ${user.department}` : ''
  return `${user.realName || user.username}（${user.username}）${dept}`
}

const formatRoomOption = (room: LabRoom) => `${room.roomName}（${room.roomCode}）`

const formatRoomName = (row: Pick<LabUsageApplication, 'labRoomName' | 'labRoomCode'>) => {
  return `${row.labRoomName}（${row.labRoomCode}）`
}

const statusName = (status?: number) => {
  const map: Record<number, string> = {
    1: '待审批',
    2: '审批中',
    3: '审批通过',
    4: '审批拒绝',
    5: '已取消'
  }
  return status ? map[status] ?? '未知' : '-'
}

const statusTag = (status?: number) => {
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'warning',
    2: 'warning',
    3: 'success',
    4: 'danger',
    5: 'info'
  }
  return status ? map[status] ?? 'info' : 'info'
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  await loadLabRooms()
  await loadApplications()
})
</script>

<style scoped>
.lab-usage-page {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overlap-alert,
.overlap-table,
.participant-alert {
  margin-bottom: 12px;
}

.participant-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.remark-item {
  margin-top: 18px;
}

.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

</style>
