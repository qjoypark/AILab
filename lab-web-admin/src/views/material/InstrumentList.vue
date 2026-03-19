<template>
  <div class="instrument-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>仪器列表</span>
          <el-button type="primary" disabled>新增仪器</el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            class="query-keyword-input"
            placeholder="仪器编号/名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.status"
            v-adaptive-select-width="['全部', '在用', '停用']"
            placeholder="请选择"
            clearable
          >
            <el-option label="全部" :value="-1" />
            <el-option label="在用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="instrumentList" border stripe>
        <el-table-column prop="instrumentCode" label="仪器编号" width="160" />
        <el-table-column prop="instrumentName" label="仪器名称" min-width="220" />
        <el-table-column prop="model" label="型号" width="180" />
        <el-table-column prop="location" label="存放位置" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">在用</el-tag>
            <el-tag v-else type="info">停用</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="instrumentList.length === 0"
        description="暂无仪器数据，后续会在此模块继续扩展"
        :image-size="100"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

interface Instrument {
  instrumentCode: string
  instrumentName: string
  model?: string
  location?: string
  status: number
}

const queryForm = reactive({
  keyword: '',
  status: -1 as number
})

const instrumentList = ref<Instrument[]>([])

const handleSearch = () => {
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = -1
  handleSearch()
}
</script>

<style scoped>
.instrument-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
