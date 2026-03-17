<template>
  <div class="hazardous-ledger">
    <el-card>
      <template #header>
        <span>危化品台账报表</span>
      </template>

      <!-- 查询表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="药品">
          <el-input v-model="queryForm.materialId" placeholder="药品ID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport" :loading="exporting">
            <el-icon><Download /></el-icon>
            导出Excel
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 台账列表 -->
      <el-table :data="ledgerList" border stripe v-loading="loading">
        <el-table-column prop="materialName" label="药品名称" min-width="150" fixed="left" />
        <el-table-column prop="casNumber" label="CAS号" width="120" />
        <el-table-column prop="dangerCategory" label="危险类别" width="120" />
        <el-table-column prop="controlType" label="管控类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.controlType === 0">非管控</el-tag>
            <el-tag v-else-if="row.controlType === 1" type="warning">易制毒</el-tag>
            <el-tag v-else-if="row.controlType === 2" type="danger">易制爆</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="openingStock" label="期初库存" width="100" align="right" />
        <el-table-column prop="totalStockIn" label="入库总量" width="100" align="right" />
        <el-table-column prop="totalStockOut" label="出库总量" width="100" align="right" />
        <el-table-column prop="closingStock" label="期末库存" width="100" align="right" />
        <el-table-column prop="unreturnedQuantity" label="未归还" width="100" align="right" />
        <el-table-column prop="actualStock" label="实际库存" width="100" align="right" />
        <el-table-column prop="discrepancyRate" label="账实差异率" width="120" align="right">
          <template #default="{ row }">
            <span :class="getDiscrepancyClass(row)">
              {{ row.discrepancyRate ? `${row.discrepancyRate}%` : '-' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 统计信息 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="6">
          <el-statistic title="危化品种类" :value="ledgerList.length" suffix="种" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="期末库存总量" :value="totalClosingStock" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="未归还总量" :value="totalUnreturned" :precision="2" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="异常药品数" :value="abnormalCount" suffix="种" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { approvalApi } from '@/api/approval'

interface LedgerItem {
  materialId: number
  materialName: string
  casNumber?: string
  dangerCategory?: string
  controlType: number
  unit: string
  openingStock: number
  totalStockIn: number
  totalStockOut: number
  closingStock: number
  unreturnedQuantity: number
  actualStock: number
  discrepancyRate?: number
}

const loading = ref(false)
const exporting = ref(false)
const ledgerList = ref<LedgerItem[]>([])
const dateRange = ref<[string, string]>()

const queryForm = reactive({
  startDate: '',
  endDate: '',
  materialId: undefined as number | undefined
})

const totalClosingStock = computed(() => {
  return ledgerList.value.reduce((sum, item) => sum + item.closingStock, 0)
})

const totalUnreturned = computed(() => {
  return ledgerList.value.reduce((sum, item) => sum + item.unreturnedQuantity, 0)
})

const abnormalCount = computed(() => {
  return ledgerList.value.filter(item => 
    item.discrepancyRate && Math.abs(item.discrepancyRate) > 5
  ).length
})

const loadLedger = async () => {
  loading.value = true
  try {
    const params = {
      startDate: queryForm.startDate,
      endDate: queryForm.endDate,
      materialId: queryForm.materialId
    }
    ledgerList.value = await approvalApi.getHazardousLedger(params)
  } catch (error) {
    console.error('加载台账失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  if (dateRange.value) {
    queryForm.startDate = dateRange.value[0]
    queryForm.endDate = dateRange.value[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
  loadLedger()
}

const handleReset = () => {
  dateRange.value = undefined
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.materialId = undefined
  loadLedger()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const params = {
      startDate: queryForm.startDate,
      endDate: queryForm.endDate,
      materialId: queryForm.materialId
    }
    const blob = await approvalApi.exportHazardousLedger(params)
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `危化品台账_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  } finally {
    exporting.value = false
  }
}

const getDiscrepancyClass = (row: LedgerItem) => {
  if (!row.discrepancyRate) return ''
  const rate = Math.abs(row.discrepancyRate)
  if (rate > 5) return 'text-danger'
  if (rate > 2) return 'text-warning'
  return ''
}

onMounted(() => {
  loadLedger()
})
</script>

<style scoped>
.hazardous-ledger {
  padding: 20px;
}

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.text-warning {
  color: #e6a23c;
  font-weight: bold;
}
</style>
