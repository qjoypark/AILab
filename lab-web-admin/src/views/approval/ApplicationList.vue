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

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="申请单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="审批中" :value="1" />
            <el-option label="审批通过" :value="2" />
            <el-option label="审批拒绝" :value="3" />
            <el-option label="已出库" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 申请列表 -->
      <el-table :data="applicationList" border stripe v-loading="loading">
        <el-table-column prop="applicationCode" label="申请单号" width="180" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="applicationPurpose" label="用途" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning">审批中</el-tag>
            <el-tag v-else-if="row.status === 2" type="success">审批通过</el-tag>
            <el-tag v-else-if="row.status === 3" type="danger">审批拒绝</el-tag>
            <el-tag v-else-if="row.status === 4" type="info">已出库</el-tag>
            <el-tag v-else type="info">已取消</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="申请时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
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

    <!-- 申请表单对话框 -->
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
        <el-form-item label="期望日期" prop="expectedDate">
          <el-date-picker v-model="applicationForm.expectedDate" type="date" style="width: 100%" />
        </el-form-item>
        
        <el-divider>申请明细</el-divider>
        
        <el-button type="primary" size="small" @click="handleAddItem" style="margin-bottom: 10px">
          添加药品
        </el-button>
        
        <el-table :data="applicationForm.items" border>
          <el-table-column label="药品" width="200">
            <template #default="{ row, $index }">
              <el-input v-model="row.materialName" placeholder="选择药品" readonly @click="selectMaterial($index)" />
            </template>
          </el-table-column>
          <el-table-column label="申请数量" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.requestedQuantity" :min="0" :precision="2" style="width: 100%" />
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

    <!-- 查看详情对话框 -->
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
        <el-descriptions-item label="用途" :span="2">{{ currentApplication?.applicationPurpose }}</el-descriptions-item>
        <el-descriptions-item label="使用地点">{{ currentApplication?.usageLocation }}</el-descriptions-item>
        <el-descriptions-item label="期望日期">{{ currentApplication?.expectedDate }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ currentApplication?.createdTime }}</el-descriptions-item>
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
            <p><strong>审批结果：</strong>
              <el-tag v-if="record.approvalResult === 1" type="success">通过</el-tag>
              <el-tag v-else type="danger">拒绝</el-tag>
            </p>
            <p v-if="record.approvalOpinion"><strong>审批意见：</strong>{{ record.approvalOpinion }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { approvalApi } from '@/api/approval'
import type { MaterialApplication, ApplicationForm, ApplicationQuery, MaterialApplicationItem } from '@/types/approval'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const applicationList = ref<MaterialApplication[]>([])
const currentApplication = ref<MaterialApplication>()
const total = ref(0)

const queryForm = reactive<ApplicationQuery>({
  keyword: '',
  status: undefined,
  page: 1,
  size: 10
})

const applicationForm = reactive<ApplicationForm>({
  applicationPurpose: '',
  usageLocation: '',
  expectedDate: '',
  items: []
})

const rules: FormRules = {
  applicationPurpose: [{ required: true, message: '请输入用途', trigger: 'blur' }]
}

const loadApplicationList = async () => {
  loading.value = true
  try {
    const res = await approvalApi.getApplicationList(queryForm)
    applicationList.value = res.list
    total.value = res.total
  } catch (error) {
    console.error('加载申请列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadApplicationList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  handleQuery()
}

const handleAdd = () => {
  Object.assign(applicationForm, {
    applicationPurpose: '',
    usageLocation: '',
    expectedDate: '',
    items: []
  })
  dialogVisible.value = true
}

const handleAddItem = () => {
  applicationForm.items.push({
    materialId: 0,
    materialName: '',
    requestedQuantity: 0,
    unit: '',
    usagePurpose: ''
  })
}

const handleRemoveItem = (index: number) => {
  applicationForm.items.splice(index, 1)
}

const selectMaterial = (index: number) => {
  // TODO: 实现药品选择对话框
  ElMessage.info('药品选择功能待实现')
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (applicationForm.items.length === 0) {
      ElMessage.warning('请添加申请明细')
      return
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
