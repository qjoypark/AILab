<template>
  <div class="approval-todo">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审批事项</span>
          <el-tag type="info" effect="plain">药品领用与实验室使用共用审批入口</el-tag>
        </div>
      </template>

      <el-table :data="todoList" border stripe v-loading="loading">
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.businessType === 'lab' ? 'success' : 'primary'">
              {{ row.businessType === 'lab' ? '实验室使用' : '药品领用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationNo" label="申请单号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="department" label="部门" width="150" show-overflow-tooltip />
        <el-table-column prop="summary" label="申请内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="当前可审批人" min-width="220">
          <template #default="{ row }">
            <el-space wrap v-if="row.currentApproverNames?.length">
              <el-tag
                v-for="(name, idx) in row.currentApproverNames"
                :key="`${row.businessType}-${row.id}-approver-${idx}`"
                type="info"
                effect="plain"
              >
                {{ name }}
              </el-tag>
            </el-space>
            <span v-else>{{ row.currentApproverName || (row.currentApproverId ? `用户#${row.currentApproverId}` : '-') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentApprovalLevel" label="审批级别" width="110" align="center">
          <template #default="{ row }">
            第 {{ row.currentApprovalLevel || 1 }} 级
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleApprove(row)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="approvalDialogVisible"
      title="审批处理"
      width="920px"
      @close="handleDialogClose"
    >
      <template v-if="currentTodo?.businessType === 'material'">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ currentMaterialApplication?.applicationCode }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentMaterialApplication?.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentMaterialApplication?.department }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(currentMaterialApplication?.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="用途" :span="2">{{ currentMaterialApplication?.applicationPurpose }}</el-descriptions-item>
          <el-descriptions-item label="使用地点">{{ currentMaterialApplication?.usageLocation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望使用日期">{{ currentMaterialApplication?.expectedDate || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>申请明细</el-divider>
        <el-table :data="currentMaterialApplication?.items ?? []" border>
          <el-table-column prop="materialCode" label="药品编码" width="120" />
          <el-table-column prop="materialName" label="药品名称" min-width="150" />
          <el-table-column prop="requestedQuantity" label="申请数量" width="100" align="right" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column label="批准数量" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.approvedQuantity"
                :min="0"
                :max="row.requestedQuantity"
                :precision="2"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column prop="usagePurpose" label="用途说明" min-width="150" />
        </el-table>
      </template>

      <template v-else-if="currentTodo?.businessType === 'lab'">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ currentLabApplication?.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="申请教师">{{ currentLabApplication?.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="实验室">
            {{ currentLabApplication ? formatLabRoomName(currentLabApplication) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(currentLabApplication?.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="使用时段" :span="2">
            {{ formatDateTime(currentLabApplication?.startTime) }} 至 {{ formatDateTime(currentLabApplication?.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="课程/项目">{{ currentLabApplication?.projectName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计人数">{{ currentLabApplication?.expectedAttendeeCount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="使用用途" :span="2">{{ currentLabApplication?.usagePurpose }}</el-descriptions-item>
          <el-descriptions-item label="特殊设备" :span="2">{{ currentLabApplication?.specialEquipment || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>共同使用教师</el-divider>
        <el-table :data="currentLabApplication?.participants ?? []" border>
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="realName" label="姓名" width="160" />
          <el-table-column prop="deptName" label="部门" />
        </el-table>
      </template>

      <el-divider>审批意见</el-divider>
      <el-form
        ref="formRef"
        :model="approvalForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="审批结果" prop="approvalResult">
          <el-radio-group v-model="approvalForm.approvalResult">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="approvalOpinion">
          <el-input
            v-model="approvalForm.approvalOpinion"
            type="textarea"
            :rows="3"
            placeholder="请填写审批意见"
          />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import { labApi } from '@/api/lab'
import { useUserStore } from '@/stores/user'
import type { ApprovalRequest, MaterialApplication } from '@/types/approval'
import type { LabUsageApplication } from '@/types/lab'

type TodoBusinessType = 'material' | 'lab'

interface ApprovalTodoItem {
  id: number
  businessType: TodoBusinessType
  applicationNo: string
  applicantName?: string
  department?: string
  summary?: string
  currentApprovalLevel?: number
  currentApproverId?: number
  currentApproverName?: string
  currentApproverNames?: string[]
  createdTime?: string
}

const loading = ref(false)
const submitting = ref(false)
const userStore = useUserStore()
const approvalDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const todoList = ref<ApprovalTodoItem[]>([])
const currentTodo = ref<ApprovalTodoItem>()
const currentMaterialApplication = ref<MaterialApplication>()
const currentLabApplication = ref<LabUsageApplication>()

const approvalForm = reactive<ApprovalRequest>({
  approvalResult: 1,
  approvalOpinion: '',
  itemApprovals: []
})

const rules: FormRules = {
  approvalResult: [{ required: true, message: '请选择审批结果', trigger: 'change' }]
}

const mapMaterialTodo = (item: MaterialApplication): ApprovalTodoItem => ({
  id: item.id,
  businessType: 'material',
  applicationNo: item.applicationCode,
  applicantName: item.applicantName,
  department: item.department,
  summary: item.applicationPurpose,
  currentApprovalLevel: item.currentApprovalLevel,
  currentApproverId: item.currentApproverId,
  currentApproverName: item.currentApproverName,
  currentApproverNames: item.currentApproverNames,
  createdTime: formatDateTime(item.createdTime)
})

const mapLabTodo = (item: LabUsageApplication): ApprovalTodoItem => ({
  id: item.id,
  businessType: 'lab',
  applicationNo: item.applicationNo,
  applicantName: item.applicantName,
  department: item.applicantDept,
  summary: `${item.labRoomName} / ${item.usagePurpose}`,
  currentApprovalLevel: resolveLabApprovalLevel(item),
  currentApproverId: item.currentApproverId,
  currentApproverName: item.currentApproverName,
  currentApproverNames: item.currentApproverNames,
  createdTime: formatDateTime(item.createdTime)
})

const resolveLabApprovalLevel = (item: LabUsageApplication) => {
  const roleLevelMap: Record<string, number> = {
    LAB_ROOM_MANAGER: 1,
    CENTER_DIRECTOR: 2,
    CENTER_ADMIN: 2,
    '003': 2,
    DEPUTY_DEAN: 3,
    '002': 3,
    DEAN: 4,
    '001': 4
  }
  return item.currentApproverRole ? roleLevelMap[item.currentApproverRole] ?? 1 : 1
}

const loadTodoList = async () => {
  loading.value = true
  try {
    const materialItems = userStore.hasAnyPermission(['application:approve'])
      ? (await approvalApi.getTodoList()).map(mapMaterialTodo)
      : []
    const labItems = userStore.hasAnyPermission(['lab-usage:approve'])
      ? (await labApi.getPendingLabUsageApplications()).map(mapLabTodo)
      : []
    todoList.value = [...materialItems, ...labItems].sort((a, b) =>
      String(b.createdTime ?? '').localeCompare(String(a.createdTime ?? ''))
    )
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row: ApprovalTodoItem) => {
  currentTodo.value = row
  approvalForm.approvalResult = 1
  approvalForm.approvalOpinion = ''
  approvalForm.itemApprovals = []

  if (row.businessType === 'material') {
    currentMaterialApplication.value = await approvalApi.getApplicationById(row.id)
    currentLabApplication.value = undefined
    currentMaterialApplication.value.items?.forEach(item => {
      if (!item.approvedQuantity) {
        item.approvedQuantity = item.requestedQuantity
      }
    })
  } else {
    currentLabApplication.value = await labApi.getLabUsageApplicationById(row.id)
    currentMaterialApplication.value = undefined
  }

  approvalDialogVisible.value = true
}

const handleSubmitApproval = async () => {
  if (!formRef.value || !currentTodo.value) return

  const valid = await formRef.value.validate()
  if (!valid || !currentTodo.value) return

  submitting.value = true
  try {
    if (currentTodo.value.businessType === 'material' && currentMaterialApplication.value) {
      approvalForm.itemApprovals = currentMaterialApplication.value.items?.map(item => ({
        itemId: item.id!,
        approvedQuantity: item.approvedQuantity || 0
      })) || []
      await approvalApi.approveApplication(currentMaterialApplication.value.id, approvalForm)
    } else if (currentTodo.value.businessType === 'lab' && currentLabApplication.value) {
      await labApi.approveLabUsageApplication(currentLabApplication.value.id, {
        approvalResult: approvalForm.approvalResult,
        approvalOpinion: approvalForm.approvalOpinion
      })
    }

    ElMessage.success('审批提交成功')
    approvalDialogVisible.value = false
    loadTodoList()
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  currentTodo.value = undefined
  currentMaterialApplication.value = undefined
  currentLabApplication.value = undefined
}

const formatLabRoomName = (item: LabUsageApplication) => `${item.labRoomName}（${item.labRoomCode}）`

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadTodoList()
})
</script>

<style scoped>
.approval-todo {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
