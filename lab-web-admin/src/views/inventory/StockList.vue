<template>
  <div class="stock-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存查询</span>
          <div>
            <el-button type="primary" @click="$router.push('/inventory/stock-in')">入库</el-button>
            <el-button type="success" @click="$router.push('/inventory/stock-out')">出库</el-button>
            <el-button type="warning" @click="$router.push('/inventory/stock-check')">盘点</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="药品编码/名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select v-model="queryForm.warehouseId" placeholder="请选择" clearable>
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
      <el-table :data="stockList" border stripe v-loading="loading">
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
      <el-table :data="stockDetailList" border>
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
import { ref, reactive, onMounted } from 'vue'
import { inventoryApi } from '@/api/inventory'
import type { StockInventory, StockQuery, Warehouse } from '@/types/inventory'

const loading = ref(false)
const detailDialogVisible = ref(false)
const stockList = ref<StockInventory[]>([])
const stockDetailList = ref<StockInventory[]>([])
const warehouseList = ref<Warehouse[]>([])
const total = ref(0)

const queryForm = reactive<StockQuery>({
  keyword: '',
  warehouseId: undefined,
  lowStock: false,
  page: 1,
  size: 10
})

const loadStockList = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getStockList(queryForm)
    stockList.value = res.list
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
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  }
}

const handleQuery = () => {
  queryForm.page = 1
  loadStockList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.warehouseId = undefined
  queryForm.lowStock = false
  handleQuery()
}

const handleViewDetail = async (row: StockInventory) => {
  try {
    stockDetailList.value = await inventoryApi.getStockDetail(row.materialId)
    detailDialogVisible.value = true
  } catch (error) {
    console.error('加载库存明细失败:', error)
  }
}

const isLowStock = (row: StockInventory) => {
  // 简单判断：可用数量小于10
  return row.availableQuantity < 10
}

const isExpiringSoon = (row: StockInventory) => {
  if (!row.expiryDate) return false
  const expiryDate = new Date(row.expiryDate)
  const today = new Date()
  const daysUntilExpiry = Math.floor((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  return daysUntilExpiry <= 30 && daysUntilExpiry >= 0
}

onMounted(() => {
  loadStockList()
  loadWarehouseList()
})
</script>

<style scoped>
.stock-list {
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

.low-stock {
  color: #f56c6c;
  font-weight: bold;
}

.expiring-soon {
  color: #e6a23c;
  font-weight: bold;
}
</style>
