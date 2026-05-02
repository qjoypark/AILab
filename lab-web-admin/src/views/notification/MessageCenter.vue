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

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部消息" name="all">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" />
        </el-tab-pane>
        <el-tab-pane label="审批通知" name="approval" />
        <el-tab-pane label="预警通知" name="alert" />
        <el-tab-pane label="系统通知" name="system" />
      </el-tabs>

      <el-table :data="messageList" border stripe v-loading="loading" @row-click="handleRowClick">
        <el-table-column width="60">
          <template #default="{ row }">
            <el-icon v-if="row.isRead === 0" color="#409eff" :size="20">
              <CircleFilled />
            </el-icon>
          </template>
        </el-table-column>

        <el-table-column prop="notificationType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.notificationType === 1" type="success">审批</el-tag>
            <el-tag v-else-if="row.notificationType === 2" type="warning">预警</el-tag>
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

        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead === 0"
              link
              type="primary"
              @click.stop="handleMarkRead(row)"
            >
              标记已读
            </el-button>
            <el-button link type="danger" @click.stop="handleDelete(row)">
              删除
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
        @size-change="loadMessageList"
        @current-change="loadMessageList"
      />
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="消息详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">
          <el-tag v-if="currentMessage?.notificationType === 1" type="success">审批</el-tag>
          <el-tag v-else-if="currentMessage?.notificationType === 2" type="warning">预警</el-tag>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { notificationApi, type NotificationItem } from '@/api/notification'

const userStore = useUserStore()

const loading = ref(false)
const detailDialogVisible = ref(false)
const activeTab = ref('all')
const messageList = ref<NotificationItem[]>([])
const currentMessage = ref<NotificationItem>()
const total = ref(0)
const unreadCount = ref(0)

const queryForm = reactive({
  notificationType: undefined as number | undefined,
  page: 1,
  size: 10
})

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 19)
}

const loadMessageList = async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) {
      messageList.value = []
      total.value = 0
      unreadCount.value = 0
      return
    }

    const res = await notificationApi.queryNotifications({
      receiverId: userId,
      notificationType: queryForm.notificationType,
      page: queryForm.page,
      size: queryForm.size
    })

    messageList.value = (res.list ?? []).map(item => ({
      ...item,
      createdTime: formatDateTime(item.createdTime),
      readTime: formatDateTime(item.readTime)
    }))
    total.value = res.total ?? 0
    unreadCount.value = res.unreadCount ?? 0
  } catch (error) {
    console.error('加载消息列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadMessageList()
}

const handleTabChange = (tab: string | number) => {
  if (tab === 'approval') {
    queryForm.notificationType = 1
  } else if (tab === 'alert') {
    queryForm.notificationType = 2
  } else if (tab === 'system') {
    queryForm.notificationType = 3
  } else {
    queryForm.notificationType = undefined
  }
  handleSearch()
}

const handleRowClick = (row: NotificationItem) => {
  currentMessage.value = row
  detailDialogVisible.value = true

  if (row.isRead === 0) {
    handleMarkRead(row)
  }
}

const handleMarkRead = async (row: NotificationItem) => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    await notificationApi.markAsRead(row.id, userId)

    row.isRead = 1
    if (unreadCount.value > 0) {
      unreadCount.value -= 1
    }
    ElMessage.success('已标记为已读')
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const handleMarkAllRead = async () => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    await notificationApi.markAllAsRead(userId)
    messageList.value.forEach(m => m.isRead = 1)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记失败:', error)
  }
}

const handleDelete = async (row: NotificationItem) => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    await ElMessageBox.confirm(
      '确定要删除这条消息吗？删除后将不再显示在消息中心。',
      '删除消息',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await notificationApi.deleteNotification(row.id, userId)
    ElMessage.success('消息已删除')
    await loadMessageList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
    }
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
