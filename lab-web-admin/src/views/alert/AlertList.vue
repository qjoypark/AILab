<template>
  <div class="alert-list">
    <el-card>
      <template #header>
        <span>预警管理</span>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="预警类型">
          <el-select v-model="queryForm.alertType" placeholder="请选择" clearable>
            <el-option label="低库存预警" value="LOW_STOCK" />
            <el-option label="有效期预警" value="EXPIRY_WARNING" />
            <el-option label="危化品异常" value="HAZARDOUS_ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="queryForm.alertLevel" placeholder="请选择" clearable>
            <el-option label="一般" value="NORMAL" />
            <el-option label="重要" value="IMPORTANT" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 预警列表 -->
      <el-table :data="alertList" border stripe v-loading="loading">
        <el-table-column prop="alertType" label="预警类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.alertType === 'LOW_STOCK'" type="warning">低库存</el-tag>
            <el-tag v-else-if="row.alertType === 'EXPIRY_WARNING'" type="info">有效期</el-tag>
            <el-tag v-else type="danger">危化品异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="预警级别" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.alertLevel === 'NORMAL'">一般</el-tag>
            <el-tag v-else-if="row.alertLevel === 'IMPORTANT'" type="warning">重要</el-tag>
            <el-tag v-else type="danger">严重</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="药品名称" min-width="150" />
        <el-table-column prop="alertContent" label="预警内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="currentValue" label="当前值" width="100" align="right" />
        <el-table-column prop="thresholdValue" label="阈值" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待处理</el-tag>
            <el-tag v-else type="success">已处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="预警时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              link
              type="primary"
              @click="handleProcess(row)"
            >
              处理
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

    <!-- 处理对话框 -->
    <el-dialog
      v-model="processDialogVisible"
      title="处理预警"
      width="600px"
      @close="handleDialogClose"
    >
      <el-descriptions :column="1" border style="margin-bottom: 20px">
        <el-descriptions-item label="预警类型">
          <el-tag v-if="currentAlert?.alertType === 'LOW_STOCK'" type="warning">低库存</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 'EXPIRY_WARNING'" type="info">有效期</el-tag>
          <el-tag v-else type="danger">危化品异常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警级别">
          <el-tag v-if="currentAlert?.alertLevel === 'NORMAL'">一般</el-tag>
          <el-tag v-else-if="currentAlert?.alertLevel === 'IMPORTANT'" type="warning">重要</el-tag>
          <el-tag v-else type="danger">严重</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentAlert?.materialName }}</el-descriptions-item>
        <el-descriptions-item label="预警内容">{{ currentAlert?.alertContent }}</el-descriptions-item>
        <el-descriptions-item label="当前值">{{ currentAlert?.currentValue }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ currentAlert?.thresholdValue }}</el-descriptions-item>
      </el-descriptions>

      <el-form
        ref="formRef"
        :model="processForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="处理措施" prop="handleAction">
          <el-input v-model="processForm.handleAction" type="textarea" :rows="4" placeholder="请描述处理措施" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProcess" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="预警详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="预警类型">
          <el-tag v-if="currentAlert?.alertType === 'LOW_STOCK'" type="warning">低库存</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 'EXPIRY_WARNING'" type="info">有效期</el-tag>
          <el-tag v-else type="danger">危化品异常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警级别">
          <el-tag v-if="currentAlert?.alertLevel === 'NORMAL'">一般</el-tag>
          <el-tag v-else-if="currentAlert?.alertLevel === 'IMPORTANT'" type="warning">重要</el-tag>
          <el-tag v-else type="danger">严重</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentAlert?.materialName }}</el-descriptions-item>
        <el-descriptions-item label="预警内容">{{ currentAlert?.alertContent }}</el-descriptions-item>
        <el-descriptions-item label="当前值">{{ currentAlert?.currentValue }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ currentAlert?.thresholdValue }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentAlert?.status === 0" type="warning">待处理</el-tag>
          <el-tag v-else type="success">已处理</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警时间">{{ currentAlert?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentAlert?.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ currentAlert?.handleBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理措施">{{ currentAlert?.handleAction || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

interface Alert {
  id: number
  alertType: string
  alertLevel: string
  materialId: number
  materialName: string
  alertContent: string
  currentValue: number
  thresholdValue: number
  status: number
  createdTime: string
  handleTime?: string
  handleBy?: string
  handleAction?: string
}

const loading = ref(false)
const submitting = ref(false)
const processDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const alertList = ref<Alert[]>([])
const currentAlert = ref<Alert>()
const total = ref(0)

const queryForm = reactive({
  alertType: '',
  alertLevel: '',
  status: undefined as number | undefined,
  page: 1,
  size: 10
})

const processForm = reactive({
  handleAction: ''
})

const rules: FormRules = {
  handleAction: [{ required: true, message: '请输入处理措施', trigger: 'blur' }]
}

const loadAlertList = async () => {
  loading.value = true
  try {
    // TODO: 调用实际API
    // const res = await alertApi.getAlertList(queryForm)
    // alertList.value = res.list
    // total.value = res.total
    
    // 模拟数据
    alertList.value = [
      {
        id: 1,
        alertType: 'LOW_STOCK',
        alertLevel: 'IMPORTANT',
        materialId: 1,
        materialName: '无水乙醇',
        alertContent: '库存数量低于安全库存',
        currentValue: 5,
        thresholdValue: 10,
        status: 0,
        createdTime: '2026-03-17 10:00:00'
      },
      {
        id: 2,
        alertType: 'EXPIRY_WARNING',
        alertLevel: 'NORMAL',
        materialId: 2,
        materialName: '盐酸',
        alertContent: '距离有效期不足30天',
        currentValue: 15,
        thresholdValue: 30,
        status: 0,
        createdTime: '2026-03-17 09:00:00'
      }
    ]
    total.value = 2
  } catch (error) {
    console.error('加载预警列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadAlertList()
}

const handleReset = () => {
  queryForm.alertType = ''
  queryForm.alertLevel = ''
  queryForm.status = undefined
  handleQuery()
}

const handleProcess = (row: Alert) => {
  currentAlert.value = row
  processForm.handleAction = ''
  processDialogVisible.value = true
}

const handleSubmitProcess = async () => {
  if (!formRef.value || !currentAlert.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      // TODO: 调用实际API
      // await alertApi.processAlert(currentAlert.value.id, processForm)
      
      ElMessage.success('处理成功')
      processDialogVisible.value = false
      loadAlertList()
    } catch (error) {
      console.error('处理失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = (row: Alert) => {
  currentAlert.value = row
  viewDialogVisible.value = true
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadAlertList()
})
</script>

<style scoped>
.alert-list {
  padding: 20px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
