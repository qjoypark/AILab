<template>
  <div class="usage-record-list">
    <el-card>
      <template #header>
        <span>药品使用</span>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            class="query-keyword-input"
            placeholder="申请单号/领用人/用途/地点"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            v-adaptive-select-width="['全部', '领用中', '已归还']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="领用中" :value="1" />
            <el-option label="已归还" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="领用日期">
          <el-date-picker
            v-model="queryForm.usageDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="recordList" border stripe v-loading="loading">
        <el-table-column prop="applicationNo" label="领用单号" width="180">
          <template #default="{ row }">
            {{ row.applicationNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="药品名称" min-width="160">
          <template #default="{ row }">
            {{ row.materialName || `药品#${row.materialId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="领用人" width="120" />
        <el-table-column prop="receivedQuantity" label="领用数量" width="100" align="right" />
        <el-table-column prop="actualUsedQuantity" label="消耗数量" width="100" align="right">
          <template #default="{ row }">
            {{ row.actualUsedQuantity ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="returnedQuantity" label="归还数量" width="100" align="right">
          <template #default="{ row }">
            {{ row.returnedQuantity ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="usagePurpose" label="用途说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="usageLocation" label="使用地点" width="140" />
        <el-table-column prop="receiveDate" label="领用日期" width="120" />
        <el-table-column prop="returnDate" label="归还日期" width="120">
          <template #default="{ row }">
            {{ row.returnDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning">领用中</el-tag>
            <el-tag v-else type="success">已归还</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
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
      v-model="returnDialogVisible"
      title="归还核实"
      width="620px"
      @close="handleDialogClose"
    >
      <el-descriptions :column="2" border style="margin-bottom: 20px">
        <el-descriptions-item label="领用单号">{{ currentRecord?.applicationNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentRecord?.materialName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用人">{{ currentRecord?.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用数量">{{ currentRecord?.receivedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途说明" :span="2">{{ currentRecord?.usagePurpose || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="formRef" :model="returnForm" :rules="rules" label-width="120px">
        <el-form-item label="归还数量" prop="returnedQuantity">
          <el-input-number
            v-model="returnForm.returnedQuantity"
            :min="0"
            :max="currentRecord?.receivedQuantity ?? 0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="自动计入消耗">
          <el-input :model-value="consumedQuantityText" readonly />
          <div class="hint-text">默认全归还；减少的数量自动计入“消耗数量”。</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReturn" :loading="submitting">确认归还</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="使用记录详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="领用单号">{{ currentRecord?.applicationNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentRecord?.materialName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用人">{{ currentRecord?.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentRecord?.status === 1" type="warning">领用中</el-tag>
          <el-tag v-else type="success">已归还</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="领用数量">{{ currentRecord?.receivedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="消耗数量">{{ currentRecord?.actualUsedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="归还数量">{{ currentRecord?.returnedQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="废弃数量">{{ currentRecord?.wasteQuantity ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途说明" :span="2">{{ currentRecord?.usagePurpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用地点">{{ currentRecord?.usageLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领用日期">{{ currentRecord?.receiveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归还日期">{{ currentRecord?.returnDate || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import type { HazardousReturnRequest, HazardousUsageRecord } from '@/types/approval'

interface ReturnFormState {
  returnedQuantity: number
  remark: string
}

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
  status: -1 as number,
  usageDateRange: [] as string[],
  page: 1,
  size: 10
})

const returnForm = reactive<ReturnFormState>({
  returnedQuantity: 0,
  remark: ''
})

const consumedQuantity = computed(() => {
  if (!currentRecord.value) return 0
  const received = Number(currentRecord.value.receivedQuantity ?? 0)
  const returned = Number(returnForm.returnedQuantity ?? 0)
  const consumed = received - returned
  return consumed > 0 ? consumed : 0
})

const consumedQuantityText = computed(() => consumedQuantity.value.toFixed(2))

const rules: FormRules = {
  returnedQuantity: [
    { required: true, message: '请输入归还数量', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback) => {
        const received = Number(currentRecord.value?.receivedQuantity ?? 0)
        const numericValue = Number(value)
        if (Number.isNaN(numericValue)) {
          callback(new Error('归还数量格式不正确'))
          return
        }
        if (numericValue < 0) {
          callback(new Error('归还数量不能小于 0'))
          return
        }
        if (numericValue > received) {
          callback(new Error('归还数量不能大于领用数量'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadRecordList = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = queryForm.usageDateRange
    const res = await approvalApi.getHazardousUsageRecords({
      keyword: queryForm.keyword,
      status: queryForm.status,
      page: queryForm.page,
      size: queryForm.size,
      startDate,
      endDate
    })
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
  queryForm.status = -1
  queryForm.usageDateRange = []
  handleQuery()
}

const handleReturn = (row: HazardousUsageRecord) => {
  currentRecord.value = row
  Object.assign(returnForm, {
    returnedQuantity: Number(row.receivedQuantity ?? 0),
    remark: ''
  })
  returnDialogVisible.value = true
}

const handleSubmitReturn = async () => {
  if (!formRef.value || !currentRecord.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      const payload: HazardousReturnRequest = {
        returnedQuantity: Number(returnForm.returnedQuantity),
        actualUsedQuantity: Number(consumedQuantity.value),
        wasteQuantity: 0,
        remark: returnForm.remark
      }
      await approvalApi.returnHazardousMaterial(currentRecord.value.id, payload)
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

.hint-text {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
