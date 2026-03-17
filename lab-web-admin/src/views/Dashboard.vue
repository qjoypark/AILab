<template>
  <div class="dashboard">
    <h1>欢迎使用智慧实验室管理系统</h1>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card>
          <el-statistic title="药品总数" :value="stats.materialCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="库存总价值" :value="stats.totalValue" prefix="¥" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="待审批申请" :value="stats.pendingApplications" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="预警数量" :value="stats.alertCount" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const stats = reactive({
  materialCount: 0,
  totalValue: 0,
  pendingApplications: 0,
  alertCount: 0
})
</script>

<style scoped>
.dashboard h1 {
  color: #303133;
  margin-bottom: 20px;
}
</style>
