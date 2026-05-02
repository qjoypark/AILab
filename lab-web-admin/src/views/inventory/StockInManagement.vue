<template>
  <div class="stock-in-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>入库管理</span>
          <div class="header-actions">
            <el-button v-if="canCreateStockIn" @click="handleDownloadImportTemplate">
              下载导入模板
            </el-button>
            <el-upload
              v-if="canCreateStockIn"
              :show-file-list="false"
              :auto-upload="false"
              accept=".xlsx,.xls"
              :on-change="handleImportFileChange"
              :disabled="importing"
            >
              <el-button type="warning" :loading="importing">Excel导入</el-button>
            </el-upload>
            <el-button v-if="canCreateStockIn" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增入库单
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" class="query-keyword-input" placeholder="入库单号" clearable />
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
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            v-adaptive-select-width="['全部', '待确认', '已确认']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="queryForm.createdTimeRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <template v-if="canListStockIn">
      <!-- 入库单列表 -->
      <el-table :data="stockInList" border stripe v-loading="loading" class="data-table">
        <el-table-column prop="stockInCode" label="入库单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="140">
          <template #default="{ row }">
            {{ getWarehouseDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="stockInType" label="入库类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.stockInType === 1">采购入库</el-tag>
            <el-tag v-else-if="row.stockInType === 2" type="warning">退货入库</el-tag>
            <el-tag v-else-if="row.stockInType === 3" type="info">盘盈入库</el-tag>
            <el-tag v-else type="success">归还入库</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" />
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
              v-if="row.status === 0 && canConfirmStockIn"
              link
              type="success"
              @click="handleConfirm(row)"
            >
              确认入库
            </el-button>
            <el-button
              v-if="row.status === 0 && canDeleteStockIn"
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
      </template>

      <el-alert
        v-else
        type="warning"
        show-icon
        :closable="false"
        title="当前角色缺少入库列表查看权限，无法加载入库单列表。"
      />
    </el-card>

    <!-- 入库单表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="1000px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="stockInForm"
        :rules="rules"
        label-width="100px"
        class="stock-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="stockInForm.warehouseId" style="width: 100%">
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
            <el-form-item label="入库类型" prop="stockInType">
              <el-select v-model="stockInForm.stockInType" style="width: 100%">
                <el-option label="采购入库" :value="1" />
                <el-option label="退货入库" :value="2" />
                <el-option label="盘盈入库" :value="3" />
                <el-option label="归还入库" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stockInForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        
        <el-divider>入库明细</el-divider>
        
        <el-button
          v-if="canCreateStockIn"
          type="primary"
          size="small"
          @click="handleAddItem"
          class="add-item-btn"
        >
          添加药品
        </el-button>
        
        <el-table :data="stockInForm.items" border>
          <el-table-column label="药品编码" width="160">
            <template #default="{ row, $index }">
              <el-input
                :model-value="getMaterialCodeDisplay(row)"
                placeholder="选择药品"
                readonly
                @click="selectMaterial($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="药品名称" width="180">
            <template #default="{ row, $index }">
              <el-input
                :model-value="getMaterialNameDisplay(row)"
                placeholder="选择药品"
                readonly
                @click="selectMaterial($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="100">
            <template #default="{ row }">
              {{ row.unit || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :precision="2" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="批次号" width="150">
            <template #default="{ row }">
              <el-input v-model="row.batchNumber" />
            </template>
          </el-table-column>
          <el-table-column label="生产日期" width="150">
            <template #default="{ row }">
              <el-date-picker v-model="row.productionDate" type="date" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="有效期" width="150">
            <template #default="{ row }">
              <el-date-picker v-model="row.expiryDate" type="date" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button v-if="canCreateStockIn" link type="danger" @click="handleRemoveItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canCreateStockIn" type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="入库单详情" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ currentStockIn?.stockInCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentStockIn ? getWarehouseDisplay(currentStockIn) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">
          <el-tag v-if="currentStockIn?.stockInType === 1">采购入库</el-tag>
          <el-tag v-else-if="currentStockIn?.stockInType === 2" type="warning">退货入库</el-tag>
          <el-tag v-else-if="currentStockIn?.stockInType === 3" type="info">盘盈入库</el-tag>
          <el-tag v-else type="success">归还入库</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentStockIn?.status === 0" type="warning">待确认</el-tag>
          <el-tag v-else type="success">已确认</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentStockIn ? getCreatorDisplay(currentStockIn) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentStockIn?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentStockIn?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>入库明细</el-divider>
      
      <el-table :data="currentStockIn?.items" border>
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
        <el-table-column prop="expiryDate" label="有效期" />
      </el-table>
    </el-dialog>

    <MaterialSelector
      v-model="materialSelectorVisible"
      @select="handleMaterialSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import type { StockIn, StockInForm, StockInDetail, Warehouse } from '@/types/inventory'
import type { Material } from '@/types/material'
import { materialApi } from '@/api/material'
import MaterialSelector from '@/components/MaterialSelector.vue'
import { useUserStore } from '@/stores/user'
import { INVENTORY_STOCK_IN_PERMISSIONS } from '@/constants/permissions'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const stockInList = ref<StockIn[]>([])
const warehouseList = ref<Warehouse[]>([])
const warehouseNameMap = ref<Record<number, string>>({})
const userNameMap = ref<Record<number, string>>({})
const materialInfoMap = ref<Record<number, { materialCode?: string; materialName?: string; unit?: string }>>({})
const currentStockIn = ref<StockIn>()
const materialSelectorVisible = ref(false)
const currentMaterialIndex = ref<number>(-1)
const total = ref(0)
const canCreateStockIn = computed(() => userStore.hasPermission('inventory:stock-in:create'))
const canListStockIn = computed(() => userStore.hasPermission('inventory:stock-in:list'))
const canConfirmStockIn = computed(() => userStore.hasPermission('inventory:stock-in:confirm'))
const canDeleteStockIn = computed(() => userStore.hasPermission('inventory:stock-in:delete'))
const canAccessStockInPage = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_IN_PERMISSIONS]))
const canReadMaterial = computed(() => userStore.hasPermission('material:list'))

const queryForm = reactive({
  keyword: '',
  warehouseId: -1 as number,
  status: -1 as number,
  createdTimeRange: [] as string[],
  page: 1,
  size: 10
})

const stockInForm = reactive<StockInForm>({
  warehouseId: 0,
  stockInType: 1,
  remark: '',
  items: []
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  stockInType: [{ required: true, message: '请选择入库类型', trigger: 'change' }]
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

const getDefaultWarehouseId = () => {
  const activeWarehouse = warehouseList.value.find((warehouse) => warehouse.status === 1)
  return activeWarehouse?.id ?? warehouseList.value[0]?.id ?? 0
}

const resolveUserName = async (userId?: number) => {
  if (!userId) return ''
  if (userNameMap.value[userId]) {
    return userNameMap.value[userId]
  }

  const currentUser = userStore.userInfo
  const userName =
    currentUser?.id === userId
      ? (currentUser.realName || currentUser.username || '')
      : ''
  if (!userName) {
    return ''
  }
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
      materialName: material.materialName,
      unit: material.unit
    }
    materialInfoMap.value[materialId] = info
    return info
  } catch {
    materialInfoMap.value[materialId] = {}
    return {}
  }
}

const getWarehouseDisplay = (stockIn: StockIn) => {
  return (
    stockIn.warehouseName ||
    warehouseNameMap.value[stockIn.warehouseId] ||
    formatIdLabel('仓库', stockIn.warehouseId)
  )
}

const getCreatorDisplay = (stockIn: StockIn) => {
  return (
    stockIn.createdByName ||
    (stockIn.createdBy ? userNameMap.value[stockIn.createdBy] : '') ||
    (stockIn.operatorId ? userNameMap.value[stockIn.operatorId] : '') ||
    '-'
  )
}

const getMaterialCodeDisplay = (detail: StockInDetail) => {
  return (
    detail.materialCode ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialCode : '') ||
    '-'
  )
}

const getMaterialNameDisplay = (detail: StockInDetail) => {
  return (
    detail.materialName ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialName : '') ||
    '-'
  )
}

const enrichStockIn = async (stockIn: StockIn) => {
  const enriched: StockIn = {
    ...stockIn,
    warehouseName: stockIn.warehouseName || warehouseNameMap.value[stockIn.warehouseId],
    createdTime: formatDateTime(stockIn.createdTime || (stockIn as any).inDate),
    updatedTime: formatDateTime(stockIn.updatedTime)
  }

  const creatorId = enriched.createdBy ?? enriched.operatorId
  if (!enriched.createdByName && creatorId) {
    enriched.createdByName = await resolveUserName(creatorId)
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
        detail.unit = detail.unit || materialInfo.unit
      }
      detail.totalPrice = detail.totalPrice ?? (detail as any).totalAmount
      detail.expiryDate = detail.expiryDate ?? (detail as any).expireDate
    }
  }

  return enriched
}

const loadStockInList = async () => {
  if (!canAccessStockInPage.value || !canListStockIn.value) {
    stockInList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const [createdDateStart, createdDateEnd] = queryForm.createdTimeRange
    const createdTimeStart = createdDateStart ? `${createdDateStart} 00:00:00` : undefined
    const createdTimeEnd = createdDateEnd ? `${createdDateEnd} 23:59:59` : undefined
    const res = await inventoryApi.getStockInList({
      keyword: queryForm.keyword,
      warehouseId: queryForm.warehouseId,
      status: queryForm.status,
      page: queryForm.page,
      size: queryForm.size,
      createdTimeStart,
      createdTimeEnd
    })
    stockInList.value = await Promise.all(res.list.map(enrichStockIn))
    total.value = res.total
  } catch (error) {
    console.error('加载入库单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadWarehouseList = async () => {
  try {
    warehouseList.value = await inventoryApi.getWarehouseList()
    rebuildWarehouseNameMap()
    if (stockInForm.warehouseId === 0) {
      stockInForm.warehouseId = getDefaultWarehouseId()
    }
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadStockInList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.warehouseId = -1
  queryForm.status = -1
  queryForm.createdTimeRange = []
  handleQuery()
}

const handleDownloadImportTemplate = async () => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  try {
    const blob = await inventoryApi.downloadStockInImportTemplate()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `入库导入模板_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载导入模板失败:', error)
  }
}

const handleImportFileChange = async (uploadFile: UploadFile) => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  const rawFile = uploadFile.raw as File | undefined
  if (!rawFile) {
    ElMessage.warning('请选择有效的Excel文件')
    return
  }

  const fileName = rawFile.name.toLowerCase()
  if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
    ElMessage.warning('仅支持 .xlsx/.xls 文件')
    return
  }

  importing.value = true
  try {
    const preview = await inventoryApi.importStockInExcel(rawFile)

    const previewItems: StockInDetail[] = (preview.items ?? []).map((item) => ({
      materialId: item.materialId,
      materialCode: '',
      materialName: '',
      unit: '',
      quantity: Number(item.quantity ?? 0),
      unitPrice: Number(item.unitPrice ?? 0),
      batchNumber: item.batchNumber || '',
      productionDate: item.productionDate || '',
      expiryDate: item.expiryDate || ''
    }))

    for (const item of previewItems) {
      if (item.materialId) {
        const materialInfo = await resolveMaterialInfo(item.materialId)
        item.materialCode = materialInfo.materialCode || ''
        item.materialName = materialInfo.materialName || ''
        item.unit = materialInfo.unit || ''
      }
    }

    dialogTitle.value = '新增入库单（Excel预填）'
    Object.assign(stockInForm, {
      warehouseId: preview.warehouseId || getDefaultWarehouseId(),
      stockInType: preview.stockInType || 1,
      remark: preview.remark || '',
      items: previewItems
    })
    dialogVisible.value = true
    ElMessage.success('Excel解析完成，请确认后提交入库单')
  } catch (error) {
    console.error('Excel导入失败:', error)
  } finally {
    importing.value = false
  }
}

const handleAdd = () => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  dialogTitle.value = '新增入库单'
  Object.assign(stockInForm, {
    warehouseId: getDefaultWarehouseId(),
    stockInType: 1,
    remark: '',
    items: []
  })
  dialogVisible.value = true
}

const handleAddItem = () => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  stockInForm.items.push({
    materialId: 0,
    materialCode: '',
    materialName: '',
    unit: '',
    quantity: 0,
    unitPrice: 0,
    batchNumber: '',
    productionDate: '',
    expiryDate: ''
  })
}

const handleRemoveItem = (index: number) => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  stockInForm.items.splice(index, 1)
}

const selectMaterial = (index: number) => {
  currentMaterialIndex.value = index
  materialSelectorVisible.value = true
}

const handleMaterialSelected = (material: Material) => {
  const row = stockInForm.items[currentMaterialIndex.value]
  if (!row) {
    return
  }

  row.materialId = material.id
  const materialCode = material.materialCode || (material as any).code || ''
  const materialName = material.materialName || (material as any).name || ''

  row.materialCode = materialCode
  row.materialName = materialName
  row.unit = material.unit
  row.unitPrice = row.unitPrice && row.unitPrice > 0 ? row.unitPrice : (material.unitPrice ?? 0)
  row.quantity = row.quantity > 0 ? row.quantity : 1

  materialInfoMap.value[material.id] = {
    materialCode,
    materialName,
    unit: material.unit
  }
}

const handleSubmit = async () => {
  if (!canCreateStockIn.value) {
    ElMessage.warning('没有入库单新增权限')
    return
  }

  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (stockInForm.items.length === 0) {
      ElMessage.warning('请添加入库明细')
      return
    }
    
    submitting.value = true
    try {
      await inventoryApi.createStockIn(stockInForm)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadStockInList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleView = async (row: StockIn) => {
  try {
    const stockIn = await inventoryApi.getStockInById(row.id)
    currentStockIn.value = await enrichStockIn(stockIn)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载入库单详情失败:', error)
  }
}

const handleConfirm = async (row: StockIn) => {
  if (!canConfirmStockIn.value) {
    ElMessage.warning('没有入库单确认权限')
    return
  }

  await ElMessageBox.confirm('确定要确认入库吗？确认后将更新库存数量。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await inventoryApi.confirmStockIn(row.id)
    ElMessage.success('确认入库成功')
    loadStockInList()
  } catch (error) {
    console.error('确认入库失败:', error)
  }
}

const handleDelete = async (row: StockIn) => {
  if (!canDeleteStockIn.value) {
    ElMessage.warning('没有入库单删除权限')
    return
  }

  await ElMessageBox.confirm('确定要删除该入库单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await inventoryApi.deleteStockIn(row.id)
    ElMessage.success('删除成功')
    loadStockInList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(async () => {
  await loadWarehouseList()
  await loadStockInList()
})
</script>

<style scoped>
.stock-in-management {
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

.stock-form :deep(.el-divider__text) {
  font-weight: 600;
  color: #334155;
}

.add-item-btn {
  margin-bottom: 10px;
  border-radius: 8px;
}

.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
