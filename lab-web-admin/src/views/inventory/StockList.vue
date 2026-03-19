<template>
  <div class="stock-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存查询</span>
          <div class="header-actions">
            <el-button v-if="canAccessStockIn" type="primary" @click="goToStockIn">入库</el-button>
            <el-button v-if="canAccessStockOut" type="success" @click="goToStockOut">出库</el-button>
            <el-button v-if="canAccessStockCheck" type="warning" @click="goToStockCheck">盘点</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" class="query-keyword-input" placeholder="药品编码/名称/仓库" clearable />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select
            v-model="queryForm.warehouseId"
            v-adaptive-select-width="['全部', ...warehouseList.map(warehouse => warehouse.warehouseName)]"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option
              v-for="warehouse in warehouseList"
              :key="warehouse.id"
              :label="warehouse.warehouseName"
              :value="warehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="queryForm.lowStock">仅显示低库存</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 库存列表 -->
      <el-table :data="stockList" border stripe v-loading="loading" class="data-table">
        <el-table-column prop="materialCode" label="药品编码" width="120" />
        <el-table-column prop="materialName" label="药品名称" min-width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="locationName" label="存储位置" width="120" />
        <el-table-column prop="batchNumber" label="批次号" width="120" />
        <el-table-column prop="quantity" label="库存数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ 'low-stock': isLowStock(row) }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="availableQuantity" label="可用数量" width="100" align="right" />
        <el-table-column prop="lockedQuantity" label="锁定数量" width="100" align="right" />
        <el-table-column prop="totalValue" label="库存金额" width="120" align="right">
          <template #default="{ row }">
            {{ row.totalValue ? `¥${row.totalValue.toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" width="120">
          <template #default="{ row }">
            <span :class="{ 'expiring-soon': isExpiringSoon(row) }">
              {{ row.expiryDate || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>

    <!-- 库存明细对话框 -->
    <el-dialog v-model="detailDialogVisible" title="库存明细" width="900px">
      <el-table :data="stockDetailList" border class="detail-table">
        <el-table-column prop="warehouseName" label="仓库" />
        <el-table-column prop="locationName" label="存储位置" />
        <el-table-column prop="batchNumber" label="批次号" />
        <el-table-column prop="quantity" label="数量" align="right" />
        <el-table-column prop="availableQuantity" label="可用" align="right" />
        <el-table-column prop="lockedQuantity" label="锁定" align="right" />
        <el-table-column prop="productionDate" label="生产日期" />
        <el-table-column prop="expiryDate" label="有效期" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import type { StockInventory, StockQuery, Warehouse } from '@/types/inventory'
import { materialApi } from '@/api/material'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  INVENTORY_STOCK_CHECK_PERMISSIONS,
  INVENTORY_STOCK_IN_PERMISSIONS,
  INVENTORY_STOCK_OUT_PERMISSIONS
} from '@/constants/permissions'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const detailDialogVisible = ref(false)
const stockList = ref<StockInventory[]>([])
const stockDetailList = ref<StockInventory[]>([])
const warehouseList = ref<Warehouse[]>([])
const warehouseNameMap = ref<Record<number, string>>({})
const materialInfoMap = ref<Record<number, { materialCode?: string; materialName?: string; safetyStock?: number }>>({})
const locationNameMap = ref<Record<string, string>>({})
const total = ref(0)
const DEFAULT_LOW_STOCK_THRESHOLD = 10
const canAccessStockIn = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_IN_PERMISSIONS]))
const canAccessStockOut = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_OUT_PERMISSIONS]))
const canAccessStockCheck = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_CHECK_PERMISSIONS]))
const canReadMaterial = computed(() => userStore.hasPermission('material:list'))

const queryForm = reactive<StockQuery>({
  keyword: '',
  warehouseId: -1,
  lowStock: false,
  page: 1,
  size: 10
})

const formatIdLabel = (prefix: string, id?: number) => {
  if (!id) return '-'
  return `${prefix}#${id}`
}

const rebuildWarehouseNameMap = () => {
  const map: Record<number, string> = {}
  warehouseList.value.forEach((warehouse) => {
    map[warehouse.id] = warehouse.warehouseName
  })
  warehouseNameMap.value = map
}

const getLocationCacheKey = (warehouseId?: number, locationId?: number) => {
  return `${warehouseId ?? 0}_${locationId ?? 0}`
}

const resolveMaterialInfo = async (materialId?: number) => {
  if (!materialId) return {}
  if (materialInfoMap.value[materialId]) {
    return materialInfoMap.value[materialId]
  }
  if (!canReadMaterial.value) {
    materialInfoMap.value[materialId] = {}
    return {}
  }

  try {
    const material = await materialApi.getMaterialById(materialId)
    const info = {
      materialCode: material.materialCode,
      materialName: material.materialName,
      safetyStock: material.safetyStock
    }
    materialInfoMap.value[materialId] = info
    return info
  } catch {
    materialInfoMap.value[materialId] = {}
    return {}
  }
}

const resolveLocationName = async (warehouseId?: number, locationId?: number) => {
  if (!warehouseId || !locationId) return ''
  const cacheKey = getLocationCacheKey(warehouseId, locationId)
  if (locationNameMap.value[cacheKey]) {
    return locationNameMap.value[cacheKey]
  }

  try {
    const locations = await inventoryApi.getLocationList(warehouseId)
    locations.forEach((location) => {
      const key = getLocationCacheKey(warehouseId, location.id)
      locationNameMap.value[key] = location.locationName
    })
    return locationNameMap.value[cacheKey] || ''
  } catch {
    locationNameMap.value[cacheKey] = ''
    return ''
  }
}

const enrichStockRow = async (row: StockInventory) => {
  const enriched: StockInventory = { ...row }

  const materialInfo = await resolveMaterialInfo(enriched.materialId)
  enriched.materialCode = enriched.materialCode || materialInfo.materialCode
  enriched.materialName = enriched.materialName || materialInfo.materialName

  if (!enriched.materialCode) {
    enriched.materialCode = formatIdLabel('药品', enriched.materialId)
  }
  if (!enriched.materialName) {
    enriched.materialName = formatIdLabel('药品', enriched.materialId)
  }

  enriched.warehouseName =
    enriched.warehouseName ||
    warehouseNameMap.value[enriched.warehouseId] ||
    formatIdLabel('仓库', enriched.warehouseId)

  const locationId = enriched.locationId
  if (!enriched.locationName && locationId) {
    enriched.locationName = await resolveLocationName(enriched.warehouseId, locationId)
  }
  if (!enriched.locationName && locationId) {
    enriched.locationName = formatIdLabel('库位', locationId)
  }
  if (!enriched.locationName) {
    enriched.locationName = '-'
  }

  return enriched
}

const loadStockList = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getStockList(queryForm)
    stockList.value = await Promise.all(res.list.map(enrichStockRow))
    total.value = res.total
  } catch (error) {
    console.error('加载库存列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  try {
    warehouseList.value = await inventoryApi.getWarehouseList()
    rebuildWarehouseNameMap()
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadStockList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.warehouseId = -1
  queryForm.lowStock = false
  handleQuery()
}

const handleViewDetail = async (row: StockInventory) => {
  try {
    const detailList = await inventoryApi.getStockDetail(row.materialId)
    stockDetailList.value = await Promise.all(detailList.map(enrichStockRow))
    detailDialogVisible.value = true
  } catch (error) {
    console.error('加载库存明细失败:', error)
  }
}

const goToStockIn = () => {
  if (!canAccessStockIn.value) {
    ElMessage.warning('暂无入库模块访问权限')
    return
  }
  router.push('/inventory/stock-in')
}

const goToStockOut = () => {
  if (!canAccessStockOut.value) {
    ElMessage.warning('暂无出库模块访问权限')
    return
  }
  router.push('/inventory/stock-out')
}

const goToStockCheck = () => {
  if (!canAccessStockCheck.value) {
    ElMessage.warning('暂无盘点模块访问权限')
    return
  }
  router.push('/inventory/stock-check')
}

const isLowStock = (row: StockInventory) => {
  const materialSafetyStock = row.materialId ? materialInfoMap.value[row.materialId]?.safetyStock : undefined
  const threshold = materialSafetyStock && materialSafetyStock > 0 ? materialSafetyStock : DEFAULT_LOW_STOCK_THRESHOLD
  const availableQuantity = row.availableQuantity ?? 0
  return availableQuantity <= threshold
}

const isExpiringSoon = (row: StockInventory) => {
  if (!row.expiryDate) return false
  const expiryDate = new Date(row.expiryDate)
  const today = new Date()
  const daysUntilExpiry = Math.floor((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  return daysUntilExpiry <= 30 && daysUntilExpiry >= 0
}

onMounted(async () => {
  await loadWarehouseList()
  await loadStockList()
})
</script>

<style scoped>
.stock-list {
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.query-form {
  margin-bottom: 10px;
  padding: 14px 14px 2px;
  border: 1px solid #e9f0fb;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.data-table :deep(.el-table__row:hover > td) {
  background: #f7fbff !important;
}

.detail-table {
  border-radius: 10px;
  overflow: hidden;
}

.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.low-stock {
  color: #ef4444;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.12);
}

.expiring-soon {
  color: #f59e0b;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.15);
}
</style>
