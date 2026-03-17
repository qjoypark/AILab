<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <el-button type="primary" @click="handleMarkAllRead" :disabled="unreadCount === 0">
            全部标记已读
          </el-button>
        </div>
      </template>

      <!-- 消息类型标签 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部消息" name="all">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" />
        </el-tab-pane>
        <el-tab-pane label="审批通知" name="approval" />
        <el-tab-pane label="预警通知" name="alert" />
        <el-tab-pane label="系统通知" name="system" />
      </el-tabs>

      <!-- 消息列表 -->
      <el-table :data="messageList" border stripe v-loading="loading" @row-click="handleRowClick">
        <el-table-column width="60">
          <template #default="{ row }">
            <el-icon v-if="row.isRead === 0" color="#409eff" :size="20">
              <CircleFilled />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="messageType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.messageType === 'APPROVAL'" type="success">审批</el-tag>
            <el-tag v-else-if="row.messageType === 'ALERT'" type="warning">预警</el-tag>
            <el-tag v-else type="info">系统</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :style="{ fontWeight: row.isRead === 0 ? 'bold' : 'normal' }">
              {{ row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdTime" label="时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead === 0"
              link
              type="primary"
              @click.stop="handleMarkRead(row)"
            >
              标记已读
            </el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
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

    <!-- 消息详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="消息详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">
          <el-tag v-if="currentMessage?.messageType === 'APPROVAL'" type="success">审批</el-tag>
          <el-tag v-else-if="currentMessage?.messageType === 'ALERT'" type="warning">预警</el-tag>
          <el-tag v-else type="info">系统</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentMessage?.title }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentMessage?.content }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentMessage?.createdTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Message {
  id: number
  messageType: string
  title: string
  content: string
  isRead: number
  createdTime: string
  relatedId?: number
}

const loading = ref(false)
const detailDialogVisible = ref(false)
const activeTab = ref('all')
const messageList = ref<Message[]>([])
const currentMessage = ref<Message>()
const total = ref(0)

const queryForm = reactive({
  messageType: '',
  page: 1,
  size: 10
})

const unreadCount = computed(() => {
  return messageList.value.filter(m => m.isRead === 0).length
})

const loadMessageList = async () => {
  loading.value = true
  try {
    // TODO: 调用实际API
    // const res = await notificationApi.getMessageList(queryForm)
    // messageList.value = res.list
    // total.value = res.total
    
    // 模拟数据
    messageList.value = [
      {
        id: 1,
        messageType: 'APPROVAL',
        title: '您的申请已通过审批',
        content: '您提交的领用申请（申请单号：APP202603170001）已通过审批，请及时领取。',
        isRead: 0,
        createdTime: '2026-03-17 14:30:00'
      },
      {
        id: 2,
        messageType: 'ALERT',
        title: '低库存预警',
        content: '无水乙醇库存不足，当前库存：5瓶，安全库存：10瓶',
        isRead: 0,
        createdTime: '2026-03-17 10:00:00'
      },
      {
        id: 3,
        messageType: 'SYSTEM',
        title: '系统维护通知',
        content: '系统将于今晚22:00-23:00进行维护，期间无法访问，请提前做好准备。',
        isRead: 1,
        createdTime: '2026-03-16 15:00:00'
      }
    ]
    total.value = 3
  } catch (error) {
    console.error('加载消息列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadMessageList()
}

const handleTabChange = (tab: string) => {
  queryForm.messageType = tab === 'all' ? '' : tab.toUpperCase()
  handleQuery()
}

const handleRowClick = (row: Message) => {
  currentMessage.value = row
  detailDialogVisible.value = true
  
  // 如果是未读消息，自动标记为已读
  if (row.isRead === 0) {
    handleMarkRead(row)
  }
}

const handleMarkRead = async (row: Message) => {
  try {
    // TODO: 调用实际API
    // await notificationApi.markAsRead(row.id)
    
    row.isRead = 1
    ElMessage.success('已标记为已读')
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const handleMarkAllRead = async () => {
  try {
    // TODO: 调用实际API
    // await notificationApi.markAllAsRead()
    
    messageList.value.forEach(m => m.isRead = 1)
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记失败:', error)
  }
}

const handleDelete = async (row: Message) => {
  await ElMessageBox.confirm('确定要删除该消息吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    // TODO: 调用实际API
    // await notificationApi.deleteMessage(row.id)
    
    const index = messageList.value.findIndex(m => m.id === row.id)
    if (index > -1) {
      messageList.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
  }
}

onMounted(() => {
  loadMessageList()
})
</script>

<style scoped>
.message-center {
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

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
