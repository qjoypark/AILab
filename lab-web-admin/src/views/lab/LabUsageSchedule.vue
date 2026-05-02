<template>
  <div class="lab-schedule-page">
    <section class="schedule-hero">
      <div>
        <p class="eyebrow">Lab Calendar</p>
        <h2>实验室使用日程</h2>
        <p class="hero-subtitle">
          只展示审批通过的实验室使用记录。同一实验室、同一时段允许多位老师共享，页面会自动聚合展示。
        </p>
      </div>
      <div class="hero-actions">
        <el-radio-group v-model="quickRange" size="large" @change="handleQuickRangeChange">
          <el-radio-button label="today">今天</el-radio-button>
          <el-radio-button label="week">本周</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" :loading="loading" @click="loadSchedules">刷新</el-button>
      </div>
    </section>

    <el-card class="filter-card" shadow="never">
      <el-form :model="queryForm" class="schedule-filter" label-position="top">
        <el-form-item label="实验室">
          <el-select v-model="queryForm.labRoomId" placeholder="全部实验室" clearable filterable>
            <el-option
              v-for="room in labRoomOptions"
              :key="room.id"
              :label="formatRoomOption(room)"
              :value="room.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="使用教师">
          <el-select
            v-model="queryForm.teacherId"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchTeacherUsers"
            :loading="teacherUserLoading"
            placeholder="搜索教师姓名、用户名或部门"
          >
            <el-option
              v-for="user in teacherUserOptions"
              :key="user.id"
              :label="formatUserOption(user)"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时段">
          <el-date-picker
            v-model="scheduleDateRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            clearable
          />
        </el-form-item>
        <el-form-item label="操作">
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="loadSchedules">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <section class="summary-grid">
      <article class="summary-card">
        <div class="summary-icon mint">
          <el-icon><Tickets /></el-icon>
        </div>
        <div>
          <span>申请记录</span>
          <strong>{{ scheduleList.length }}</strong>
          <small>{{ dateRangeLabel }}</small>
        </div>
      </article>
      <article class="summary-card">
        <div class="summary-icon blue">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div>
          <span>使用实验室</span>
          <strong>{{ summary.usedRoomCount }}</strong>
          <small>{{ summary.busiestRoom || '暂无使用安排' }}</small>
        </div>
      </article>
      <article class="summary-card">
        <div class="summary-icon amber">
          <el-icon><User /></el-icon>
        </div>
        <div>
          <span>涉及教师</span>
          <strong>{{ summary.teacherCount }}</strong>
          <small>{{ summary.sharedGroupCount }} 个共享时段</small>
        </div>
      </article>
      <article class="summary-card">
        <div class="summary-icon coral">
          <el-icon><Clock /></el-icon>
        </div>
        <div>
          <span>下一场使用</span>
          <strong>{{ summary.nextStartTime || '-' }}</strong>
          <small>{{ summary.nextRoom || '暂无后续日程' }}</small>
        </div>
      </article>
    </section>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="content-header">
          <div>
            <strong>日程视图</strong>
            <span>按审批通过后的实际使用安排展示</span>
          </div>
          <el-radio-group v-model="viewMode">
            <el-radio-button label="timeline">日程轴</el-radio-button>
            <el-radio-button label="room">按实验室</el-radio-button>
            <el-radio-button label="table">明细表</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-alert
        v-if="sharedGroups.length"
        type="success"
        show-icon
        :closable="false"
        class="schedule-alert"
      >
        <template #title>
          当前筛选范围内有 {{ sharedGroups.length }} 个共享使用时段。共享不视为冲突，请以现场协调为准。
        </template>
      </el-alert>

      <div v-loading="loading" class="schedule-content">
        <el-empty v-if="!scheduleList.length" description="当前筛选条件下暂无已通过的使用日程" />

        <div v-else-if="viewMode === 'timeline'" class="timeline-view">
          <section v-for="day in dayBuckets" :key="day.dateKey" class="day-section">
            <div class="day-stamp">
              <strong>{{ day.day }}</strong>
              <span>{{ day.weekday }}</span>
              <small>{{ day.items.length }} 条</small>
            </div>
            <div class="day-events">
              <article
                v-for="item in day.items"
                :key="item.applicationId"
                class="event-card"
                :class="{ shared: isSharedSchedule(item) }"
                @click="openScheduleDetail(item)"
              >
                <div class="event-time">
                  <span>{{ formatTime(item.startTime) }}</span>
                  <i />
                  <span>{{ formatTime(item.endTime) }}</span>
                </div>
                <div class="event-main">
                  <div class="event-title">
                    <strong>{{ item.projectName || item.usagePurpose }}</strong>
                    <el-tag v-if="isSharedSchedule(item)" type="warning" effect="plain">共享</el-tag>
                  </div>
                  <p>{{ formatScheduleRoom(item) }}</p>
                  <div class="event-meta">
                    <el-tag type="info" effect="plain">{{ item.applicantName }}</el-tag>
                    <el-tag
                      v-for="participantName in visibleParticipantNames(item)"
                      :key="`${item.applicationId}-${participantName}`"
                      effect="plain"
                    >
                      {{ participantName }}
                    </el-tag>
                    <span v-if="hiddenParticipantCount(item) > 0">+{{ hiddenParticipantCount(item) }} 位</span>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </div>

        <div v-else-if="viewMode === 'room'" class="room-board">
          <article v-for="room in roomBuckets" :key="room.key" class="room-column">
            <div class="room-column-header">
              <strong>{{ room.roomName }}</strong>
              <span>{{ room.roomCode }}</span>
            </div>
            <div class="room-events">
              <button
                v-for="item in room.items"
                :key="item.applicationId"
                class="room-event"
                type="button"
                @click="openScheduleDetail(item)"
              >
                <span>{{ formatShortDate(item.startTime) }} {{ formatTime(item.startTime) }}</span>
                <strong>{{ item.projectName || item.usagePurpose }}</strong>
                <small>{{ collectTeacherNames(item).join('、') }}</small>
              </button>
            </div>
          </article>
        </div>

        <el-table v-else :data="scheduleList" border stripe>
          <el-table-column prop="labRoomName" label="实验室" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatScheduleRoom(row) }}
            </template>
          </el-table-column>
          <el-table-column label="使用时段" min-width="280">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请教师" width="130" />
          <el-table-column label="共同使用教师" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <el-space wrap v-if="participantNames(row).length">
                <el-tag
                  v-for="participantName in participantNames(row)"
                  :key="`${row.applicationId}-${participantName}`"
                  type="info"
                  effect="plain"
                >
                  {{ participantName }}
                </el-tag>
              </el-space>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="projectName" label="课程/项目" min-width="160" show-overflow-tooltip />
          <el-table-column prop="usagePurpose" label="用途" min-width="220" show-overflow-tooltip />
          <el-table-column prop="applicationNo" label="申请单号" width="170" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openScheduleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-drawer v-model="detailDrawerVisible" title="使用日程详情" size="520px">
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="currentSchedule">
          <div class="detail-title">
            <span>{{ currentSchedule.applicationNo }}</span>
            <strong>{{ currentSchedule.projectName || currentSchedule.usagePurpose }}</strong>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="实验室">{{ formatScheduleRoom(currentSchedule) }}</el-descriptions-item>
            <el-descriptions-item label="使用时段">
              {{ formatDateTime(currentSchedule.startTime) }} 至 {{ formatDateTime(currentSchedule.endTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="申请教师">{{ currentSchedule.applicantName }}</el-descriptions-item>
            <el-descriptions-item label="共同使用教师">
              {{ participantNames(currentSchedule).length ? participantNames(currentSchedule).join('、') : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="用途">{{ currentSchedule.usagePurpose }}</el-descriptions-item>
          </el-descriptions>

          <template v-if="currentApplication">
            <el-divider>审批记录</el-divider>
            <el-timeline v-if="currentApplication.approvalRecords?.length">
              <el-timeline-item
                v-for="record in currentApplication.approvalRecords"
                :key="record.id || `${record.approvalLevel}-${record.approvalTime}`"
                :timestamp="formatDateTime(record.approvalTime)"
                :type="record.approvalResult === 1 ? 'success' : 'danger'"
              >
                <strong>{{ record.approverName || `第 ${record.approvalLevel} 级审批` }}</strong>
                <p>{{ record.approvalOpinion || '无审批意见' }}</p>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审批记录" />
          </template>
          <el-alert
            v-else-if="detailAccessDenied"
            type="info"
            show-icon
            :closable="false"
            class="detail-access-alert"
            title="已显示公开日程摘要。申请详情和审批记录仅申请人、共同使用老师、实验室管理人员或相关审批人员可查看。"
          />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Clock, OfficeBuilding, Refresh, Search, Tickets, User } from '@element-plus/icons-vue'
import { labApi } from '@/api/lab'
import { userApi } from '@/api/user'
import type { LabRoom, LabUsageApplication, LabUsageSchedule, LabUsageScheduleQuery } from '@/types/lab'
import type { User as SystemUser } from '@/types/user'

type QuickRange = 'today' | 'week' | 'month'
type ViewMode = 'timeline' | 'room' | 'table'

interface SharedScheduleGroup {
  key: string
  items: LabUsageSchedule[]
}

interface DayBucket {
  dateKey: string
  day: string
  weekday: string
  items: LabUsageSchedule[]
}

interface RoomBucket {
  key: string
  roomCode: string
  roomName: string
  items: LabUsageSchedule[]
}

const loading = ref(false)
const detailLoading = ref(false)
const labRoomOptions = ref<LabRoom[]>([])
const scheduleList = ref<LabUsageSchedule[]>([])
const scheduleDateRange = ref<string[]>([])
const teacherUserOptions = ref<SystemUser[]>([])
const teacherUserLoading = ref(false)
const quickRange = ref<QuickRange>('week')
const viewMode = ref<ViewMode>('timeline')
const detailDrawerVisible = ref(false)
const currentSchedule = ref<LabUsageSchedule>()
const currentApplication = ref<LabUsageApplication>()
const detailAccessDenied = ref(false)

const queryForm = reactive<LabUsageScheduleQuery>({
  labRoomId: undefined,
  teacherId: undefined,
  startTime: undefined,
  endTime: undefined
})

const sharedGroups = computed<SharedScheduleGroup[]>(() => {
  const groupMap = new Map<string, LabUsageSchedule[]>()
  scheduleList.value.forEach(item => {
    const key = `${item.labRoomId}-${item.startTime}-${item.endTime}`
    groupMap.set(key, [...(groupMap.get(key) ?? []), item])
  })

  return Array.from(groupMap.entries())
    .filter(([, items]) => {
      const teachers = new Set(items.flatMap(item => collectTeacherNames(item)))
      return items.length > 1 || teachers.size > 1
    })
    .map(([key, items]) => ({ key, items }))
})

const dayBuckets = computed<DayBucket[]>(() => {
  const bucketMap = new Map<string, LabUsageSchedule[]>()
  sortedSchedules.value.forEach(item => {
    const dateKey = item.startTime?.slice(0, 10) || 'unknown'
    bucketMap.set(dateKey, [...(bucketMap.get(dateKey) ?? []), item])
  })

  return Array.from(bucketMap.entries()).map(([dateKey, items]) => {
    const date = parseLocalDate(dateKey)
    return {
      dateKey,
      day: date ? `${date.getMonth() + 1}/${date.getDate()}` : dateKey,
      weekday: date ? weekdayName(date) : '',
      items
    }
  })
})

const roomBuckets = computed<RoomBucket[]>(() => {
  const bucketMap = new Map<string, RoomBucket>()
  sortedSchedules.value.forEach(item => {
    const key = String(item.labRoomId)
    const bucket = bucketMap.get(key) ?? {
      key,
      roomCode: item.labRoomCode,
      roomName: item.labRoomName,
      items: []
    }
    bucket.items.push(item)
    bucketMap.set(key, bucket)
  })
  return Array.from(bucketMap.values()).sort((a, b) => b.items.length - a.items.length)
})

const sortedSchedules = computed(() => {
  return [...scheduleList.value].sort((a, b) => {
    const timeCompare = (a.startTime || '').localeCompare(b.startTime || '')
    if (timeCompare !== 0) {
      return timeCompare
    }
    return (a.labRoomName || '').localeCompare(b.labRoomName || '')
  })
})

const summary = computed(() => {
  const roomNames = new Set<string>()
  const teacherNames = new Set<string>()
  const roomUsageCount = new Map<string, number>()

  sortedSchedules.value.forEach(item => {
    const roomName = formatScheduleRoom(item)
    roomNames.add(roomName)
    roomUsageCount.set(roomName, (roomUsageCount.get(roomName) ?? 0) + 1)
    collectTeacherNames(item).forEach(name => teacherNames.add(name))
  })

  const busiestRoom = Array.from(roomUsageCount.entries()).sort((a, b) => b[1] - a[1])[0]?.[0]
  const now = new Date()
  const nextSchedule = sortedSchedules.value.find(item => {
    const start = parseLocalDate(item.startTime)
    return start ? start.getTime() >= now.getTime() : false
  })

  return {
    usedRoomCount: roomNames.size,
    teacherCount: teacherNames.size,
    sharedGroupCount: sharedGroups.value.length,
    busiestRoom,
    nextStartTime: nextSchedule ? formatShortDateTime(nextSchedule.startTime) : '',
    nextRoom: nextSchedule ? formatScheduleRoom(nextSchedule) : ''
  }
})

const dateRangeLabel = computed(() => {
  const [startTime, endTime] = scheduleDateRange.value ?? []
  if (!startTime || !endTime) {
    return '全部时段'
  }
  return `${formatShortDate(startTime)} - ${formatShortDate(endTime)}`
})

const loadLabRooms = async () => {
  const result = await labApi.getLabRoomPage({ page: 1, size: 200, status: 1 })
  labRoomOptions.value = result.list
}

const loadSchedules = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = scheduleDateRange.value ?? []
    queryForm.startTime = startTime
    queryForm.endTime = endTime
    scheduleList.value = await labApi.getLabUsageSchedules(queryForm)
  } finally {
    loading.value = false
  }
}

const searchTeacherUsers = async (keyword: string) => {
  teacherUserLoading.value = true
  try {
    const result = await userApi.getSelectableUsers({
      keyword: keyword?.trim() || '',
      page: 1,
      size: 50,
      userType: 2,
      status: 1
    })
    teacherUserOptions.value = result.list
  } finally {
    teacherUserLoading.value = false
  }
}

const handleQuickRangeChange = () => {
  scheduleDateRange.value = buildRange(quickRange.value)
  loadSchedules()
}

const handleReset = () => {
  queryForm.labRoomId = undefined
  queryForm.teacherId = undefined
  quickRange.value = 'week'
  scheduleDateRange.value = buildRange('week')
  loadSchedules()
}

const openScheduleDetail = async (item: LabUsageSchedule) => {
  currentSchedule.value = item
  currentApplication.value = undefined
  detailAccessDenied.value = false
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    currentApplication.value = await labApi.getLabUsageApplicationByIdSilently(item.applicationId)
  } catch (error) {
    console.error('加载实验室使用申请详情失败:', error)
    detailAccessDenied.value = true
  } finally {
    detailLoading.value = false
  }
}

const formatUserOption = (user: SystemUser) => {
  const dept = user.department ? ` / ${user.department}` : ''
  return `${user.realName || user.username}（${user.username}）${dept}`
}

const formatRoomOption = (room: LabRoom) => `${room.roomName}（${room.roomCode}）`

const formatScheduleRoom = (row: LabUsageSchedule) => {
  return `${row.labRoomName}（${row.labRoomCode}）`
}

const collectTeacherNames = (item: LabUsageSchedule) => {
  const names = new Set<string>()
  if (item.applicantName) {
    names.add(item.applicantName)
  }
  ;(item.participants ?? []).forEach(participant => {
    if (participant.realName) {
      names.add(participant.realName)
    }
  })
  return Array.from(names)
}

const participantNames = (item: LabUsageSchedule) => {
  const names = new Set<string>()
  ;(item.participants ?? []).forEach(participant => {
    if (participant.realName && participant.realName !== item.applicantName) {
      names.add(participant.realName)
    }
  })
  return Array.from(names)
}

const visibleParticipantNames = (item: LabUsageSchedule): string[] => {
  return participantNames(item).slice(0, 3)
}

const hiddenParticipantCount = (item: LabUsageSchedule) => {
  return Math.max(participantNames(item).length - 3, 0)
}

const isSharedSchedule = (item: LabUsageSchedule) => {
  const key = `${item.labRoomId}-${item.startTime}-${item.endTime}`
  return sharedGroups.value.some(group => group.key === key)
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const formatShortDateTime = (value?: string) => {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

const formatShortDate = (value?: string) => {
  if (!value) return '-'
  return value.slice(5, 10)
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(11, 16)
}

const parseLocalDate = (value?: string) => {
  if (!value) {
    return undefined
  }
  const normalized = value.includes('T') ? value : `${value}T00:00:00`
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? undefined : date
}

const weekdayName = (date: Date) => {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
}

const toDateTimeValue = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const startOfDay = (date: Date) => {
  const value = new Date(date)
  value.setHours(0, 0, 0, 0)
  return value
}

const endOfDay = (date: Date) => {
  const value = new Date(date)
  value.setHours(23, 59, 59, 0)
  return value
}

const buildRange = (range: QuickRange) => {
  const now = new Date()
  if (range === 'today') {
    return [toDateTimeValue(startOfDay(now)), toDateTimeValue(endOfDay(now))]
  }

  if (range === 'month') {
    const start = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0)
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59)
    return [toDateTimeValue(start), toDateTimeValue(end)]
  }

  const day = now.getDay() || 7
  const start = startOfDay(now)
  start.setDate(now.getDate() - day + 1)
  const end = endOfDay(start)
  end.setDate(start.getDate() + 6)
  return [toDateTimeValue(start), toDateTimeValue(end)]
}

onMounted(async () => {
  scheduleDateRange.value = buildRange('week')
  await Promise.all([loadLabRooms(), searchTeacherUsers('')])
  await loadSchedules()
})
</script>

<style scoped>
.lab-schedule-page {
  min-height: 100%;
  padding: 20px;
  background:
    radial-gradient(circle at 12% 8%, rgba(74, 144, 128, 0.18), transparent 28%),
    linear-gradient(135deg, #f7fbf7 0%, #f4f8fb 48%, #fffaf2 100%);
}

.schedule-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 26px;
  overflow: hidden;
  border: 1px solid rgba(46, 91, 79, 0.12);
  border-radius: 24px;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.92), rgba(236, 246, 241, 0.92)),
    repeating-linear-gradient(45deg, rgba(46, 91, 79, 0.06) 0, rgba(46, 91, 79, 0.06) 1px, transparent 1px, transparent 14px);
  box-shadow: 0 18px 48px rgba(30, 72, 63, 0.08);
}

.eyebrow {
  margin: 0 0 8px;
  color: #4c7f6e;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.schedule-hero h2 {
  margin: 0;
  color: #183c33;
  font-size: 30px;
  font-weight: 850;
}

.hero-subtitle {
  max-width: 760px;
  margin: 10px 0 0;
  color: #5b6f67;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 12px;
}

.filter-card,
.content-card {
  border: 1px solid rgba(40, 93, 80, 0.1);
  border-radius: 18px;
}

.schedule-filter {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.1fr) minmax(320px, 1.4fr) auto;
  gap: 16px;
  align-items: end;
}

.schedule-filter :deep(.el-form-item) {
  margin-bottom: 0;
}

.schedule-filter :deep(.el-select),
.schedule-filter :deep(.el-date-editor) {
  width: 100%;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.summary-card {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 104px;
  padding: 18px;
  border: 1px solid rgba(41, 86, 74, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 12px 32px rgba(30, 72, 63, 0.06);
}

.summary-icon {
  display: grid;
  flex: none;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 16px;
  color: #fff;
  font-size: 22px;
}

.summary-icon.mint {
  background: linear-gradient(135deg, #1f8f74, #6bc6a9);
}

.summary-icon.blue {
  background: linear-gradient(135deg, #2f6f8f, #78b7d0);
}

.summary-icon.amber {
  background: linear-gradient(135deg, #bd7a1c, #f0bd67);
}

.summary-icon.coral {
  background: linear-gradient(135deg, #b6533f, #e9977f);
}

.summary-card span,
.summary-card small {
  display: block;
  color: #6b7c75;
}

.summary-card strong {
  display: block;
  margin: 5px 0;
  color: #163d34;
  font-size: 24px;
  line-height: 1.1;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.content-header strong {
  display: block;
  color: #1d3f36;
  font-size: 16px;
}

.content-header span {
  color: #7a8984;
  font-size: 13px;
}

.schedule-alert {
  margin-bottom: 16px;
}

.schedule-content {
  min-height: 240px;
}

.timeline-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.day-section {
  display: grid;
  grid-template-columns: 94px minmax(0, 1fr);
  gap: 16px;
}

.day-stamp {
  position: sticky;
  top: 12px;
  align-self: start;
  padding: 14px 10px;
  border-radius: 18px;
  background: #1f3d34;
  color: #fff;
  text-align: center;
  box-shadow: 0 12px 24px rgba(31, 61, 52, 0.18);
}

.day-stamp strong,
.day-stamp span,
.day-stamp small {
  display: block;
}

.day-stamp strong {
  font-size: 22px;
}

.day-stamp span {
  margin: 4px 0;
}

.day-stamp small {
  color: #b9d8cf;
}

.day-events {
  display: grid;
  gap: 12px;
}

.event-card {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  cursor: pointer;
  border: 1px solid #dce8e3;
  border-radius: 18px;
  background: #fff;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.event-card:hover {
  transform: translateY(-2px);
  border-color: #98c9b8;
  box-shadow: 0 14px 30px rgba(33, 76, 64, 0.1);
}

.event-card.shared {
  border-color: #efd59d;
  background: linear-gradient(110deg, #fffaf0, #ffffff 64%);
}

.event-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #31584e;
  font-weight: 800;
}

.event-time i {
  width: 2px;
  height: 22px;
  margin: 4px 0;
  border-radius: 999px;
  background: #b8d4ca;
}

.event-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.event-title strong {
  color: #183c33;
  font-size: 16px;
}

.event-main p {
  margin: 6px 0 10px;
  color: #65756f;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.event-meta span {
  color: #7b8b85;
  font-size: 12px;
}

.room-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
}

.room-column {
  min-height: 220px;
  padding: 14px;
  border: 1px solid #dce8e3;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fcfa, #fff);
}

.room-column-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.room-column-header strong {
  color: #193d34;
  font-size: 16px;
}

.room-column-header span {
  color: #758780;
}

.room-events {
  display: grid;
  gap: 10px;
}

.room-event {
  display: grid;
  gap: 5px;
  width: 100%;
  padding: 12px;
  cursor: pointer;
  border: 1px solid #e1ebe6;
  border-radius: 14px;
  background: #fff;
  color: inherit;
  font: inherit;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.room-event:hover {
  transform: translateX(3px);
  border-color: #91c5b2;
}

.room-event span,
.room-event small {
  color: #758780;
}

.room-event strong {
  color: #20463d;
}

.detail-drawer {
  min-height: 260px;
}

.detail-title {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eff8f4, #fff8ed);
}

.detail-title span,
.detail-title strong {
  display: block;
}

.detail-title span {
  color: #6e8079;
}

.detail-title strong {
  margin-top: 6px;
  color: #173d34;
  font-size: 18px;
}

.detail-access-alert {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .schedule-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .schedule-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .lab-schedule-page {
    padding: 12px;
  }

  .hero-actions,
  .content-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .schedule-filter,
  .summary-grid,
  .day-section {
    grid-template-columns: 1fr;
  }

  .day-stamp {
    position: static;
    text-align: left;
  }

  .event-card {
    grid-template-columns: 1fr;
  }

  .event-time {
    flex-direction: row;
    justify-content: flex-start;
    gap: 8px;
  }

  .event-time i {
    width: 18px;
    height: 2px;
    margin: 0;
  }
}
</style>
