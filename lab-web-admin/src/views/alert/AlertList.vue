<template>
  <div class="alert-list">
    <el-card>
      <template #header>
        <span>预警管理</span>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="预警类型">
          <el-select v-model="queryForm.alertType" placeholder="请选择" clearable>
            <el-option label="低库存预警" :value="1" />
            <el-option label="有效期预警" :value="2" />
            <el-option label="危化品账实差异" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警级别">
          <el-select v-model="queryForm.alertLevel" placeholder="请选择" clearable>
            <el-option label="提示" :value="1" />
            <el-option label="警告" :value="2" />
            <el-option label="严重" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="待处理" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已忽略" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="alertList" border stripe v-loading="loading">
        <el-table-column prop="alertType" label="预警类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.alertType === 1" type="warning">低库存</el-tag>
            <el-tag v-else-if="row.alertType === 2" type="info">有效期</el-tag>
            <el-tag v-else-if="row.alertType === 4" type="danger">账实差异</el-tag>
            <el-tag v-else>其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="预警级别" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.alertLevel === 1">提示</el-tag>
            <el-tag v-else-if="row.alertLevel === 2" type="warning">警告</el-tag>
            <el-tag v-else type="danger">严重</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTitle" label="预警标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="alertContent" label="预警内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="warning">待处理</el-tag>
            <el-tag v-else-if="row.status === 2" type="success">已处理</el-tag>
            <el-tag v-else type="info">已忽略</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTime" label="预警时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
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

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadAlertList"
        @current-change="loadAlertList"
      />
    </el-card>

    <el-dialog
      v-model="processDialogVisible"
      title="处理预警"
      width="600px"
      @close="handleDialogClose"
    >
      <el-descriptions :column="1" border style="margin-bottom: 20px">
        <el-descriptions-item label="预警类型">
          <el-tag v-if="currentAlert?.alertType === 1" type="warning">低库存</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 2" type="info">有效期</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 4" type="danger">账实差异</el-tag>
          <el-tag v-else>其他</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警级别">
          <el-tag v-if="currentAlert?.alertLevel === 1">提示</el-tag>
          <el-tag v-else-if="currentAlert?.alertLevel === 2" type="warning">警告</el-tag>
          <el-tag v-else type="danger">严重</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警标题">{{ currentAlert?.alertTitle }}</el-descriptions-item>
        <el-descriptions-item label="预警内容">{{ currentAlert?.alertContent }}</el-descriptions-item>
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

    <el-dialog v-model="viewDialogVisible" title="预警详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="预警类型">
          <el-tag v-if="currentAlert?.alertType === 1" type="warning">低库存</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 2" type="info">有效期</el-tag>
          <el-tag v-else-if="currentAlert?.alertType === 4" type="danger">账实差异</el-tag>
          <el-tag v-else>其他</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警级别">
          <el-tag v-if="currentAlert?.alertLevel === 1">提示</el-tag>
          <el-tag v-else-if="currentAlert?.alertLevel === 2" type="warning">警告</el-tag>
          <el-tag v-else type="danger">严重</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警标题">{{ currentAlert?.alertTitle }}</el-descriptions-item>
        <el-descriptions-item label="预警内容">{{ currentAlert?.alertContent }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentAlert?.status === 1" type="warning">待处理</el-tag>
          <el-tag v-else-if="currentAlert?.status === 2" type="success">已处理</el-tag>
          <el-tag v-else type="info">已忽略</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预警时间">{{ currentAlert?.alertTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentAlert?.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ currentAlert?.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理措施">{{ currentAlert?.handleRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { alertApi, type AlertRecord } from '@/api/alert'

const loading = ref(false)
const submitting = ref(false)
const processDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const alertList = ref<AlertRecord[]>([])
const currentAlert = ref<AlertRecord>()
const total = ref(0)
const userStore = useUserStore()

const queryForm = reactive({
  alertType: undefined as number | undefined,
  alertLevel: undefined as number | undefined,
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

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 19)
}

const normalizeAlert = (alert: AlertRecord): AlertRecord => ({
  ...alert,
  alertTime: formatDateTime(alert.alertTime),
  handleTime: formatDateTime(alert.handleTime)
})

const loadAlertList = async () => {
  loading.value = true
  try {
    const res = await alertApi.getAlertList(queryForm)
    alertList.value = res.list.map(normalizeAlert)
    total.value = res.total
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
  queryForm.alertType = undefined
  queryForm.alertLevel = undefined
  queryForm.status = undefined
  handleQuery()
}

const handleProcess = (row: AlertRecord) => {
  currentAlert.value = row
  processForm.handleAction = ''
  processDialogVisible.value = true
}

const handleSubmitProcess = async () => {
  if (!formRef.value || !currentAlert.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const userId = userStore.userInfo?.id
    if (!userId) {
      ElMessage.warning('当前用户信息无效，请重新登录后重试')
      return
    }

    submitting.value = true
    try {
      await alertApi.handleAlert(currentAlert.value.id, userId, processForm.handleAction)
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

const handleView = async (row: AlertRecord) => {
  try {
    const detail = await alertApi.getAlertById(row.id)
    currentAlert.value = normalizeAlert(detail)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载预警详情失败:', error)
  }
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
