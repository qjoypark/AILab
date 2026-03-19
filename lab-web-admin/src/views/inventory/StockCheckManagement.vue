<template>
  <div class="stock-check-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存盘点</span>
          <el-button v-if="canCreateStockCheck" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建盘点单
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" class="query-keyword-input" placeholder="盘点单号" clearable />
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
            v-adaptive-select-width="['全部', '盘点中', '已完成']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="盘点中" :value="0" />
            <el-option label="已完成" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 盘点单列表 -->
      <el-table :data="checkList" border stripe v-loading="loading">
        <el-table-column prop="checkCode" label="盘点单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="140">
          <template #default="{ row }">
            {{ getWarehouseDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="checkType" label="盘点类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.checkType === 1">全盘</el-tag>
            <el-tag v-else type="info">抽盘</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">盘点中</el-tag>
            <el-tag v-else type="success">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="120">
          <template #default="{ row }">
            {{ getCreatorDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="completedTime" label="完成时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 0 && canRecordStockCheck"
              link
              type="warning"
              @click="handleRecord(row)"
            >
              录入盘点
            </el-button>
            <el-button
              v-if="row.status === 0 && canCompleteStockCheck"
              link
              type="success"
              @click="handleComplete(row)"
            >
              完成盘点
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

    <!-- 创建盘点单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建盘点单"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="checkForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="checkForm.warehouseId" style="width: 100%">
            <el-option
              v-for="warehouse in warehouseList"
              :key="warehouse.id"
              :label="warehouse.warehouseName"
              :value="warehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型" prop="checkType">
          <el-radio-group v-model="checkForm.checkType">
            <el-radio :label="1">全盘</el-radio>
            <el-radio :label="2">抽盘</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="checkForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canCreateStockCheck" type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 录入盘点对话框 -->
    <el-dialog
      v-model="recordDialogVisible"
      title="录入盘点明细"
      width="1000px"
    >
      <el-button
        v-if="canRecordStockCheck"
        type="primary"
        size="small"
        @click="handleAddCheckItem"
        style="margin-bottom: 10px"
      >
        添加药品
      </el-button>
      
      <el-table :data="checkItems" border max-height="400px">
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
        <el-table-column label="批次号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNumber" />
          </template>
        </el-table-column>
        <el-table-column label="账面数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.bookQuantity" :min="0" :precision="2" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="实际数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQuantity" :min="0" :precision="2" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="差异" width="100" align="right">
          <template #default="{ row }">
            <span :class="getDifferenceClass(row)">
              {{ (row.actualQuantity - row.bookQuantity).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异原因" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.differenceReason" placeholder="可选" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button v-if="canRecordStockCheck" link type="danger" @click="handleRemoveCheckItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button v-if="canRecordStockCheck" type="primary" @click="handleSubmitCheckItems" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="盘点单详情" width="1000px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="盘点单号">{{ currentCheck?.checkCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentCheck ? getWarehouseDisplay(currentCheck) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="盘点类型">
          <el-tag v-if="currentCheck?.checkType === 1">全盘</el-tag>
          <el-tag v-else type="info">抽盘</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentCheck?.status === 0" type="warning">盘点中</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentCheck ? getCreatorDisplay(currentCheck) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentCheck?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间" :span="2">{{ currentCheck?.completedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentCheck?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>盘点明细</el-divider>
      
      <el-table :data="currentCheck?.items" border>
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
        <el-table-column prop="batchNumber" label="批次号" />
        <el-table-column prop="bookQuantity" label="账面数量" align="right" />
        <el-table-column prop="actualQuantity" label="实际数量" align="right" />
        <el-table-column label="差异" align="right">
          <template #default="{ row }">
            <span :class="getDifferenceClass(row)">
              {{ row.differenceQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="differenceReason" label="差异原因" />
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
import type { StockCheck, StockCheckForm, StockCheckDetail, Warehouse } from '@/types/inventory'
import type { Material } from '@/types/material'
import { materialApi } from '@/api/material'
import { useUserStore } from '@/stores/user'
import { INVENTORY_STOCK_CHECK_PERMISSIONS } from '@/constants/permissions'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const recordDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const materialSelectorVisible = ref(false)
const formRef = ref<FormInstance>()
const checkList = ref<StockCheck[]>([])
const warehouseList = ref<Warehouse[]>([])
const warehouseNameMap = ref<Record<number, string>>({})
const userNameMap = ref<Record<number, string>>({})
const materialInfoMap = ref<Record<number, { materialCode?: string; materialName?: string }>>({})
const currentCheck = ref<StockCheck>()
const checkItems = ref<StockCheckDetail[]>([])
const currentItemIndex = ref<number>()
const currentCheckId = ref<number>()
const total = ref(0)
const canCreateStockCheck = computed(() => userStore.hasPermission('inventory:stock-check:create'))
const canRecordStockCheck = computed(() => userStore.hasPermission('inventory:stock-check:record'))
const canCompleteStockCheck = computed(() => userStore.hasPermission('inventory:stock-check:complete'))
const canAccessStockCheckPage = computed(() => userStore.hasAnyPermission([...INVENTORY_STOCK_CHECK_PERMISSIONS]))
const canReadMaterial = computed(() => userStore.hasPermission('material:list'))

const queryForm = reactive({
  keyword: '',
  warehouseId: -1 as number,
  status: -1 as number,
  page: 1,
  size: 10
})

const checkForm = reactive<StockCheckForm>({
  warehouseId: 0,
  checkType: 1,
  remark: ''
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  checkType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }]
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

const getWarehouseDisplay = (check: StockCheck) => {
  return (
    check.warehouseName ||
    warehouseNameMap.value[check.warehouseId] ||
    formatIdLabel('仓库', check.warehouseId)
  )
}

const getCreatorDisplay = (check: StockCheck) => {
  return (
    check.createdByName ||
    (check.createdBy ? userNameMap.value[check.createdBy] : '') ||
    formatIdLabel('用户', check.createdBy)
  )
}

const getMaterialCodeDisplay = (detail: StockCheckDetail) => {
  return (
    detail.materialCode ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialCode : '') ||
    '-'
  )
}

const getMaterialNameDisplay = (detail: StockCheckDetail) => {
  return (
    detail.materialName ||
    (detail.materialId ? materialInfoMap.value[detail.materialId]?.materialName : '') ||
    '-'
  )
}

const enrichStockCheck = async (stockCheck: StockCheck) => {
  const enriched: StockCheck = {
    ...stockCheck,
    warehouseName: stockCheck.warehouseName || warehouseNameMap.value[stockCheck.warehouseId],
    createdTime: formatDateTime(stockCheck.createdTime || (stockCheck as any).checkDate),
    completedTime: formatDateTime(stockCheck.completedTime)
  }

  const creatorId = enriched.createdBy
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
      detail.differenceQuantity = detail.differenceQuantity ?? (detail as any).diffQuantity
      detail.differenceReason = detail.differenceReason ?? (detail as any).diffReason
    }
  }

  return enriched
}

const loadCheckList = async () => {
  if (!canAccessStockCheckPage.value) {
    checkList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const res = await inventoryApi.getStockCheckList(queryForm)
    checkList.value = await Promise.all(res.list.map(enrichStockCheck))
    total.value = res.total
  } catch (error) {
    console.error('加载盘点单列表失败:', error)
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
  loadCheckList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.warehouseId = -1
  queryForm.status = -1
  handleQuery()
}

const handleAdd = () => {
  if (!canCreateStockCheck.value) {
    ElMessage.warning('没有盘点单新增权限')
    return
  }

  Object.assign(checkForm, {
    warehouseId: 0,
    checkType: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!canCreateStockCheck.value) {
    ElMessage.warning('没有盘点单新增权限')
    return
  }

  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      await inventoryApi.createStockCheck(checkForm)
      ElMessage.success('创建成功')
      dialogVisible.value = false
      loadCheckList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleRecord = async (row: StockCheck) => {
  if (!canRecordStockCheck.value) {
    ElMessage.warning('没有盘点录入权限')
    return
  }

  currentCheckId.value = row.id
  try {
    const check = await inventoryApi.getStockCheckById(row.id)
    const enriched = await enrichStockCheck(check)
    checkItems.value = enriched.items || []
    recordDialogVisible.value = true
  } catch (error) {
    console.error('加载盘点单详情失败:', error)
  }
}

const handleAddCheckItem = () => {
  if (!canRecordStockCheck.value) {
    ElMessage.warning('没有盘点录入权限')
    return
  }

  checkItems.value.push({
    materialId: 0,
    materialCode: '',
    materialName: '',
    batchNumber: '',
    bookQuantity: 0,
    actualQuantity: 0,
    differenceQuantity: 0,
    differenceReason: ''
  })
}

const handleRemoveCheckItem = (index: number) => {
  if (!canRecordStockCheck.value) {
    ElMessage.warning('没有盘点录入权限')
    return
  }

  checkItems.value.splice(index, 1)
}

const selectMaterial = (index: number) => {
  currentItemIndex.value = index
  materialSelectorVisible.value = true
}

const handleMaterialSelected = (material: Material) => {
  if (currentItemIndex.value !== undefined) {
    const item = checkItems.value[currentItemIndex.value]
    item.materialId = material.id
    const materialCode = material.materialCode || (material as any).code || ''
    const materialName = material.materialName || (material as any).name || ''
    item.materialCode = materialCode
    item.materialName = materialName

    materialInfoMap.value[material.id] = {
      materialCode,
      materialName
    }
  }
}

const handleSubmitCheckItems = async () => {
  if (!canRecordStockCheck.value) {
    ElMessage.warning('没有盘点录入权限')
    return
  }

  if (!currentCheckId.value) return
  
  if (checkItems.value.length === 0) {
    ElMessage.warning('请添加盘点明细')
    return
  }
  
  // 计算差异数量
  checkItems.value.forEach(item => {
    item.differenceQuantity = item.actualQuantity - item.bookQuantity
  })
  
  submitting.value = true
  try {
    await inventoryApi.submitCheckItems(currentCheckId.value, checkItems.value)
    ElMessage.success('保存成功')
    recordDialogVisible.value = false
    loadCheckList()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleComplete = async (row: StockCheck) => {
  if (!canCompleteStockCheck.value) {
    ElMessage.warning('没有盘点完成权限')
    return
  }

  await ElMessageBox.confirm('确定要完成盘点吗？完成后将根据盘点结果调整库存。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  
  try {
    await inventoryApi.completeStockCheck(row.id)
    ElMessage.success('完成盘点成功')
    loadCheckList()
  } catch (error) {
    console.error('完成盘点失败:', error)
  }
}

const handleView = async (row: StockCheck) => {
  try {
    const check = await inventoryApi.getStockCheckById(row.id)
    currentCheck.value = await enrichStockCheck(check)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载盘点单详情失败:', error)
  }
}

const getDifferenceClass = (row: StockCheckDetail) => {
  const diff = row.actualQuantity - row.bookQuantity
  if (diff > 0) return 'text-success'
  if (diff < 0) return 'text-danger'
  return ''
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(async () => {
  await loadWarehouseList()
  await loadCheckList()
})
</script>

<style scoped>
.stock-check-management {
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

.text-success {
  color: #67c23a;
  font-weight: bold;
}

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>
