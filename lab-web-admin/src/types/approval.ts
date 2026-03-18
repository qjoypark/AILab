export interface MaterialApplication {
  id: number
  applicationCode: string
  applicantId: number
  applicantName?: string
  department?: string
  applicationPurpose: string
  usageLocation?: string
  expectedDate?: string
  status: number // 0-草稿 1-审批中 2-审批通过 3-审批拒绝 4-已出库 5-已取消
  currentApprovalLevel?: number
  createdTime?: string
  items?: MaterialApplicationItem[]
  approvalRecords?: ApprovalRecord[]
  stockOutFlowStatus?: number
  stockOutFlowStatusName?: string
  stockOutOrderNos?: string
  stockOutOrders?: StockOutOrderInfo[]
}

export interface StockOutOrderInfo {
  id: number
  outOrderNo: string
  warehouseId?: number
  warehouseName?: string
  status?: number
  statusName?: string
  createdTime?: string
}

export interface MaterialApplicationItem {
  id?: number
  applicationId?: number
  materialId: number
  materialCode?: string
  materialName?: string
  materialType?: number
  requestedQuantity: number
  availableStock?: number
  approvedQuantity?: number
  unit?: string
  usagePurpose?: string
}

export interface ApplicationForm {
  applicationPurpose: string
  usageLocation?: string
  expectedDate?: string
  items: MaterialApplicationItem[]
}

export interface ApplicationQuery {
  keyword?: string
  status?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface ApprovalRecord {
  id: number
  applicationId: number
  approvalLevel: number
  approverId: number
  approverName?: string
  approvalResult: number // 1-通过 2-拒绝
  approvalOpinion?: string
  approvedQuantity?: number
  approvalTime?: string
}

export interface ApprovalRequest {
  approvalResult: number
  approvalOpinion?: string
  itemApprovals?: {
    itemId: number
    approvedQuantity: number
  }[]
}

export interface HazardousUsageRecord {
  id: number
  materialId: number
  materialName?: string
  userId: number
  userName?: string
  receivedQuantity: number
  actualUsedQuantity?: number
  returnedQuantity?: number
  wasteQuantity?: number
  usagePurpose: string
  usageLocation: string
  receiveDate: string
  returnDate?: string
  status: number // 1-使用中 2-已归还
}

export interface HazardousReturnRequest {
  actualUsedQuantity: number
  returnedQuantity: number
  wasteQuantity: number
  remark?: string
}
