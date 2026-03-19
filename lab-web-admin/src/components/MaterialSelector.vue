<template>
  <el-dialog
    v-model="visible"
    title="选择药品"
    width="900px"
    @close="handleClose"
  >
    <!-- 搜索表单 -->
    <el-form :model="queryForm" inline style="margin-bottom: 20px">
      <el-form-item label="关键词">
        <el-input v-model="queryForm.keyword" class="query-keyword-input" placeholder="药品编码/名称" clearable />
      </el-form-item>
      <el-form-item label="药品类型">
        <el-select
          v-model="queryForm.materialType"
          v-adaptive-select-width="['全部', '耗材', '试剂', '危化品']"
          placeholder="请选择"
          clearable
        >
          <el-option label="全部" :value="-1" />
          <el-option label="耗材" :value="1" />
          <el-option label="试剂" :value="2" />
          <el-option label="危化品" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 药品列表 -->
    <el-table
      :data="materialList"
      border
      stripe
      v-loading="loading"
      @row-click="handleRowClick"
      highlight-current-row
      max-height="400px"
    >
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="materialCode" label="药品编码" width="120" />
      <el-table-column prop="materialName" label="药品名称" min-width="150" />
      <el-table-column prop="materialType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.materialType === 1">耗材</el-tag>
          <el-tag v-else-if="row.materialType === 2" type="success">试剂</el-tag>
          <el-tag v-else type="danger">危化品</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="specification" label="规格" width="120" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="unitPrice" label="单价" width="100" align="right">
        <template #default="{ row }">
          {{ row.unitPrice ? `¥${row.unitPrice}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="isControlled" label="管控" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isControlled === 1" type="warning" size="small">易制毒</el-tag>
          <el-tag v-else-if="row.isControlled === 2" type="danger" size="small">易制爆</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="queryForm.page"
      v-model:page-size="queryForm.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end"
      @size-change="handleQuery"
      @current-change="handleQuery"
    />

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :disabled="!selectedMaterial">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { materialApi } from '@/api/material'
import type { Material, MaterialQuery } from '@/types/material'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', material: Material): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const materialList = ref<Material[]>([])
const selectedMaterial = ref<Material>()
const total = ref(0)

const visible = ref(props.modelValue)

const queryForm = reactive<MaterialQuery>({
  keyword: '',
  materialType: -1,
  status: 1, // 只查询启用的药品
  page: 1,
  size: 10
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadMaterialList()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadMaterialList = async () => {
  loading.value = true
  try {
    const res = await materialApi.getMaterialList(queryForm)
    materialList.value = res.list
    total.value = res.total
  } catch (error) {
    console.error('加载药品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = (trigger?: number | Event) => {
  if (typeof trigger !== 'number') {
    queryForm.page = 1
  }
  loadMaterialList()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.materialType = -1
  handleQuery()
}

const handleRowClick = (row: Material) => {
  selectedMaterial.value = row
}

const handleConfirm = () => {
  if (selectedMaterial.value) {
    emit('select', selectedMaterial.value)
    handleClose()
  }
}

const handleClose = () => {
  visible.value = false
  selectedMaterial.value = undefined
}
</script>

<style scoped>
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
