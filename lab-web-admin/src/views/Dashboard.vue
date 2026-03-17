<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
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

    <!-- 快捷入口 -->
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
      <!-- 待办事项 -->
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

      <!-- 最新消息 -->
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
                <el-icon v-if="!row.isRead" color="#409eff"><CircleFilled /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="title" show-overflow-tooltip />
            <el-table-column prop="time" width="150" align="right" />
          </el-table>
          <el-empty v-if="messageList.length === 0" description="暂无新消息" :image-size="100" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警列表 -->
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
            <el-tag v-if="row.alertType === 'LOW_STOCK'" type="warning">低库存</el-tag>
            <el-tag v-else-if="row.alertType === 'EXPIRY_WARNING'" type="info">有效期</el-tag>
            <el-tag v-else type="danger">危化品异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="药品名称" min-width="150" />
        <el-table-column prop="alertContent" label="预警内容" min-width="200" />
        <el-table-column prop="createdTime" label="时间" width="180" />
      </el-table>
      <el-empty v-if="alertList.length === 0" description="暂无预警" :image-size="100" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const statistics = ref({
  totalMaterials: 0,
  totalStockValue: 0,
  pendingApprovals: 0,
  alertCount: 0
})

const quickLinks = [
  { path: '/materials', text: '药品管理', icon: 'Goods', color: '#409eff' },
  { path: '/inventory/stock', text: '库存查询', icon: 'Box', color: '#67c23a' },
  { path: '/inventory/stock-in', text: '入库管理', icon: 'Upload', color: '#e6a23c' },
  { path: '/inventory/stock-out', text: '出库管理', icon: 'Download', color: '#f56c6c' },
  { path: '/applications', text: '领用申请', icon: 'Document', color: '#909399' },
  { path: '/approval/todo', text: '待审批', icon: 'CircleCheck', color: '#409eff' }
]

const todoList = ref([
  { title: '审批申请单 APP202603170001', time: '2026-03-17 14:30' },
  { title: '审批申请单 APP202603170002', time: '2026-03-17 13:20' },
  { title: '处理低库存预警', time: '2026-03-17 10:00' }
])

const messageList = ref([
  { title: '您的申请已通过审批', time: '2026-03-17 14:30', isRead: false },
  { title: '低库存预警通知', time: '2026-03-17 10:00', isRead: false },
  { title: '系统维护通知', time: '2026-03-16 15:00', isRead: true }
])

const alertList = ref([
  {
    alertType: 'LOW_STOCK',
    materialName: '无水乙醇',
    alertContent: '库存数量低于安全库存',
    createdTime: '2026-03-17 10:00:00'
  },
  {
    alertType: 'EXPIRY_WARNING',
    materialName: '盐酸',
    alertContent: '距离有效期不足30天',
    createdTime: '2026-03-17 09:00:00'
  }
])

const loadStatistics = async () => {
  // TODO: 调用实际API加载统计数据
  statistics.value = {
    totalMaterials: 156,
    totalStockValue: 285600.50,
    pendingApprovals: 3,
    alertCount: 2
  }
}

onMounted(() => {
  loadStatistics()
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
