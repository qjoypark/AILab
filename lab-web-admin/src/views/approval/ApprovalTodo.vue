<template>
  <div class="approval-todo">
    <el-card>
      <template #header>
        <span>待审批事项</span>
      </template>

      <el-table :data="todoList" border stripe v-loading="loading">
        <el-table-column prop="applicationCode" label="申请单号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="department" label="部门" width="140" />
        <el-table-column prop="applicationPurpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column prop="currentApproverName" label="当前可审批人" min-width="220">
          <template #default="{ row }">
            <el-space wrap v-if="row.currentApproverNames?.length">
              <el-tag
                v-for="(name, idx) in row.currentApproverNames"
                :key="`${row.id}-todo-approver-${idx}`"
                type="info"
                effect="plain"
              >
                {{ name }}
              </el-tag>
            </el-space>
            <span v-else>{{ row.currentApproverName || (row.currentApproverId ? `用户#${row.currentApproverId}` : '-') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentApprovalLevel" label="当前审批级别" width="120" align="center">
          <template #default="{ row }">
            第 {{ row.currentApprovalLevel }} 级
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
      width="900px"
      @close="handleDialogClose"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ currentApplication?.applicationCode }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentApplication?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentApplication?.department }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentApplication?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ currentApplication?.applicationPurpose }}</el-descriptions-item>
        <el-descriptions-item label="使用地点">{{ currentApplication?.usageLocation }}</el-descriptions-item>
        <el-descriptions-item label="期望使用日期">{{ currentApplication?.expectedDate }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>申请明细</el-divider>

      <el-table :data="currentApplication?.items" border>
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

      <el-divider>审批意见</el-divider>

      <el-form
        ref="formRef"
        :model="approvalForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="审批结果" prop="approvalResult">
          <el-radio-group v-model="approvalForm.approvalResult">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import type { MaterialApplication, ApprovalRequest } from '@/types/approval'

const loading = ref(false)
const submitting = ref(false)
const approvalDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const todoList = ref<MaterialApplication[]>([])
const currentApplication = ref<MaterialApplication>()

const approvalForm = reactive<ApprovalRequest>({
  approvalResult: 1,
  approvalOpinion: '',
  itemApprovals: []
})

const rules: FormRules = {
  approvalResult: [{ required: true, message: '请选择审批结果', trigger: 'change' }]
}

const loadTodoList = async () => {
  loading.value = true
  try {
    todoList.value = await approvalApi.getTodoList()
  } catch (error) {
    console.error('加载待审批列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row: MaterialApplication) => {
  try {
    currentApplication.value = await approvalApi.getApplicationById(row.id)

    if (currentApplication.value.items) {
      currentApplication.value.items.forEach(item => {
        if (!item.approvedQuantity) {
          item.approvedQuantity = item.requestedQuantity
        }
      })
    }

    approvalForm.approvalResult = 1
    approvalForm.approvalOpinion = ''
    approvalDialogVisible.value = true
  } catch (error) {
    console.error('加载申请详情失败:', error)
  }
}

const handleSubmitApproval = async () => {
  const application = currentApplication.value
  if (!formRef.value || !application) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    approvalForm.itemApprovals = application.items?.map(item => ({
      itemId: item.id!,
      approvedQuantity: item.approvedQuantity || 0
    })) || []

    submitting.value = true
    try {
      await approvalApi.approveApplication(application.id, approvalForm)
      ElMessage.success('审批提交成功')
      approvalDialogVisible.value = false
      loadTodoList()
    } catch (error) {
      console.error('审批提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadTodoList()
})
</script>

<style scoped>
.approval-todo {
  padding: 20px;
}
</style>
