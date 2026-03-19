<template>
  <div class="application-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>领用申请</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建申请
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            class="query-keyword-input"
            placeholder="申请单号"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            v-adaptive-select-width="['全部', '审批中', '审批通过', '审批拒绝', '已出库', '已取消']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="审批中" :value="1" />
            <el-option label="审批通过" :value="2" />
            <el-option label="审批拒绝" :value="3" />
            <el-option label="已出库" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker
            v-model="queryForm.createdDateRange"
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

      <el-table :data="applicationList" border stripe v-loading="loading">
        <el-table-column prop="applicationCode" label="申请单号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="department" label="部门" width="140" />
        <el-table-column prop="applicationPurpose" label="用途" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning">审批中</el-tag>
            <el-tag v-else-if="row.status === 2" type="success">审批通过</el-tag>
            <el-tag v-else-if="row.status === 3" type="danger">审批拒绝</el-tag>
            <el-tag v-else-if="row.status === 4" type="info">已出库</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="warning"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
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
      title="新建申请"
      width="900px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="applicationForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用途" prop="applicationPurpose">
          <el-input v-model="applicationForm.applicationPurpose" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="使用地点" prop="usageLocation">
          <el-input v-model="applicationForm.usageLocation" />
        </el-form-item>
        <el-form-item label="期望使用日期" prop="expectedDate">
          <el-date-picker
            v-model="applicationForm.expectedDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-divider>申请明细</el-divider>

        <el-button type="primary" size="small" @click="handleAddItem" style="margin-bottom: 10px">
          添加药品
        </el-button>

        <el-table :data="applicationForm.items" border>
          <el-table-column label="药品" width="220">
            <template #default="{ row, $index }">
              <el-input
                v-model="row.materialName"
                placeholder="选择药品"
                readonly
                @click="selectMaterial($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="总可用库存" width="130" align="right">
            <template #default="{ row }">
              {{ row.availableStock ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="申请数量" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.requestedQuantity"
                :min="0"
                :max="row.availableStock && row.availableStock > 0 ? row.availableStock : undefined"
                :precision="2"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80">
            <template #default="{ row }">
              {{ row.unit || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="用途说明">
            <template #default="{ row }">
              <el-input v-model="row.usagePurpose" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="handleRemoveItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="申请详情" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ currentApplication?.applicationCode }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentApplication?.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentApplication?.department }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentApplication?.status === 1" type="warning">审批中</el-tag>
          <el-tag v-else-if="currentApplication?.status === 2" type="success">审批通过</el-tag>
          <el-tag v-else-if="currentApplication?.status === 3" type="danger">审批拒绝</el-tag>
          <el-tag v-else-if="currentApplication?.status === 4" type="info">已出库</el-tag>
          <el-tag v-else type="info">已取消</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批角色">
          {{ currentApplication?.currentApproverRole || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="出库流程" :span="2">
          <el-tag v-if="currentApplication?.stockOutFlowStatus === 2" type="success">已全部出库</el-tag>
          <el-tag v-else-if="currentApplication?.stockOutFlowStatus === 1" type="warning">出库流程中</el-tag>
          <el-tag v-else type="info">未生成出库单</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请用途" :span="2">{{ currentApplication?.applicationPurpose }}</el-descriptions-item>
        <el-descriptions-item label="使用地点">{{ currentApplication?.usageLocation }}</el-descriptions-item>
        <el-descriptions-item label="期望使用日期">{{ currentApplication?.expectedDate }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ currentApplication?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="出库单号" :span="2">
          <el-space wrap v-if="currentApplication?.stockOutOrders?.length">
            <el-tag
              v-for="order in currentApplication?.stockOutOrders"
              :key="order.id"
              type="info"
            >
              {{ order.outOrderNo }}（{{ order.warehouseName || '未知仓库' }} / {{ order.statusName || '-' }}）
            </el-tag>
          </el-space>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider>申请明细</el-divider>

      <el-table :data="currentApplication?.items" border>
        <el-table-column prop="materialCode" label="药品编码" />
        <el-table-column prop="materialName" label="药品名称" />
        <el-table-column prop="requestedQuantity" label="申请数量" align="right" />
        <el-table-column prop="approvedQuantity" label="批准数量" align="right" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column prop="usagePurpose" label="用途说明" />
      </el-table>

      <el-divider>审批记录</el-divider>

      <el-timeline>
        <el-timeline-item
          v-for="record in currentApplication?.approvalRecords"
          :key="record.id"
          :timestamp="record.approvalTime"
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
    </el-dialog>

    <StockMaterialSelector
      v-model="materialSelectorVisible"
      @select="handleMaterialSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import type { MaterialApplication, ApplicationForm, ApplicationQuery } from '@/types/approval'
import StockMaterialSelector from '@/components/StockMaterialSelector.vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const applicationList = ref<MaterialApplication[]>([])
const currentApplication = ref<MaterialApplication>()
const materialSelectorVisible = ref(false)
const currentMaterialIndex = ref<number>(-1)
const total = ref(0)

const getTodayDate = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const queryForm = reactive<ApplicationQuery & { createdDateRange: string[] }>({
  keyword: '',
  status: -1,
  createdDateRange: [],
  page: 1,
  size: 10
})

const applicationForm = reactive<ApplicationForm>({
  applicationPurpose: '',
  usageLocation: '',
  expectedDate: getTodayDate(),
  items: []
})

const rules: FormRules = {
  applicationPurpose: [{ required: true, message: '请输入用途', trigger: 'blur' }]
}

const loadApplicationList = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = queryForm.createdDateRange
    const res = await approvalApi.getApplicationList({
      keyword: queryForm.keyword,
      status: queryForm.status,
      page: queryForm.page,
      size: queryForm.size,
      startDate,
      endDate
    })
    applicationList.value = res.list
    total.value = res.total
  } catch (error) {
    console.error('加载申请列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadApplicationList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = -1
  queryForm.createdDateRange = []
  handleQuery()
}

const handleAdd = () => {
  Object.assign(applicationForm, {
    applicationPurpose: '',
    usageLocation: '',
    expectedDate: getTodayDate(),
    items: []
  })
  dialogVisible.value = true
}

const handleAddItem = () => {
  applicationForm.items.push({
    materialId: 0,
    materialName: '',
    availableStock: 0,
    requestedQuantity: 0,
    unit: '',
    usagePurpose: ''
  })
}

const handleRemoveItem = (index: number) => {
  applicationForm.items.splice(index, 1)
}

const selectMaterial = (index: number) => {
  currentMaterialIndex.value = index
  materialSelectorVisible.value = true
}

const handleMaterialSelected = (material: {
  materialId: number
  materialCode: string
  materialName: string
  unit?: string
  availableQuantity: number
}) => {
  const row = applicationForm.items[currentMaterialIndex.value]
  if (!row) {
    return
  }

  row.materialId = material.materialId
  row.materialCode = material.materialCode
  row.materialName = material.materialName
  row.unit = material.unit
  row.availableStock = material.availableQuantity
  if (row.requestedQuantity <= 0) {
    row.requestedQuantity = material.availableQuantity > 0 ? 1 : 0
  } else if (material.availableQuantity > 0 && row.requestedQuantity > material.availableQuantity) {
    row.requestedQuantity = material.availableQuantity
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (applicationForm.items.length === 0) {
      ElMessage.warning('请添加申请明细')
      return
    }

    for (const item of applicationForm.items) {
      if (!item.materialId) {
        ElMessage.warning('请选择药品')
        return
      }
      if (item.requestedQuantity <= 0) {
        ElMessage.warning(`药品 ${item.materialName || item.materialId} 的申请数量必须大于 0`)
        return
      }
      if (item.availableStock !== undefined && item.availableStock >= 0 && item.requestedQuantity > item.availableStock) {
        ElMessage.warning(`药品 ${item.materialName || item.materialId} 的申请数量不能超过可用库存`)
        return
      }
    }

    submitting.value = true
    try {
      await approvalApi.createApplication(applicationForm)
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadApplicationList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = async (row: MaterialApplication) => {
  try {
    currentApplication.value = await approvalApi.getApplicationById(row.id)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载申请详情失败:', error)
  }
}

const handleCancel = async (row: MaterialApplication) => {
  await ElMessageBox.confirm('确定要取消该申请吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await approvalApi.cancelApplication(row.id)
    ElMessage.success('取消成功')
    loadApplicationList()
  } catch (error) {
    console.error('取消失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadApplicationList()
})
</script>

<style scoped>
.application-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
