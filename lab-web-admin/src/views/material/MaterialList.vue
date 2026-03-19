<template>
  <div class="material-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>药品列表</span>
          <el-button v-if="canCreateMaterial" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增药品
          </el-button>
        </div>
      </template>

      <template v-if="canReadMaterial">
        <el-form :model="queryForm" inline>
          <el-form-item label="关键字">
            <el-input
              v-model="queryForm.keyword"
              class="query-keyword-input"
              placeholder="药品编码/名称"
              clearable
            />
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
          <el-form-item label="管控类型">
            <el-select
              v-model="queryForm.isControlled"
              v-adaptive-select-width="['全部', '非管控', '易制毒', '易制爆']"
              placeholder="请选择"
              clearable
            >
              <el-option label="全部" :value="-1" />
              <el-option label="非管控" :value="0" />
              <el-option label="易制毒" :value="1" />
              <el-option label="易制爆" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="materialList" border stripe v-loading="loading">
          <el-table-column prop="materialCode" label="药品编码" width="120" />
          <el-table-column prop="materialName" label="药品名称" />
          <el-table-column prop="materialType" label="类型" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.materialType === 1">耗材</el-tag>
              <el-tag v-else-if="row.materialType === 2" type="success">试剂</el-tag>
              <el-tag v-else type="danger">危化品</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" />
          <el-table-column prop="specification" label="规格" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="unitPrice" label="单价" width="100">
            <template #default="{ row }">
              {{ row.unitPrice ? `￥${row.unitPrice}` : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="isControlled" label="管控" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.isControlled === 1" type="warning">易制毒</el-tag>
              <el-tag v-else-if="row.isControlled === 2" type="danger">易制爆</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button v-if="canUpdateMaterial" link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="canDeleteMaterial" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

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
        title="当前角色未分配“物资读取”权限，仅可使用已授权的物资功能。"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="materialForm" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品编码" prop="materialCode">
              <el-input v-model="materialForm.materialCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="药品名称" prop="materialName">
              <el-input v-model="materialForm.materialName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="药品类型" prop="materialType">
              <el-select v-model="materialForm.materialType" style="width: 100%">
                <el-option label="耗材" :value="1" />
                <el-option label="试剂" :value="2" />
                <el-option label="危化品" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select
                v-model="materialForm.categoryId"
                :data="categoryTree"
                :props="{ label: 'categoryName', children: 'children' }"
                node-key="id"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="materialForm.specification" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="materialForm.unit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产厂家" prop="manufacturer">
              <el-input v-model="materialForm.manufacturer" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="materialForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="materialForm.materialType === 3">
          <el-col :span="12">
            <el-form-item label="CAS号" prop="casNumber">
              <el-input v-model="materialForm.casNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="危险类别" prop="dangerCategory">
              <el-input v-model="materialForm.dangerCategory" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20" v-if="materialForm.materialType === 3">
          <el-col :span="12">
            <el-form-item label="管控类型" prop="isControlled">
              <el-select v-model="materialForm.isControlled" style="width: 100%">
                <el-option label="非管控" :value="0" />
                <el-option label="易制毒" :value="1" />
                <el-option label="易制爆" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="存储条件" prop="storageConditions">
          <el-input v-model="materialForm.storageConditions" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="安全说明" prop="safetyInstructions">
          <el-input v-model="materialForm.safetyInstructions" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="药品详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="药品编码">{{ currentMaterial?.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="药品名称">{{ currentMaterial?.materialName }}</el-descriptions-item>
        <el-descriptions-item label="药品类型">{{ getMaterialTypeLabel(currentMaterial?.materialType) }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentMaterial?.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentMaterial?.specification || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ currentMaterial?.unit || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生产厂家">{{ currentMaterial?.manufacturer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ formatMoney(currentMaterial?.unitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="CAS号">{{ currentMaterial?.casNumber || '-' }}</el-descriptions-item>
        <el-descriptions-item label="危险类别">{{ currentMaterial?.dangerCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="管控类型">{{ getControlLabel(currentMaterial?.isControlled) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentMaterial?.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="info">禁用</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="存储条件" :span="2">{{ currentMaterial?.storageConditions || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安全说明" :span="2">{{ currentMaterial?.safetyInstructions || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { materialApi } from '@/api/material'
import { useUserStore } from '@/stores/user'
import type { Material, MaterialCategory, MaterialForm, MaterialQuery } from '@/types/material'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const materialList = ref<Material[]>([])
const currentMaterial = ref<Material>()
const categoryTree = ref<MaterialCategory[]>([])
const total = ref(0)

const canReadMaterial = computed(() => userStore.hasPermission('material:list'))
const canCreateMaterial = computed(() => userStore.hasPermission('material:create'))
const canUpdateMaterial = computed(() => userStore.hasPermission('material:update'))
const canDeleteMaterial = computed(() => userStore.hasPermission('material:delete'))

const queryForm = reactive<MaterialQuery>({
  keyword: '',
  materialType: -1,
  isControlled: -1,
  page: 1,
  size: 10
})

const materialForm = reactive<MaterialForm>({
  materialCode: '',
  materialName: '',
  materialType: 1,
  categoryId: 0,
  specification: '',
  unit: '',
  manufacturer: '',
  unitPrice: undefined,
  casNumber: '',
  dangerCategory: '',
  isControlled: 0,
  storageConditions: '',
  safetyInstructions: ''
})

const rules: FormRules = {
  materialCode: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  materialType: [{ required: true, message: '请选择药品类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

const loadMaterialList = async () => {
  if (!canReadMaterial.value) {
    materialList.value = []
    total.value = 0
    return
  }

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

const loadCategoryTree = async () => {
  try {
    categoryTree.value = await materialApi.getCategoryTree()
  } catch (error) {
    console.error('加载分类树失败:', error)
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
  queryForm.isControlled = -1
  handleQuery()
}

const handleAdd = () => {
  if (!canCreateMaterial.value) {
    ElMessage.warning('没有物资新增权限')
    return
  }

  dialogTitle.value = '新增药品'
  Object.assign(materialForm, {
    id: undefined,
    materialCode: '',
    materialName: '',
    materialType: 1,
    categoryId: 0,
    specification: '',
    unit: '',
    manufacturer: '',
    unitPrice: undefined,
    casNumber: '',
    dangerCategory: '',
    isControlled: 0,
    storageConditions: '',
    safetyInstructions: ''
  })
  dialogVisible.value = true
}

const handleView = async (row: Material) => {
  try {
    currentMaterial.value = await materialApi.getMaterialById(row.id)
    viewDialogVisible.value = true
  } catch (error) {
    console.error('加载药品详情失败:', error)
  }
}

const getMaterialTypeLabel = (materialType?: number) => {
  if (materialType === 1) return '耗材'
  if (materialType === 2) return '试剂'
  if (materialType === 3) return '危化品'
  return '-'
}

const getControlLabel = (isControlled?: number) => {
  if (isControlled === 1) return '易制毒'
  if (isControlled === 2) return '易制爆'
  return '非管控'
}

const formatMoney = (amount?: number) => {
  if (amount === undefined || amount === null) {
    return '-'
  }
  return `￥${Number(amount).toFixed(2)}`
}

const handleEdit = async (row: Material) => {
  if (!canUpdateMaterial.value) {
    ElMessage.warning('没有物资编辑权限')
    return
  }
  dialogTitle.value = '编辑药品'
  try {
    const material = await materialApi.getMaterialById(row.id)
    Object.assign(materialForm, material)
    dialogVisible.value = true
  } catch (error) {
    console.error('加载药品详情失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (materialForm.id && !canUpdateMaterial.value) {
      ElMessage.warning('没有物资编辑权限')
      return
    }
    if (!materialForm.id && !canCreateMaterial.value) {
      ElMessage.warning('没有物资新增权限')
      return
    }

    submitting.value = true
    try {
      if (materialForm.id) {
        await materialApi.updateMaterial(materialForm.id, materialForm)
        ElMessage.success('更新成功')
      } else {
        await materialApi.createMaterial(materialForm)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadMaterialList()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: Material) => {
  if (!canDeleteMaterial.value) {
    ElMessage.warning('没有物资删除权限')
    return
  }

  await ElMessageBox.confirm('确定要删除该药品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await materialApi.deleteMaterial(row.id)
    ElMessage.success('删除成功')
    await loadMaterialList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(() => {
  loadMaterialList()
  loadCategoryTree()
})
</script>

<style scoped>
.material-list {
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
