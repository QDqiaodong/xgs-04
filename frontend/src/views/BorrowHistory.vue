<template>
  <div>
    <h2 class="page-title">📋 借阅历史记录</h2>

    <el-table :data="historyList" v-loading="loading" style="width: 100%">
      <el-table-column prop="book.title" label="图书名称" min-width="150" />
      <el-table-column prop="borrower.nickname" label="借阅人" width="120" />
      <el-table-column prop="owner.nickname" label="所有者" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="borrowTime" label="借出时间" width="180" />
      <el-table-column prop="returnTime" label="归还时间" width="180">
        <template #default="{ row }">
          {{ row.returnTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: center"
      @current-change="loadHistory"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { borrowRecordAPI } from '@/api'

const currentUserId = 1
const historyList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已同意', type: 'primary' },
  REJECTED: { text: '已拒绝', type: 'danger' },
  BORROWING: { text: '借阅中', type: 'info' },
  RETURNED: { text: '已归还', type: 'success' }
}

const getStatusText = (status) => statusMap[status]?.text || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await borrowRecordAPI.getByOwner(currentUserId)
    if (res.code === 200) {
      historyList.value = res.data
      total.value = res.data.length
    }
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
  font-size: 24px;
  color: #303133;
}
</style>
