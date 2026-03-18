import request from '@/utils/request'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

export interface AlertRecord {
  id: number
  alertType: number
  alertLevel: number
  businessType?: string
  businessId?: number
  alertTitle: string
  alertContent: string
  alertTime?: string
  status: number
  handlerId?: number
  handlerName?: string
  handleTime?: string
  handleRemark?: string
}

export interface AlertQuery {
  page?: number
  size?: number
  alertType?: number
  alertLevel?: number
  status?: number
}

export const alertApi = {
  getAlertList(params: AlertQuery) {
    return request.get<any, PageResult<AlertRecord>>('/alerts', { params }).then((result) => ({
      list: result?.records ?? result?.list ?? [],
      total: result?.total ?? 0
    }))
  },

  getAlertById(id: number) {
    return request.get<any, AlertRecord>(`/alerts/${id}`)
  },

  handleAlert(id: number, handlerId: number, handleRemark?: string) {
    return request.post(`/alerts/${id}/handle`, null, {
      params: {
        handlerId,
        handleRemark
      }
    })
  },

  ignoreAlert(id: number, handlerId: number) {
    return request.post(`/alerts/${id}/ignore`, null, {
      params: { handlerId }
    })
  }
}
