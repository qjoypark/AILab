<template>
  <div class="stock-check-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存盘点</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建盘点单
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="盘点单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
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
        <el-table-column prop="warehouseName" label="仓库" width="120" />
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
        <el-table-column prop="createdByName" label="创建人" width="100" />
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="completedTime" label="完成时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="warning"
              @click="handleRecord(row)"
            >
              录入盘点
            </el-button>
            <el-button
              v-if="row.status === 0"
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
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 录入盘点对话框 -->
    <el-dialog
      v-model="recordDialogVisible"
      title="录入盘点明细"
      width="1000px"
    >
      <el-button type="primary" size="small" @click="handleAddCheckItem" style="margin-bottom: 10px">
        添加药品
      </el-button>
      
      <el-table :data="checkItems" border max-height="400px">
        <el-table-column label="药品" width="200">
          <template #default="{ row, $index }">
            <el-input v-model="row.materialName" placeholder="选择药品" readonly @click="selectMaterial($index)" />
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
            <el-button link type="danger" @click="handleRemoveCheckItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCheckItems" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="盘点单详情" width="1000px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="盘点单号">{{ currentCheck?.checkCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentCheck?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="盘点类型">
          <el-tag v-if="currentCheck?.checkType === 1">全盘</el-tag>
          <el-tag v-else type="info">抽盘</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentCheck?.status === 0" type="warning">盘点中</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentCheck?.createdByName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentCheck?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间" :span="2">{{ currentCheck?.completedTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentCheck?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>盘点明细</el-divider>
      
      <el-table :data="currentCheck?.items" border>
        <el-table-column prop="materialCode" label="药品编码" />
        <el-table-column prop="materialName" label="药品名称" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import MaterialSelector from '@/components/MaterialSelector.vue'
import type { StockCheck, StockCheckForm, StockCheckDetail, Warehouse } from '@/types/inventory'
import type { Material } from '@/types/material'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const recordDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const materialSelectorVisible = ref(false)
const formRef = ref<FormInstance>()
const checkList = ref<StockCheck[]>([])
const warehouseList = ref<Warehouse[]>([])
const currentCheck = ref<StockCheck>()
const checkItems = ref<StockCheckDetail[]>([])
const currentItemIndex = ref<number>()
const currentCheckId = ref<number>()
const total = ref(0)

const queryForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
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

const loadCheckList = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getStockCheckList(queryForm)
    checkList.value = res.list
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
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadCheckList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  handleQuery()
}

const handleAdd = () => {
  Object.assign(checkForm, {
    warehouseId: 0,
    checkType: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
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
  currentCheckId.value = row.id
  try {
    const check = await inventoryApi.getStockCheckById(row.id)
    checkItems.value = check.items || []
    recordDialogVisible.value = true
  } catch (error) {
    console.error('加载盘点单详情失败:', error)
  }
}

const handleAddCheckItem = () => {
  checkItems.value.push({
    materialId: 0,
    materialName: '',
    batchNumber: '',
    bookQuantity: 0,
    actualQuantity: 0,
    differenceQuantity: 0,
    differenceReason: ''
  })
}

const handleRemoveCheckItem = (index: number) => {
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
    item.materialCode = material.materialCode
    item.materialName = material.materialName
  }
}

const handleSubmitCheckItems = async () => {
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
    currentCheck.value = await inventoryApi.getStockCheckById(row.id)
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

onMounted(() => {
  loadCheckList()
  loadWarehouseList()
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
