export interface LabRoomManager {
  id?: number
  labRoomId?: number
  managerId: number
  managerName: string
  isPrimary?: number
  status?: number
  createdTime?: string
}

export interface LabRoom {
  id: number
  roomCode: string
  roomName: string
  building?: string
  floor?: string
  roomNo?: string
  capacity?: number
  roomType?: number
  safetyLevel?: number
  equipmentSummary?: string
  notice?: string
  status: number
  createdTime?: string
  updatedTime?: string
  managers?: LabRoomManager[]
}

export interface LabRoomForm {
  id?: number
  roomCode: string
  roomName: string
  building?: string
  floor?: string
  roomNo?: string
  capacity?: number
  roomType?: number
  safetyLevel?: number
  equipmentSummary?: string
  notice?: string
  status?: number
}

export interface LabRoomQuery {
  page?: number
  size?: number
  status?: number
  roomType?: number
  keyword?: string
}

export interface LabRoomListResult {
  list: LabRoom[]
  total: number
}

export interface LabUsageParticipant {
  id?: number
  applicationId?: number
  userId: number
  realName: string
  deptName?: string
  createdTime?: string
}

export interface LabUsageApplication {
  id: number
  applicationNo: string
  applicantId: number
  applicantName: string
  applicantDept?: string
  labRoomId: number
  labRoomCode: string
  labRoomName: string
  usageType?: number
  usagePurpose: string
  projectName?: string
  expectedAttendeeCount?: number
  startTime: string
  endTime: string
  specialEquipment?: string
  safetyCommitment?: number
  status: number
  approvalStatus?: number
  currentApproverId?: number
  currentApproverRole?: string
  currentApproverName?: string
  currentApproverIds?: number[]
  currentApproverNames?: string[]
  currentPendingStatus?: string
  remark?: string
  createdTime?: string
  updatedTime?: string
  participants?: LabUsageParticipant[]
  approvalRecords?: Array<{
    id?: number
    approverName?: string
    approvalLevel?: number
    approvalResult?: number
    approvalOpinion?: string
    approvalTime?: string
  }>
}

export interface LabUsageApplicationForm {
  labRoomId?: number
  usageType?: number
  usagePurpose: string
  projectName?: string
  expectedAttendeeCount?: number
  startTime?: string
  endTime?: string
  specialEquipment?: string
  safetyCommitment?: number
  remark?: string
  participants: LabUsageParticipant[]
}

export interface LabUsageApplicationQuery {
  page?: number
  size?: number
  status?: number
  labRoomId?: number
  keyword?: string
  startTime?: string
  endTime?: string
}

export interface LabUsageApplicationListResult {
  list: LabUsageApplication[]
  total: number
}

export interface LabUsageSchedule {
  applicationId: number
  applicationNo: string
  labRoomId: number
  labRoomCode: string
  labRoomName: string
  applicantName: string
  applicantDept?: string
  usagePurpose: string
  projectName?: string
  startTime: string
  endTime: string
  participants?: LabUsageParticipant[]
}

export interface LabUsageScheduleQuery {
  labRoomId?: number
  teacherId?: number
  startTime?: string
  endTime?: string
}
