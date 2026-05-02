import request from '@/utils/request'
import type {
  MaterialApplication,
  ApplicationForm,
  ApplicationQuery,
  ApprovalRequest,
  HazardousUsageRecord,
  HazardousReturnRequest
} from '@/types/approval'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

const mapApplicationStatus = (status?: number) => {
  if (status === 3) return 2
  if (status === 4) return 3
  if (status === 5 || status === 6) return 4
  if (status === 7) return 5
  return 1
}

const roleDisplayNameMap: Record<string, string> = {
  ADMIN: '系统管理员',
  CENTER_ADMIN: '实验中心主任',
  CENTER_DIRECTOR: '中心主任',
  LAB_MANAGER: '实验室负责人',
  LAB_ROOM_MANAGER: '实验室管理人员',
  DEPUTY_DEAN: '分管院长',
  DEAN: '院长',
  TEACHER: '教师',
  STUDENT: '学生',
  EQUIPMENT_ADMIN: '设备管理员',
  '001': '院长',
  '002': '分管院长',
  '003': '实验中心主任',
  '005': '实验中心管理人员',
  '006': '教师',
  '008': '学生'
}

const mapApprovalRoleName = (role?: string): string => {
  if (!role) {
    return ''
  }
  const roleKey = String(role).trim()
  return roleDisplayNameMap[roleKey] ?? roleKey
}

const mapPendingStatus = (item: any): string => {
  if (item.currentPendingStatus) {
    return item.currentPendingStatus
  }
  if (item.status === 3 || item.approvalStatus === 2) return '审批通过'
  if (item.status === 4 || item.approvalStatus === 3) return '审批拒绝'
  if (item.status === 5) return '出库流程中'
  if (item.status === 6) return '已完成'
  if (item.status === 7) return '已取消'
  return '待审批'
}

const mapApplication = (item: any): MaterialApplication => ({
  ...item,
  applicationCode: item.applicationCode ?? item.applicationNo,
  applicationPurpose: item.applicationPurpose ?? item.usagePurpose,
  department: item.department ?? item.applicantDept,
  status: mapApplicationStatus(item.status),
  approvalStatus: item.approvalStatus,
  currentApprovalLevel: item.currentApprovalLevel ?? 1,
  currentApproverId: item.currentApproverId,
  currentApproverName: item.currentApproverName,
  currentApproverRole: mapApprovalRoleName(item.currentApproverRole),
  currentApproverIds: item.currentApproverIds ?? [],
  currentApproverNames: item.currentApproverNames ?? [],
  currentPendingStatus: mapPendingStatus(item),
  stockOutFlowStatus: item.stockOutFlowStatus,
  stockOutFlowStatusName: item.stockOutFlowStatusName,
  stockOutOrderNos: item.stockOutOrderNos,
  stockOutOrders: item.stockOutOrders ?? [],
  items: (item.items ?? []).map((child: any) => ({
    ...child,
    requestedQuantity: child.requestedQuantity ?? child.applyQuantity,
    usagePurpose: child.usagePurpose ?? child.remark
  })),
  approvalRecords: (item.approvalRecords ?? []).map((record: any) => ({
    ...record,
    approverName: record.approverName,
    approvalResult: record.approvalResult
  }))
})

const toListResult = <T>(result: PageResult<T>, mapper?: (item: any) => T) => {
  const list = (result?.records ?? result?.list ?? []).map(item => mapper ? mapper(item) : item)
  return {
    list,
    total: result?.total ?? list.length
  }
}

export const approvalApi = {
  getApplicationList(params: ApplicationQuery) {
    const mappedParams = {
      ...params,
      uiStatus: params.status,
      status: undefined
    }
    return request
      .get<any, PageResult<any>>('/applications', { params: mappedParams })
      .then(result => toListResult<MaterialApplication>(result, mapApplication))
  },

  getApplicationById(id: number) {
    return request.get<any, any>(`/applications/${id}`).then(mapApplication)
  },

  createApplication(data: ApplicationForm) {
    const payload = {
      applicationType: 1,
      usagePurpose: data.applicationPurpose,
      usageLocation: data.usageLocation,
      expectedDate: data.expectedDate,
      items: data.items.map(item => ({
        materialId: item.materialId,
        applyQuantity: item.requestedQuantity,
        remark: item.usagePurpose
      }))
    }
    return request.post<any, number>('/applications', payload)
  },

  cancelApplication(id: number) {
    return request.post(`/applications/${id}/cancel`)
  },

  approveApplication(id: number, data: ApprovalRequest) {
    const payload = {
      approvalResult: data.approvalResult,
      approvalOpinion: data.approvalOpinion,
      approvedQuantities: (data.itemApprovals ?? []).map(item => ({
        itemId: item.itemId,
        approvedQuantity: item.approvedQuantity
      }))
    }
    return request.post(`/applications/${id}/approve`, payload)
  },

  getTodoList() {
    return request
      .get<any, any[]>('/applications/pending')
      .then(result => (result ?? []).map(mapApplication))
  },

  getHazardousUsageRecords(params: {
    keyword?: string
    status?: number
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }) {
    const mappedParams = {
      ...params,
      status: params.status === undefined || params.status === -1 ? undefined : params.status
    }
    return request
      .get<any, PageResult<any>>('/hazardous/usage-records', { params: mappedParams })
      .then(result => toListResult<HazardousUsageRecord>(result, item => ({
        ...item,
        receiveDate: item.receiveDate ?? item.usageDate
      })))
  },

  returnHazardousMaterial(id: number, data: HazardousReturnRequest) {
    return request.post(`/hazardous/usage-records/${id}/return`, data)
  },

  getHazardousLedger(params: { startDate?: string; endDate?: string; materialId?: number }) {
    return request.get<any, any>('/hazardous/ledger', { params })
  },

  exportHazardousLedger(params: { startDate?: string; endDate?: string; materialId?: number }) {
    return request.get<any, Blob>('/hazardous/ledger/export', {
      params,
      responseType: 'blob'
    })
  }
}
