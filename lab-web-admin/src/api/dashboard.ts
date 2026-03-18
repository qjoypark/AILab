import request from '@/utils/request'

export interface TodoItem {
  type: string
  typeDesc?: string
  businessId: number
  businessNo?: string
  title: string
  content?: string
  priority?: number
  priorityDesc?: string
  deadline?: string
  createdTime?: string
  applicantName?: string
  applicantDept?: string
}

export interface TodoListResult {
  total: number
  approvalCount: number
  alertCount: number
  list: TodoItem[]
}

export interface StockSummary {
  totalValue?: number
  categories?: Array<{
    categoryId: number
    categoryName: string
    itemCount: number
    totalQuantity?: number
    totalValue?: number
    valuePercentage?: number
  }>
}

export const dashboardApi = {
  getTodoList(userId: number) {
    return request.get<any, TodoListResult>('/todo', { params: { userId } })
  },

  getStockSummary(params?: { warehouseId?: number; materialType?: number }) {
    return request.get<any, StockSummary>('/reports/stock-summary', { params })
  }
}
