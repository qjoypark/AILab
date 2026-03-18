<template>
  <div class="dashboard">
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="药品总数" :value="statistics.totalMaterials" suffix="种">
            <template #prefix>
              <el-icon color="#409eff"><Goods /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="库存总值" :value="statistics.totalStockValue" prefix="¥" :precision="2">
            <template #prefix>
              <el-icon color="#67c23a"><Money /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="待审批" :value="statistics.pendingApprovals" suffix="个">
            <template #prefix>
              <el-icon color="#e6a23c"><Document /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="预警数量" :value="statistics.alertCount" suffix="个">
            <template #prefix>
              <el-icon color="#f56c6c"><Warning /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 20px">
      <template #header>
        <span>快捷入口</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="4" v-for="item in quickLinks" :key="item.path">
          <div class="quick-link" @click="$router.push(item.path)">
            <el-icon :size="32" :color="item.color">
              <component :is="item.icon" />
            </el-icon>
            <div class="link-text">{{ item.text }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button link type="primary" @click="$router.push('/approval/todo')">更多</el-button>
            </div>
          </template>
          <el-table :data="todoList" :show-header="false" max-height="300">
            <el-table-column prop="title" />
            <el-table-column prop="time" width="180" align="right" />
          </el-table>
          <el-empty v-if="todoList.length === 0" description="暂无待办事项" :image-size="100" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最新消息</span>
              <el-button link type="primary" @click="$router.push('/notifications')">更多</el-button>
            </div>
          </template>
          <el-table :data="messageList" :show-header="false" max-height="300">
            <el-table-column width="60">
              <template #default="{ row }">
                <el-icon v-if="row.isRead === 0" color="#409eff"><CircleFilled /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="title" show-overflow-tooltip />
            <el-table-column prop="time" width="150" align="right" />
          </el-table>
          <el-empty v-if="messageList.length === 0" description="暂无新消息" :image-size="100" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>预警提醒</span>
          <el-button link type="primary" @click="$router.push('/alerts')">更多</el-button>
        </div>
      </template>
      <el-table :data="alertList" border stripe max-height="300">
        <el-table-column prop="alertType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.alertType === 1" type="warning">低库存</el-tag>
            <el-tag v-else-if="row.alertType === 2" type="info">有效期</el-tag>
            <el-tag v-else-if="row.alertType === 4" type="danger">账实差异</el-tag>
            <el-tag v-else>其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTitle" label="预警标题" min-width="180" />
        <el-table-column prop="alertContent" label="预警内容" min-width="220" />
        <el-table-column prop="alertTime" label="时间" width="180" />
      </el-table>
      <el-empty v-if="alertList.length === 0" description="暂无预警" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { materialApi } from '@/api/material'
import { dashboardApi } from '@/api/dashboard'
import { notificationApi } from '@/api/notification'
import { alertApi } from '@/api/alert'

const userStore = useUserStore()

const statistics = ref({
  totalMaterials: 0,
  totalStockValue: 0,
  pendingApprovals: 0,
  alertCount: 0
})

const quickLinks = [
  { path: '/materials', text: '药品列表', icon: 'Goods', color: '#409eff' },
  { path: '/inventory/stock', text: '库存查询', icon: 'Box', color: '#67c23a' },
  { path: '/inventory/stock-in', text: '入库管理', icon: 'Upload', color: '#e6a23c' },
  { path: '/inventory/stock-out', text: '出库管理', icon: 'Download', color: '#f56c6c' },
  { path: '/applications', text: '领用申请', icon: 'Document', color: '#909399' },
  { path: '/approval/todo', text: '待审批', icon: 'CircleCheck', color: '#409eff' }
]

const todoList = ref<Array<{ title: string; time: string }>>([])
const messageList = ref<Array<{ title: string; time: string; isRead: number }>>([])
const alertList = ref<Array<{ alertType: number; alertTitle: string; alertContent: string; alertTime: string }>>([])

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 16)
}

const loadDashboardData = async () => {
  const userId = userStore.userInfo?.id
  if (!userId) {
    statistics.value = {
      totalMaterials: 0,
      totalStockValue: 0,
      pendingApprovals: 0,
      alertCount: 0
    }
    todoList.value = []
    messageList.value = []
    alertList.value = []
    return
  }

  try {
    const [materialResult, stockSummary, todoResult, notificationResult, alertResult] = await Promise.all([
      materialApi.getMaterialList({ page: 1, size: 1 }),
      dashboardApi.getStockSummary(),
      dashboardApi.getTodoList(userId),
      notificationApi.queryNotifications({ receiverId: userId, page: 1, size: 5 }),
      alertApi.getAlertList({ page: 1, size: 5, status: 1 })
    ])

    statistics.value = {
      totalMaterials: materialResult.total ?? 0,
      totalStockValue: Number(stockSummary.totalValue ?? 0),
      pendingApprovals: todoResult.approvalCount ?? 0,
      alertCount: todoResult.alertCount ?? alertResult.total ?? 0
    }

    todoList.value = (todoResult.list ?? []).slice(0, 5).map(item => ({
      title: item.title,
      time: formatDateTime(item.deadline || item.createdTime)
    }))

    messageList.value = (notificationResult.list ?? []).slice(0, 5).map(item => ({
      title: item.title,
      time: formatDateTime(item.createdTime),
      isRead: item.isRead
    }))

    alertList.value = (alertResult.list ?? []).slice(0, 5).map(item => ({
      alertType: item.alertType,
      alertTitle: item.alertTitle,
      alertContent: item.alertContent,
      alertTime: formatDateTime(item.alertTime)
    }))
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
}

.quick-link:hover {
  background-color: #f5f7fa;
  transform: translateY(-2px);
}

.link-text {
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}
</style>
