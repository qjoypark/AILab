<template>
  <div class="stock-out-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>出库管理</span>
          <el-button v-if="canCreateStockOut" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增出库单
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="出库单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 出库单列表 -->
      <el-table :data="stockOutList" border stripe v-loading="loading">
        <el-table-column prop="stockOutCode" label="出库单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="140">
          <template #default="{ row }">
            {{ getWarehouseDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="stockOutType" label="出库类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.stockOutType === 1">领用出库</el-tag>
            <el-tag v-else-if="row.stockOutType === 2" type="warning">报废出库</el-tag>
            <el-tag v-else type="info">盘亏出库</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicationId" label="关联申请单" width="120">
          <template #default="{ row }">
            {{ row.applicationId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="120" align="right">
          <template #default="{ row }">
            {{ row.totalAmount ? `¥${row.totalAmount.toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待确认</el-tag>
            <el-tag v-else type="success">已确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="120">
          <template #default="{ row }">
            {{ getCreatorDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 0 && canConfirmStockOut"
              link
              type="success"
              @click="handleConfirm(row)"
            >
              确认出库
            </el-button>
            <el-button
              v-if="row.status === 0 && canDeleteStockOut"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
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

    <!-- 出库单表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="1000px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="stockOutForm"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="stockOutForm.warehouseId" style="width: 100%">
                <el-option
                  v-for="warehouse in warehouseList"
                  :key="warehouse.id"
                  :label="warehouse.warehouseName"
                  :value="warehouse.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出库类型" prop="stockOutType">
              <el-select v-model="stockOutForm.stockOutType" style="width: 100%">
                <el-option label="领用出库" :value="1" />
                <el-option label="报废出库" :value="2" />
                <el-option label="盘亏出库" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stockOutForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        
        <el-divider>出库明细</el-divider>
        
        <el-button
          v-if="canCreateStockOut"
          type="primary"
          size="small"
          @click="handleAddItem"
          style="margin-bottom: 10px"
        >
          添加药品
        </el-button>
        
        <el-table :data="stockOutForm.items" border>
          <el-table-column label="药品编码" width="160">
            <template #default="{ row, $index }">
              <el-input :model-value="getMaterialCodeDisplay(row)" placeholder="选择药品" readonly @click="selectMaterial($index)" />
            </template>
          </el-table-column>
          <el-table-column label="药品名称" width="180">
            <template #default="{ row, $index }">
              <el-input :model-value="getMaterialNameDisplay(row)" placeholder="选择药品" readonly @click="selectMaterial($index)" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="2" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="批次号" width="150">
            <template #default="{ row }">
              <el-input v-model="row.batchNumber" placeholder="可选" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button v-if="canCreateStockOut" link type="danger" @click="handleRemoveItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canCreateStockOut" type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="出库单详情" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ currentStockOut?.stockOutCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentStockOut ? getWarehouseDisplay(currentStockOut) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="出库类型">
          <el-tag v-if="currentStockOut?.stockOutType === 1">领用出库</el-tag>
          <el-tag v-else-if="currentStockOut?.stockOutType === 2" type="warning">报废出库</el-tag>
          <el-tag v-else type="info">盘亏出库</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentStockOut?.status === 0" type="warning">待确认</el-tag>
          <el-tag v-else type="success">已确认</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentStockOut ? getCreatorDisplay(currentStockOut) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentStockOut?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentStockOut?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>出库明细</el-divider>
      
      <el-table :data="currentStockOut?.items" border>
        <el-table-column label="药品编码">
          <template #default="{ row }">
            {{ getMaterialCodeDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column label="药品名称">
          <template #default="{ row }">
            {{ getMaterialNameDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="存放位置">
          <template #default="{ row }">
            {{ getLocationDisplay(currentStockOut, row) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" align="right" />
        <el-table-column prop="unitPrice" label="单价" align="right">
          <template #default="{ row }">
            {{ row.unitPrice ? `¥${row.unitPrice.toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" align="right">
          <template #default="{ row }">
            {{ row.totalPrice ? `¥${row.totalPrice.toFixed(2)}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="batchNumber" label="批次号" />
      </el-table>
    </el-dialog>

    <!-- 药品选择器 -->
    <MaterialSelector
      v-model="materialSelectorVisible"
      @select="handleMaterialSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import MaterialSelector from '@/components/MaterialSelector.vue'
import type { StockOut, StockOutDetail, StockOutForm, Warehouse } from '@/types/inventory'
import type { Material } from '@/types/material'
import { materialApi } from '@/api/material'
import { useUserStore } from '@/stores/user'
import { INVENTORY_STOCK_OUT_PERMISSIONS } from '@/constants/permissions'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const materialSelectorVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const stockOutList = ref<StockOut[]>([])
const warehouseList = ref<Warehouse[]>([])
const warehouseNameMap = ref<Record<number, string>>({})
const userNameMap = ref<Record<number, string>>({})
const materialInfoMap = ref<Record<number, { materialCode?: string; materialName?: string }>>({})
const locationNameMap = ref<Record<string, string>>({})
const currentStockOut = ref<StockOut>()
const currentItemIndex = ref<number>()
const total = ref(0)
const canCreateStockOut = computed(() => userStore.hasPermission('inventory:stock-out:create'))
const canConfirmStockOut = computed(() => userStore.hasPermission('inventory:stock-out:confirm'))
const canDeleteStockOut = computed(() => userStore.hasPermission('inventory:stock-out:delete'))
const canAccessStockOutPage = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_OUT_PERMISSIONS]))
const canReadMaterial = computed(() => userStore.hasPermission('material:list'))

const queryForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  size: 10
})

const stockOutForm = reactive<StockOutForm>({
  warehouseId: 0,
  stockOutType: 1,
  remark: '',
  items: []
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  stockOutType: [{ required: true, message: '请选择出库类型', trigger: 'change' }]
}

const formatIdLabel = (prefix: string, value?: number) => {
  if (!value) return '-'
  return `${prefix}#${value}`
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 19)
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

const resolveUserName = async (userId?: number) => {
  if (!userId) return ''
  if (userNameMap.value[userId]) {
    return userNameMap.value[userId]
  }

  const currentUser = userStore.userInfo
  const userName =
    currentUser?.id === userId
      ? (currentUser.realName || currentUser.username || String(userId))
      : String(userId)
  userNameMap.value[userId] = userName
  return userName
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
      materialName: material.materialName
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

const getWarehouseDisplay = (stockOut: StockOut) => {
  return (
    stockOut.warehouseName ||
    warehouseNameMap.value[stockOut.warehouseId] ||
    formatIdLabel('仓库', stockOut.warehouseId)
  )
}

const getCreatorDisplay = (stockOut: StockOut) => {
  return (
    stockOut.createdByName ||
    (stockOut.createdBy ? userNameMap.value[stockOut.createdBy] : '') ||
    (stockOut.operatorId ? userNameMap.value[stockOut.operatorId] : '') ||
    formatIdLabel('用户', stockOut.createdBy ?? stockOut.operatorId)
  )
}

const getMaterialCodeDisplay = (detail: StockOutDetail) => {
  return (
    detail.materialCode ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialCode : '') ||
    '-'
  )
}

const getMaterialNameDisplay = (detail: StockOutDetail) => {
  return (
    detail.materialName ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialName : '') ||
    '-'
  )
}

const getLocationDisplay = (stockOut: StockOut | undefined, detail: StockOutDetail) => {
  if (detail.locationName) {
    return detail.locationName
  }
  if (!stockOut || !detail.locationId) {
    return '-'
  }
  const cacheKey = getLocationCacheKey(stockOut.warehouseId, detail.locationId)
  return locationNameMap.value[cacheKey] || `库位#${detail.locationId}`
}

const enrichStockOut = async (stockOut: StockOut) => {
  const enriched: StockOut = {
    ...stockOut,
    warehouseName: stockOut.warehouseName || warehouseNameMap.value[stockOut.warehouseId],
    createdTime: formatDateTime(stockOut.createdTime || (stockOut as any).outDate)
  }

  const creatorId = enriched.createdBy ?? enriched.operatorId
  if (!enriched.createdByName && creatorId) {
    enriched.createdByName = await resolveUserName(creatorId)
  }
  if (!enriched.createdByName && creatorId) {
    enriched.createdByName = formatIdLabel('用户', creatorId)
  }
  if (!enriched.createdByName) {
    enriched.createdByName = '-'
  }

  if (enriched.items && enriched.items.length > 0) {
    for (const detail of enriched.items) {
      if ((!detail.materialCode || !detail.materialName) && detail.materialId) {
        const materialInfo = await resolveMaterialInfo(detail.materialId)
        detail.materialCode = detail.materialCode || materialInfo.materialCode
        detail.materialName = detail.materialName || materialInfo.materialName
      }
      detail.totalPrice = detail.totalPrice ?? (detail as any).totalAmount
      if (!detail.locationName && detail.locationId) {
        detail.locationName = await resolveLocationName(enriched.warehouseId, detail.locationId)
      }
    }
  }

  return enriched
}

const loadStockOutList = async () => {
  if (!canAccessStockOutPage.value) {
    stockOutList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const res = await inventoryApi.getStockOutList(queryForm)
    stockOutList.value = await Promise.all(res.list.map(enrichStockOut))
    total.value = res.total
  } catch (error) {
    console.error('加载出库单列表失败:', error)
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
  loadStockOutList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  handleQuery()
}

const handleAdd = () => {
  if (!canCreateStockOut.value) {
    ElMessage.warning('没有出库单新增权限')
    return
  }

  dialogTitle.value = '新增出库单'
  Object.assign(stockOutForm, {
    warehouseId: 0,
    stockOutType: 1,
    remark: '',
    items: []
  })
  dialogVisible.value = true
}

const handleAddItem = () => {
  if (!canCreateStockOut.value) {
    ElMessage.warning('没有出库单新增权限')
    return
  }

  stockOutForm.items.push({
    materialId: 0,
    materialCode: '',
    materialName: '',
    quantity: 0,
    batchNumber: ''
  })
}

const handleRemoveItem = (index: number) => {
  if (!canCreateStockOut.value) {
    ElMessage.warning('没有出库单新增权限')
    return
  }

  stockOutForm.items.splice(index, 1)
}

const selectMaterial = (index: number) => {
  currentItemIndex.value = index
  materialSelectorVisible.value = true
}

const handleMaterialSelected = (material: Material) => {
  if (currentItemIndex.value !== undefined) {
    const item = stockOutForm.items[currentItemIndex.value]
    item.materialId = material.id
    const materialCode = material.materialCode || (material as any).code || ''
    const materialName = material.materialName || (material as any).name || ''
    item.materialCode = materialCode
    item.materialName = materialName
    item.unitPrice = material.unitPrice

    materialInfoMap.value[material.id] = {
      materialCode,
      materialName
    }
  }
}

const handleSubmit = async () => {
  if (!canCreateStockOut.value) {
    ElMessage.warning('没有出库单新增权限')
    return
  }

  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (stockOutForm.items.length === 0) {
      ElMessage.warning('请添加出库明细')
      return
    }
    
    submitting.value = true
    try {
      await inventoryApi.createStockOut(stockOutForm)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadStockOutList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = async (row: StockOut) => {
  try {
    const stockOut = await inventoryApi.getStockOutById(row.id)
    currentStockOut.value = await enrichStockOut(stockOut)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载出库单详情失败:', error)
  }
}

const handleConfirm = async (row: StockOut) => {
  if (!canConfirmStockOut.value) {
    ElMessage.warning('没有出库单确认权限')
    return
  }

  await ElMessageBox.confirm('确定要确认出库吗？确认后将扣减库存数量。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await inventoryApi.confirmStockOut(row.id)
    ElMessage.success('确认出库成功')
    loadStockOutList()
  } catch (error) {
    console.error('确认出库失败:', error)
  }
}

const handleDelete = async (row: StockOut) => {
  if (!canDeleteStockOut.value) {
    ElMessage.warning('没有出库单删除权限')
    return
  }

  await ElMessageBox.confirm('确定要删除该出库单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await inventoryApi.deleteStockOut(row.id)
    ElMessage.success('删除成功')
    loadStockOutList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(async () => {
  await loadWarehouseList()
  await loadStockOutList()
})
</script>

<style scoped>
.stock-out-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
