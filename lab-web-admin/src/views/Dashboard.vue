<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.key" :xs="24" :sm="12" :md="12" :lg="6">
        <el-card class="stat-card" :class="card.theme" shadow="never">
          <div class="stat-content">
            <div>
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">
                <el-statistic :value="card.value" :suffix="card.suffix" />
              </div>
            </div>
            <div class="stat-icon">
              <el-icon :size="24">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>快捷入口</span>
          <small>常用功能一键直达</small>
        </div>
      </template>

      <div class="quick-grid">
        <div v-for="item in visibleQuickLinks" :key="item.path" class="quick-link" @click="go(item.path)">
          <el-icon :size="22" :style="{ color: item.color }">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.text }}</span>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button v-if="todoHeaderLink" class="header-link-btn" link @click="go(todoHeaderLink.path)">
                {{ todoHeaderLink.text }}
              </el-button>
            </div>
          </template>

          <el-table :data="todoList" :show-header="false" max-height="300">
            <el-table-column prop="title" />
            <el-table-column prop="time" width="180" align="right" />
          </el-table>
          <el-empty v-if="todoList.length === 0" description="暂无待办事项" :image-size="90" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>最新消息</span>
              <el-button class="header-link-btn" link @click="go('/notifications')">查看全部</el-button>
            </div>
          </template>

          <el-table :data="messageList" :show-header="false" max-height="300">
            <el-table-column width="56">
              <template #default="{ row }">
                <el-icon v-if="row.isRead === 0" color="#3b82f6"><Notification /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="title" show-overflow-tooltip />
            <el-table-column prop="time" width="150" align="right" />
          </el-table>
          <el-empty v-if="messageList.length === 0" description="暂无系统消息" :image-size="90" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>预警提醒</span>
          <el-button class="header-link-btn" link @click="go('/alerts')">查看全部</el-button>
        </div>
      </template>

      <el-table :data="alertList" border stripe max-height="320">
        <el-table-column prop="alertType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.alertType === 1" type="warning">低库存</el-tag>
            <el-tag v-else-if="row.alertType === 2" type="info">临近有效期</el-tag>
            <el-tag v-else-if="row.alertType === 4" type="danger">账实差异</el-tag>
            <el-tag v-else>其他</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTitle" label="预警标题" min-width="180" />
        <el-table-column prop="alertContent" label="预警内容" min-width="220" />
        <el-table-column prop="alertTime" label="时间" width="180" />
      </el-table>
      <el-empty v-if="alertList.length === 0" description="暂无预警" :image-size="90" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { materialApi } from '@/api/material'
import { dashboardApi } from '@/api/dashboard'
import { notificationApi } from '@/api/notification'
import { alertApi } from '@/api/alert'
import { approvalApi } from '@/api/approval'
import { labApi } from '@/api/lab'
import type { MaterialApplication } from '@/types/approval'
import type { LabUsageApplication } from '@/types/lab'
import {
  ALERT_PERMISSIONS,
  APPROVAL_PERMISSIONS,
  APPROVAL_TODO_PERMISSIONS,
  INVENTORY_STOCK_IN_PERMISSIONS,
  INVENTORY_STOCK_OUT_PERMISSIONS,
  INVENTORY_STOCK_PERMISSIONS,
  MATERIAL_PERMISSIONS
} from '@/constants/permissions'

const router = useRouter()
const userStore = useUserStore()

const statistics = ref({
  totalMaterials: 0,
  totalStockValue: 0,
  pendingApprovals: 0,
  alertCount: 0
})

const statCards = computed(() => [
  {
    key: 'materials',
    label: '药品总数',
    value: statistics.value.totalMaterials,
    suffix: '种',
    icon: 'Goods',
    theme: 'is-blue'
  },
  {
    key: 'stockValue',
    label: '库存总值',
    value: statistics.value.totalStockValue,
    suffix: '元',
    icon: 'Money',
    theme: 'is-emerald'
  },
  {
    key: 'pending',
    label: '待审批',
    value: statistics.value.pendingApprovals,
    suffix: '项',
    icon: 'Document',
    theme: 'is-amber'
  },
  {
    key: 'alerts',
    label: '预警数量',
    value: statistics.value.alertCount,
    suffix: '条',
    icon: 'Warning',
    theme: 'is-rose'
  }
])

type QuickLink = {
  path: string
  text: string
  icon: string
  color: string
  permissions?: readonly string[]
}

const quickLinks: QuickLink[] = [
  { path: '/materials', text: '药品列表', icon: 'Goods', color: '#3b82f6', permissions: MATERIAL_PERMISSIONS },
  { path: '/inventory/stock', text: '库存查询', icon: 'Box', color: '#10b981', permissions: INVENTORY_STOCK_PERMISSIONS },
  { path: '/inventory/stock-in', text: '入库管理', icon: 'Upload', color: '#f59e0b', permissions: INVENTORY_STOCK_IN_PERMISSIONS },
  { path: '/inventory/stock-out', text: '出库管理', icon: 'Download', color: '#ef4444', permissions: INVENTORY_STOCK_OUT_PERMISSIONS },
  { path: '/applications', text: '领用申请', icon: 'Document', color: '#6366f1', permissions: APPROVAL_PERMISSIONS },
  { path: '/approval/todo', text: '申请审批', icon: 'CircleCheck', color: '#0ea5e9', permissions: APPROVAL_TODO_PERMISSIONS },
  { path: '/alerts', text: '预警管理', icon: 'Bell', color: '#f97316', permissions: ALERT_PERMISSIONS },
  { path: '/notifications', text: '消息中心', icon: 'Message', color: '#64748b' }
]

const visibleQuickLinks = computed(() =>
  quickLinks.filter(item => !item.permissions || userStore.hasAnyPermission([...item.permissions]))
)

const todoList = ref<Array<{ title: string; time: string }>>([])
const messageList = ref<Array<{ title: string; time: string; isRead: number }>>([])
const alertList = ref<Array<{ alertType: number; alertTitle: string; alertContent: string; alertTime: string }>>([])

const canViewMaterialStats = computed(() => userStore.hasAnyPermission([...MATERIAL_PERMISSIONS]))
const canViewStockStats = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_PERMISSIONS]))
const canViewMaterialApprovalTodos = computed(() => userStore.hasAnyPermission(['application:approve']))
const canViewLabApprovalTodos = computed(() => userStore.hasAnyPermission(['lab-usage:approve']))
const canViewApprovalTodos = computed(() => userStore.hasAnyPermission([...APPROVAL_TODO_PERMISSIONS]))
const canViewAlerts = computed(() => userStore.hasAnyPermission([...ALERT_PERMISSIONS]))
const todoHeaderLink = computed(() => {
  if (canViewApprovalTodos.value) {
    return { path: '/approval/todo', text: '查看审批' }
  }
  if (canViewAlerts.value) {
    return { path: '/alerts', text: '查看预警' }
  }
  return null
})

const resolveSafely = async <T,>(promise: Promise<T>, fallback: T, label: string): Promise<T> => {
  try {
    return await promise
  } catch (error) {
    console.warn(`加载首页${label}失败`, error)
    return fallback
  }
}

const mapMaterialTodo = (item: MaterialApplication) => {
  const typeName = item.applicationType === 2 ? '危化品领用' : '药品领用'
  return {
    title: `[${typeName}] ${item.applicantName || '-'} 提交的申请`,
    time: formatDateTime(item.createdTime)
  }
}

const mapLabTodo = (item: LabUsageApplication) => ({
  title: `[实验室使用] ${item.applicantName || '-'} 提交的申请`,
  time: formatDateTime(item.createdTime)
})

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) {
    return '-'
  }
  return dateTime.replace('T', ' ').slice(0, 16)
}

const go = (path: string) => {
  router.push(path)
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
    const [materialResult, stockSummary, materialTodoItems, labTodoItems, notificationResult, alertResult] = await Promise.all([
      canViewMaterialStats.value
        ? resolveSafely(materialApi.getMaterialList({ page: 1, size: 1 }), { list: [], total: 0 }, '药品统计')
        : Promise.resolve({ list: [], total: 0 }),
      canViewStockStats.value
        ? resolveSafely(dashboardApi.getStockSummary(), { totalValue: 0 }, '库存统计')
        : Promise.resolve({ totalValue: 0 }),
      canViewMaterialApprovalTodos.value
        ? resolveSafely(approvalApi.getTodoList(), [], '药品待审批')
        : Promise.resolve([]),
      canViewLabApprovalTodos.value
        ? resolveSafely(labApi.getPendingLabUsageApplications(), [], '实验室待审批')
        : Promise.resolve([]),
      resolveSafely(notificationApi.queryNotifications({ receiverId: userId, page: 1, size: 5 }), { list: [], total: 0 }, '最新消息'),
      canViewAlerts.value
        ? resolveSafely(alertApi.getAlertList({ page: 1, size: 5, status: 1 }), { list: [], total: 0 }, '预警提醒')
        : Promise.resolve({ list: [], total: 0 })
    ])

    statistics.value = {
      totalMaterials: canViewMaterialStats.value ? materialResult.total ?? 0 : 0,
      totalStockValue: canViewStockStats.value ? Number(stockSummary.totalValue ?? 0) : 0,
      pendingApprovals: canViewApprovalTodos.value ? materialTodoItems.length + labTodoItems.length : 0,
      alertCount: canViewAlerts.value ? alertResult.total ?? 0 : 0
    }

    const approvalTodos = [
      ...materialTodoItems.map(mapMaterialTodo),
      ...labTodoItems.map(mapLabTodo)
    ].sort((a, b) => String(b.time ?? '').localeCompare(String(a.time ?? '')))

    const alertTodos = canViewAlerts.value
      ? (alertResult.list ?? []).map(item => ({
          title: `[预警] ${item.alertTitle}`,
          time: formatDateTime(item.alertTime)
        }))
      : []

    todoList.value = [...approvalTodos, ...alertTodos].slice(0, 5)

    messageList.value = (notificationResult.list ?? []).slice(0, 5).map(item => ({
      title: item.title,
      time: formatDateTime(item.createdTime),
      isRead: item.isRead
    }))

    alertList.value = canViewAlerts.value
      ? (alertResult.list ?? []).slice(0, 5).map(item => ({
          alertType: item.alertType,
          alertTitle: item.alertTitle,
          alertContent: item.alertContent,
          alertTime: formatDateTime(item.alertTime)
        }))
      : []
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
  gap: 16px;
}

.stat-card {
  border: none;
  color: #fff;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.stat-value :deep(.el-statistic__content) {
  color: #fff;
}

.stat-value :deep(.el-statistic__number) {
  font-size: 28px;
  font-weight: 600;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
}

.is-blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.is-emerald {
  background: linear-gradient(135deg, #10b981, #059669);
}

.is-amber {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.is-rose {
  background: linear-gradient(135deg, #fb7185, #e11d48);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e8eef8;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-link span {
  font-size: 14px;
  color: #334155;
}

.quick-link:hover {
  border-color: #bfdbfe;
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.12);
}

@media (max-width: 1200px) {
  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.card-header small {
  color: #64748b;
  font-size: 12px;
  font-weight: 400;
}

.header-link-btn {
  padding: 4px 10px !important;
  border-radius: 8px !important;
  color: #64748b !important;
  font-weight: 500;
  background: transparent !important;
  box-shadow: none !important;
  border: 1px solid transparent !important;
}

.header-link-btn:hover {
  color: #2563eb !important;
  background: #eef4ff !important;
  border-color: #dbeafe !important;
}
</style>
