<template>
  <div class="usage-record-list">
    <el-card>
      <template #header>
        <span>危化品使用记录</span>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="药品名称/用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="使用中" :value="1" />
            <el-option label="已归还" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 使用记录列表 -->
      <el-table :data="recordList" border stripe v-loading="loading">
        <el-table-column prop="materialName" label="药品名称" min-width="150" />
        <el-table-column prop="userName" label="使用人" width="100" />
        <el-table-column prop="receivedQuantity" label="领用数量" width="100" align="right" />
        <el-table-column prop="actualUsedQuantity" label="实际使用" width="100" align="right">
          <template #default="{ row }">
            {{ row.actualUsedQuantity || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="returnedQuantity" label="归还数量" width="100" align="right">
          <template #default="{ row }">
            {{ row.returnedQuantity || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="wasteQuantity" label="废弃数量" width="100" align="right">
          <template #default="{ row }">
            {{ row.wasteQuantity || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="usagePurpose" label="用途" min-width="150" show-overflow-tooltip />
        <el-table-column prop="usageLocation" label="使用地点" width="120" />
        <el-table-column prop="receiveDate" label="领用日期" width="120" />
        <el-table-column prop="returnDate" label="归还日期" width="120">
          <template #default="{ row }">
            {{ row.returnDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning">使用中</el-tag>
            <el-tag v-else type="success">已归还</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              link
              type="primary"
              @click="handleReturn(row)"
            >
              归还
            </el-button>
            <el-button link type="info" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 归还对话框 -->
    <el-dialog
      v-model="returnDialogVisible"
      title="危化品归还"
      width="600px"
      @close="handleDialogClose"
    >
      <el-descriptions :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="药品名称">{{ currentRecord?.materialName }}</el-descriptions-item>
        <el-descriptions-item label="使用人">{{ currentRecord?.userName }}</el-descriptions-item>
        <el-descriptions-item label="领用数量">{{ currentRecord?.receivedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="领用日期">{{ currentRecord?.receiveDate }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ currentRecord?.usagePurpose }}</el-descriptions-item>
      </el-descriptions>

      <el-form
        ref="formRef"
        :model="returnForm"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="实际使用量" prop="actualUsedQuantity">
          <el-input-number
            v-model="returnForm.actualUsedQuantity"
            :min="0"
            :max="currentRecord?.receivedQuantity"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="归还数量" prop="returnedQuantity">
          <el-input-number
            v-model="returnForm.returnedQuantity"
            :min="0"
            :max="remainingQuantity"
            :precision="2"
            style="width: 100%"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 5px">
            剩余可归还: {{ remainingQuantity.toFixed(2) }}
          </div>
        </el-form-item>
        <el-form-item label="废弃数量" prop="wasteQuantity">
          <el-input-number
            v-model="returnForm.wasteQuantity"
            :min="0"
            :max="remainingQuantity"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReturn" :loading="submitting">确定归还</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="使用记录详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="药品名称">{{ currentRecord?.materialName }}</el-descriptions-item>
        <el-descriptions-item label="使用人">{{ currentRecord?.userName }}</el-descriptions-item>
        <el-descriptions-item label="领用数量">{{ currentRecord?.receivedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="实际使用量">{{ currentRecord?.actualUsedQuantity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归还数量">{{ currentRecord?.returnedQuantity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="废弃数量">{{ currentRecord?.wasteQuantity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ currentRecord?.usagePurpose }}</el-descriptions-item>
        <el-descriptions-item label="使用地点">{{ currentRecord?.usageLocation }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentRecord?.status === 1" type="warning">使用中</el-tag>
          <el-tag v-else type="success">已归还</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="领用日期">{{ currentRecord?.receiveDate }}</el-descriptions-item>
        <el-descriptions-item label="归还日期">{{ currentRecord?.returnDate || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import type { HazardousUsageRecord, HazardousReturnRequest } from '@/types/approval'

const loading = ref(false)
const submitting = ref(false)
const returnDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const recordList = ref<HazardousUsageRecord[]>([])
const currentRecord = ref<HazardousUsageRecord>()
const total = ref(0)

const queryForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  size: 10
})

const returnForm = reactive<HazardousReturnRequest>({
  actualUsedQuantity: 0,
  returnedQuantity: 0,
  wasteQuantity: 0,
  remark: ''
})

const remainingQuantity = computed(() => {
  if (!currentRecord.value) return 0
  return currentRecord.value.receivedQuantity - returnForm.actualUsedQuantity
})

const rules: FormRules = {
  actualUsedQuantity: [
    { required: true, message: '请输入实际使用量', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value + returnForm.returnedQuantity + returnForm.wasteQuantity !== currentRecord.value?.receivedQuantity) {
          callback(new Error('实际使用量 + 归还数量 + 废弃数量 必须等于领用数量'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  returnedQuantity: [{ required: true, message: '请输入归还数量', trigger: 'blur' }],
  wasteQuantity: [{ required: true, message: '请输入废弃数量', trigger: 'blur' }]
}

const loadRecordList = async () => {
  loading.value = true
  try {
    const res = await approvalApi.getHazardousUsageRecords(queryForm)
    recordList.value = res.list
    total.value = res.total
  } catch (error) {
    console.error('加载使用记录失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadRecordList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  handleQuery()
}

const handleReturn = (row: HazardousUsageRecord) => {
  currentRecord.value = row
  Object.assign(returnForm, {
    actualUsedQuantity: row.receivedQuantity,
    returnedQuantity: 0,
    wasteQuantity: 0,
    remark: ''
  })
  returnDialogVisible.value = true
}

const handleSubmitReturn = async () => {
  if (!formRef.value || !currentRecord.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      await approvalApi.returnHazardousMaterial(currentRecord.value!.id, returnForm)
      ElMessage.success('归还成功')
      returnDialogVisible.value = false
      loadRecordList()
    } catch (error) {
      console.error('归还失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = (row: HazardousUsageRecord) => {
  currentRecord.value = row
  viewDialogVisible.value = true
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadRecordList()
})
</script>

<style scoped>
.usage-record-list {
  padding: 20px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
