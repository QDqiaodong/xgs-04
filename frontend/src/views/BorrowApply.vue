<template>
  <div>
    <h2 class="page-title">📝 借阅申请管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="收到的申请" name="received">
        <el-table :data="receivedList" v-loading="loading" style="width: 100%">
          <el-table-column prop="book.title" label="图书名称" />
          <el-table-column prop="borrower.nickname" label="申请人" />
          <el-table-column prop="startDate" label="开始日期" />
          <el-table-column prop="endDate" label="结束日期" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                size="small"
                type="success"
                @click="handleApprove(row)"
              >
                同意
              </el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                size="small"
                type="danger"
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
              <el-button
                v-if="row.status === 'APPROVED'"
                size="small"
                type="primary"
                @click="handleConfirmBorrow(row)"
              >
                确认借出
              </el-button>
              <el-button
                v-if="row.status === 'BORROWING'"
                size="small"
                type="success"
                @click="handleConfirmReturn(row)"
              >
                确认归还
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的申请" name="sent">
        <el-table :data="sentList" v-loading="loading" style="width: 100%">
          <el-table-column prop="book.title" label="图书名称" />
          <el-table-column prop="owner.nickname" label="所有者" />
          <el-table-column prop="startDate" label="开始日期" />
          <el-table-column prop="endDate" label="结束日期" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { borrowRecordAPI } from '@/api'
import { refreshBorrowRecords } from '@/store/borrowRecord'

const currentUserId = 1
const activeTab = ref('received')
const receivedList = ref([])
const sentList = ref([])
const loading = ref(false)

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已同意', type: 'primary' },
  REJECTED: { text: '已拒绝', type: 'danger' },
  BORROWING: { text: '借阅中', type: 'info' },
  RETURNED: { text: '已归还', type: 'success' }
}

const getStatusText = (status) => statusMap[status]?.text || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const loadReceived = async () => {
  loading.value = true
  try {
    const res = await borrowRecordAPI.getByOwner(currentUserId)
    if (res.code === 200) {
      receivedList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载申请列表失败')
  } finally {
    loading.value = false
  }
}

const loadSent = async () => {
  try {
    const res = await borrowRecordAPI.getByBorrower(currentUserId)
    if (res.code === 200) {
      sentList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载申请列表失败')
  }
}

const handleApprove = async (row) => {
  try {
    const res = await borrowRecordAPI.approve(row.id)
    if (res.code === 200) {
      ElMessage.success('已同意借阅申请')
      refreshBorrowRecords()
      loadReceived()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm('确定要拒绝该借阅申请吗？', '提示', { type: 'warning' })
    const res = await borrowRecordAPI.reject(row.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝借阅申请')
      refreshBorrowRecords()
      loadReceived()
    }
  } catch {
  }
}

const handleConfirmBorrow = async (row) => {
  try {
    const res = await borrowRecordAPI.confirmBorrow(row.id)
    if (res.code === 200) {
      ElMessage.success('已确认借出')
      refreshBorrowRecords()
      loadReceived()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleConfirmReturn = async (row) => {
  try {
    const res = await borrowRecordAPI.confirmReturn(row.id)
    if (res.code === 200) {
      ElMessage.success('已确认归还')
      refreshBorrowRecords()
      loadReceived()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadReceived()
  loadSent()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 20px;
  font-size: 24px;
  color: #303133;
}
</style>
