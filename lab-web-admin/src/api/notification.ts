import request from '@/utils/request'

export interface NotificationItem {
  id: number
  receiverId: number
  notificationType: number
  notificationTypeDesc?: string
  title: string
  content: string
  businessType?: string
  businessId?: number
  pushChannel?: number
  pushChannelDesc?: string
  isRead: number
  readTime?: string
  createdTime?: string
}

export interface NotificationPageResult {
  total: number
  unreadCount: number
  list: NotificationItem[]
}

export interface NotificationQuery {
  receiverId: number
  notificationType?: number
  isRead?: number
  page?: number
  size?: number
}

export const notificationApi = {
  queryNotifications(params: NotificationQuery) {
    return request.get<any, NotificationPageResult>('/notifications', { params })
  },

  markAsRead(id: number, userId: number) {
    return request.post(`/notifications/${id}/read`, null, { params: { userId } })
  },

  markAllAsRead(userId: number) {
    return request.post('/notifications/read-all', null, { params: { userId } })
  },

  getUnreadCount(userId: number) {
    return request.get<any, number>('/notifications/unread-count', { params: { userId } })
  }
}
