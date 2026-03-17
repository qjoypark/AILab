<template>
  <div class="stock-in-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>入库管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增入库单
          </el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="入库单号" clearable />
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

      <!-- 入库单列表 -->
      <el-table :data="stockInList" border stripe v-loading="loading">
        <el-table-column prop="stockInCode" label="入库单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="stockInType" label="入库类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.stockInType === 1">采购入库</el-tag>
            <el-tag v-else-if="row.stockInType === 2" type="warning">退货入库</el-tag>
            <el-tag v-else type="info">盘盈入库</el-tag>
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
        <el-table-column prop="createdByName" label="创建人" width="100" />
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="success"
              @click="handleConfirm(row)"
            >
              确认入库
            </el-button>
            <el-button
              v-if="row.status === 0"
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
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stockInForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        
        <el-divider>入库明细</el-divider>
        
        <el-button type="primary" size="small" @click="handleAddItem" style="margin-bottom: 10px">
          添加药品
        </el-button>
        
        <el-table :data="stockInForm.items" border>
          <el-table-column label="药品" width="200">
            <template #default="{ row, $index }">
              <el-input v-model="row.materialName" placeholder="选择药品" readonly @click="selectMaterial($index)" />
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
              <el-button link type="danger" @click="handleRemoveItem($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="入库单详情" width="900px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ currentStockIn?.stockInCode }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ currentStockIn?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">
          <el-tag v-if="currentStockIn?.stockInType === 1">采购入库</el-tag>
          <el-tag v-else-if="currentStockIn?.stockInType === 2" type="warning">退货入库</el-tag>
          <el-tag v-else type="info">盘盈入库</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentStockIn?.status === 0" type="warning">待确认</el-tag>
          <el-tag v-else type="success">已确认</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentStockIn?.createdByName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentStockIn?.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentStockIn?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider>入库明细</el-divider>
      
      <el-table :data="currentStockIn?.items" border>
        <el-table-column prop="materialCode" label="药品编码" />
        <el-table-column prop="materialName" label="药品名称" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import type { StockIn, StockInForm, StockInDetail, Warehouse } from '@/types/inventory'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const stockInList = ref<StockIn[]>([])
const warehouseList = ref<Warehouse[]>([])
const currentStockIn = ref<StockIn>()
const total = ref(0)

const queryForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
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

const loadStockInList = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getStockInList(queryForm)
    stockInList.value = res.list
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
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadStockInList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  handleQuery()
}

const handleAdd = () => {
  dialogTitle.value = '新增入库单'
  Object.assign(stockInForm, {
    warehouseId: 0,
    stockInType: 1,
    remark: '',
    items: []
  })
  dialogVisible.value = true
}

const handleAddItem = () => {
  stockInForm.items.push({
    materialId: 0,
    materialName: '',
    quantity: 0,
    unitPrice: 0,
    batchNumber: '',
    productionDate: '',
    expiryDate: ''
  })
}

const handleRemoveItem = (index: number) => {
  stockInForm.items.splice(index, 1)
}

const selectMaterial = (index: number) => {
  // TODO: 实现药品选择对话框
  ElMessage.info('药品选择功能待实现')
}

const handleSubmit = async () => {
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
    currentStockIn.value = await inventoryApi.getStockInById(row.id)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载入库单详情失败:', error)
  }
}

const handleConfirm = async (row: StockIn) => {
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

onMounted(() => {
  loadStockInList()
  loadWarehouseList()
})
</script>

<style scoped>
.stock-in-management {
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
