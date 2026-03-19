<template>
  <div class="warehouse-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>仓库管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增仓库
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="仓库类型">
          <el-select
            v-model="queryForm.warehouseType"
            v-adaptive-select-width="['全部', '普通仓库', '危化品仓库']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="普通仓库" :value="1" />
            <el-option label="危化品仓库" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="warehouseList" border stripe v-loading="loading">
        <el-table-column prop="warehouseCode" label="仓库编码" width="160" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="180" />
        <el-table-column prop="warehouseType" label="仓库类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.warehouseType === 1">普通仓库</el-tag>
            <el-tag v-else type="warning">危化品仓库</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadWarehouseList"
        @current-change="loadWarehouseList"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="warehouseForm"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="warehouseForm.warehouseCode" placeholder="如：WH003" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="warehouseForm.warehouseName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="仓库类型" prop="warehouseType">
              <el-select v-model="warehouseForm.warehouseType" style="width: 100%">
                <el-option label="普通仓库" :value="1" />
                <el-option label="危化品仓库" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="warehouseForm.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置">
          <el-input v-model="warehouseForm.location" placeholder="仓库实际位置" />
        </el-form-item>
        <el-form-item label="负责人ID">
          <el-input-number
            v-model="warehouseForm.managerId"
            :min="1"
            :precision="0"
            controls-position="right"
            style="width: 100%"
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { inventoryApi } from '@/api/inventory'
import type { Warehouse, WarehouseForm, WarehouseQuery } from '@/types/inventory'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const warehouseList = ref<Warehouse[]>([])
const total = ref(0)

const queryForm = reactive<WarehouseQuery>({
  warehouseType: -1,
  page: 1,
  size: 10
})

const warehouseForm = reactive<WarehouseForm>({
  warehouseCode: '',
  warehouseName: '',
  warehouseType: 1,
  location: '',
  managerId: undefined,
  status: 1
})

const rules: FormRules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }]
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 19)
}

const normalizeWarehouse = (warehouse: Warehouse): Warehouse => ({
  ...warehouse,
  status: warehouse.status ?? 1,
  createdTime: formatDateTime(warehouse.createdTime),
  updatedTime: formatDateTime(warehouse.updatedTime)
})

const loadWarehouseList = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getWarehousePage(queryForm)
    warehouseList.value = res.list.map(normalizeWarehouse)
    total.value = res.total
  } catch (error) {
    console.error('加载仓库列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadWarehouseList()
}

const handleReset = () => {
  queryForm.warehouseType = -1
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增仓库'
  Object.assign(warehouseForm, {
    id: undefined,
    warehouseCode: '',
    warehouseName: '',
    warehouseType: 1,
    location: '',
    managerId: undefined,
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: Warehouse) => {
  dialogTitle.value = '编辑仓库'
  Object.assign(warehouseForm, {
    id: row.id,
    warehouseCode: row.warehouseCode ?? '',
    warehouseName: row.warehouseName,
    warehouseType: row.warehouseType,
    location: row.location ?? '',
    managerId: row.managerId,
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (warehouseForm.id) {
        await inventoryApi.updateWarehouse(warehouseForm.id, warehouseForm)
        ElMessage.success('更新仓库成功')
      } else {
        await inventoryApi.createWarehouse(warehouseForm)
        ElMessage.success('新增仓库成功')
      }
      dialogVisible.value = false
      loadWarehouseList()
    } catch (error) {
      console.error('提交仓库失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: Warehouse) => {
  await ElMessageBox.confirm(`确定要删除仓库【${row.warehouseName}】吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await inventoryApi.deleteWarehouse(row.id)
    ElMessage.success('删除仓库成功')
    loadWarehouseList()
  } catch (error) {
    console.error('删除仓库失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadWarehouseList()
})
</script>

<style scoped>
.warehouse-management {
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
