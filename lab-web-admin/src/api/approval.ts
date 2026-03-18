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

const mapApplication = (item: any): MaterialApplication => ({
  ...item,
  applicationCode: item.applicationCode ?? item.applicationNo,
  applicationPurpose: item.applicationPurpose ?? item.usagePurpose,
  department: item.department ?? item.applicantDept,
  status: mapApplicationStatus(item.status),
  currentApprovalLevel: item.currentApprovalLevel ?? 1,
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
    return request
      .get<any, PageResult<any>>('/applications', { params })
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
      .get<any, PageResult<any>>('/applications', {
        params: {
          status: 2,
          page: 1,
          size: 100
        }
      })
      .then(result => (result?.records ?? []).map(mapApplication))
  },

  getHazardousUsageRecords(params: { keyword?: string; status?: number; page?: number; size?: number }) {
    return request
      .get<any, PageResult<any>>('/hazardous/usage-records', { params })
      .then(result => toListResult<HazardousUsageRecord>(result, (item) => ({
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
