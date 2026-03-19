<template>
  <el-dialog
    v-model="visible"
    title="从库存选择药品"
    width="900px"
    @close="handleClose"
  >
    <el-form :model="queryForm" inline style="margin-bottom: 16px">
      <el-form-item label="关键词">
        <el-input
          v-model="queryForm.keyword"
          class="query-keyword-input"
          placeholder="药品编码/名称"
          clearable
          @input="applyFilter"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadStockMaterials">刷新库存</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="filteredList"
      border
      stripe
      v-loading="loading"
      highlight-current-row
      max-height="420px"
      @row-click="handleRowClick"
    >
      <el-table-column prop="materialCode" label="药品编码" width="160" />
      <el-table-column prop="materialName" label="药品名称" min-width="180" />
      <el-table-column prop="unit" label="单位" width="100" />
      <el-table-column prop="availableQuantity" label="总可用库存" width="140" align="right" />
    </el-table>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :disabled="!selectedRow" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { inventoryApi } from '@/api/inventory'
import { materialApi } from '@/api/material'

interface StockMaterialSelection {
  materialId: number
  materialCode: string
  materialName: string
  unit?: string
  availableQuantity: number
}

interface Props {
  modelValue: boolean
  warehouseId?: number
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', row: StockMaterialSelection): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const visible = ref(props.modelValue)
const sourceList = ref<StockMaterialSelection[]>([])
const filteredList = ref<StockMaterialSelection[]>([])
const selectedRow = ref<StockMaterialSelection>()

const queryForm = reactive({
  keyword: ''
})

watch(() => props.modelValue, async (value) => {
  visible.value = value
  if (value) {
    await loadStockMaterials()
  }
})

watch(() => props.warehouseId, async (current, previous) => {
  if (!visible.value) {
    return
  }
  if (current === previous) {
    return
  }
  await loadStockMaterials()
})

watch(visible, (value) => {
  emit('update:modelValue', value)
})

const applyFilter = () => {
  const keyword = queryForm.keyword.trim().toLowerCase()
  if (!keyword) {
    filteredList.value = [...sourceList.value]
    return
  }
  filteredList.value = sourceList.value.filter((row) => {
    return row.materialCode.toLowerCase().includes(keyword) ||
      row.materialName.toLowerCase().includes(keyword)
  })
}

const loadStockMaterials = async () => {
  loading.value = true
  try {
    const aggregateMap = new Map<number, { availableQuantity: number }>()
    let page = 1
    let total = 0
    let loaded = 0
    const size = 200

    do {
      const pageResult = await inventoryApi.getStockList({
        page,
        size,
        warehouseId: props.warehouseId && props.warehouseId > 0 ? props.warehouseId : undefined
      })
      total = pageResult.total
      if (pageResult.list.length === 0) {
        break
      }
      loaded += pageResult.list.length

      pageResult.list.forEach((stock) => {
        const materialId = stock.materialId
        const current = aggregateMap.get(materialId) ?? { availableQuantity: 0 }
        current.availableQuantity += Number(stock.availableQuantity ?? 0)
        aggregateMap.set(materialId, current)
      })

      page += 1
    } while (loaded < total)

    const materialIds = [...aggregateMap.keys()]
    const rows = await Promise.all(materialIds.map(async (materialId) => {
      try {
        const material = await materialApi.getMaterialById(materialId)
        return {
          materialId,
          materialCode: material.materialCode || `药品#${materialId}`,
          materialName: material.materialName || `药品#${materialId}`,
          unit: material.unit,
          availableQuantity: aggregateMap.get(materialId)?.availableQuantity ?? 0
        } as StockMaterialSelection
      } catch {
        return {
          materialId,
          materialCode: `药品#${materialId}`,
          materialName: `药品#${materialId}`,
          unit: '',
          availableQuantity: aggregateMap.get(materialId)?.availableQuantity ?? 0
        } as StockMaterialSelection
      }
    }))

    sourceList.value = rows
      .filter((row) => row.availableQuantity > 0)
      .sort((a, b) => a.materialCode.localeCompare(b.materialCode))
    applyFilter()
  } finally {
    loading.value = false
  }
}

const handleRowClick = (row: StockMaterialSelection) => {
  selectedRow.value = row
}

const handleConfirm = () => {
  if (!selectedRow.value) {
    return
  }
  emit('select', selectedRow.value)
  handleClose()
}

const handleClose = () => {
  visible.value = false
  selectedRow.value = undefined
  queryForm.keyword = ''
}
</script>
