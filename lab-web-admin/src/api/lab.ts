import request from '@/utils/request'
import type {
  LabRoom,
  LabRoomForm,
  LabRoomListResult,
  LabRoomManager,
  LabRoomQuery,
  LabUsageApplication,
  LabUsageApplicationForm,
  LabUsageApplicationListResult,
  LabUsageApplicationQuery,
  LabUsageSchedule,
  LabUsageScheduleQuery
} from '@/types/lab'

interface PageResult<T> {
  records?: T[]
  list?: T[]
  total?: number
}

const toListResult = <T>(result: PageResult<T>): { list: T[]; total: number } => {
  const list = result?.records ?? result?.list ?? []
  return {
    list,
    total: result?.total ?? list.length
  }
}

export const labApi = {
  getLabRoomPage(params: LabRoomQuery): Promise<LabRoomListResult> {
    return request
      .get<any, PageResult<LabRoom>>('/lab-rooms', { params })
      .then(result => toListResult(result))
  },

  getLabRoomById(id: number): Promise<LabRoom> {
    return request.get<any, LabRoom>(`/lab-rooms/${id}`)
  },

  createLabRoom(data: LabRoomForm): Promise<LabRoom> {
    return request.post<any, LabRoom>('/lab-rooms', data)
  },

  updateLabRoom(id: number, data: LabRoomForm): Promise<LabRoom> {
    return request.put<any, LabRoom>(`/lab-rooms/${id}`, data)
  },

  deleteLabRoom(id: number): Promise<void> {
    return request.delete(`/lab-rooms/${id}`)
  },

  getLabRoomManagers(id: number): Promise<LabRoomManager[]> {
    return request.get<any, LabRoomManager[]>(`/lab-rooms/${id}/managers`)
  },

  saveLabRoomManagers(id: number, managers: LabRoomManager[]): Promise<LabRoomManager[]> {
    return request.put<any, LabRoomManager[]>(`/lab-rooms/${id}/managers`, { managers })
  },

  getLabUsageApplicationPage(params: LabUsageApplicationQuery): Promise<LabUsageApplicationListResult> {
    return request
      .get<any, PageResult<LabUsageApplication>>('/lab-usage-applications', { params })
      .then(result => toListResult(result))
  },

  getPendingLabUsageApplications(): Promise<LabUsageApplication[]> {
    return request.get<any, LabUsageApplication[]>('/lab-usage-applications/pending')
  },

  getLabUsageApplicationById(id: number): Promise<LabUsageApplication> {
    return request.get<any, LabUsageApplication>(`/lab-usage-applications/${id}`)
  },

  getLabUsageApplicationByIdSilently(id: number): Promise<LabUsageApplication> {
    return request.get<any, LabUsageApplication>(`/lab-usage-applications/${id}`, {
      silentError: true
    } as any)
  },

  exportLabUsageApplicationPdf(id: number): Promise<Blob> {
    return request.get<any, Blob>(`/lab-usage-applications/${id}/pdf`, {
      responseType: 'blob'
    })
  },

  getLabUsageOverlaps(params: {
    labRoomId: number
    startTime: string
    endTime: string
  }): Promise<LabUsageApplication[]> {
    return request.get<any, LabUsageApplication[]>('/lab-usage-applications/overlaps', { params })
  },

  createLabUsageApplication(data: LabUsageApplicationForm): Promise<number> {
    return request.post<any, number>('/lab-usage-applications', data)
  },

  cancelLabUsageApplication(id: number): Promise<void> {
    return request.post(`/lab-usage-applications/${id}/cancel`)
  },

  approveLabUsageApplication(id: number, data: { approvalResult: number; approvalOpinion?: string }): Promise<void> {
    return request.post(`/lab-usage-applications/${id}/approve`, data)
  },

  getLabUsageSchedules(params: LabUsageScheduleQuery): Promise<LabUsageSchedule[]> {
    return request.get<any, LabUsageSchedule[]>('/lab-usage-applications/schedules', { params })
  }
}
