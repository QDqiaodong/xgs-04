<template>
  <div>
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <span class="title-icon">✅</span>
          借阅审批工作台
        </h2>
        <el-tag v-if="pendingCount > 0" type="danger" size="large" effect="dark" class="pending-count-tag">
          {{ pendingCount }} 项待处理
        </el-tag>
        <el-tag v-else type="success" size="large" class="pending-count-tag">
          暂无待办
        </el-tag>
      </div>
      <div class="header-actions">
        <el-checkbox
          v-model="selectAll"
          :indeterminate="isIndeterminate"
          @change="handleSelectAll"
          :disabled="pendingList.length === 0"
        >
          全选
        </el-checkbox>
        <el-button
          type="success"
          :disabled="selectedIds.length === 0"
          @click="handleBatchApprove"
        >
          <el-icon><Check /></el-icon>
          批量同意 ({{ selectedIds.length }})
        </el-button>
        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchReject"
        >
          <el-icon><Close /></el-icon>
          批量拒绝 ({{ selectedIds.length }})
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon pending">⏳</div>
          <div class="stat-info">
            <div class="stat-number">{{ pendingCount }}</div>
            <div class="stat-label">待审批</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon approved">✔️</div>
          <div class="stat-info">
            <div class="stat-number">{{ approvedCount }}</div>
            <div class="stat-label">已同意</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon rejected">✖️</div>
          <div class="stat-info">
            <div class="stat-number">{{ rejectedCount }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon borrowing">📖</div>
          <div class="stat-info">
            <div class="stat-number">{{ borrowingCount }}</div>
            <div class="stat-label">借阅中</div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-loading="loading" class="card-list">
      <div v-if="pendingList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无待处理的借阅申请，太棒了！">
          <template #image>
            <div class="empty-emoji">🎉</div>
          </template>
        </el-empty>
      </div>

      <el-card
        v-for="record in pendingList"
        :key="record.id"
        class="approval-card"
        shadow="hover"
        :class="{ 'card-selected': selectedIds.includes(record.id) }"
      >
        <div class="card-inner">
          <div class="card-checkbox">
            <el-checkbox
              v-model="selectedIds"
              :value="record.id"
              @change="updateSelectAllState"
            />
          </div>

          <div class="card-content">
            <div class="card-header-row">
              <div class="book-info">
                <span class="book-title">《{{ record.book.title }}》</span>
                <el-tag type="warning" size="small" effect="light">
                  {{ record.book.author }}
                </el-tag>
              </div>
              <el-tag type="warning" size="default" effect="dark" class="status-tag">
                待审核
              </el-tag>
            </div>

            <div class="info-grid">
              <div class="info-item">
                <el-icon class="info-icon"><User /></el-icon>
                <div class="info-text">
                  <span class="info-label">借阅人</span>
                  <span class="info-value">{{ record.borrower.nickname }}</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Calendar /></el-icon>
                <div class="info-text">
                  <span class="info-label">借阅周期</span>
                  <span class="info-value">{{ record.startDate }} 至 {{ record.endDate }}</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Clock /></el-icon>
                <div class="info-text">
                  <span class="info-label">申请时间</span>
                  <span class="info-value">{{ formatTime(record.createTime) }}</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Phone /></el-icon>
                <div class="info-text">
                  <span class="info-label">联系方式</span>
                  <span class="info-value">{{ record.borrower.phone || '未填写' }}</span>
                </div>
              </div>
            </div>

            <div v-if="record.remark" class="remark-section">
              <div class="remark-label">
                <el-icon><ChatDotRound /></el-icon>
                借阅备注
              </div>
              <div class="remark-content">{{ record.remark }}</div>
            </div>

            <div class="card-actions">
              <el-button
                type="success"
                size="default"
                @click="handleApprove(record)"
              >
                <el-icon><Check /></el-icon>
                同意借阅
              </el-button>
              <el-button
                type="danger"
                size="default"
                @click="handleReject(record)"
              >
                <el-icon><Close /></el-icon>
                拒绝申请
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Refresh, User, Calendar, Clock, Phone, ChatDotRound } from '@element-plus/icons-vue'
import { borrowRecordAPI } from '@/api'
import { refreshBorrowRecords } from '@/store/borrowRecord'

const currentUserId = 1
const loading = ref(false)
const allRecords = ref([])
const selectedIds = ref([])
const selectAll = ref(false)
const isIndeterminate = ref(false)

const pendingList = computed(() => allRecords.value.filter(r => r.status === 'PENDING'))
const pendingCount = computed(() => pendingList.value.length)
const approvedCount = computed(() => allRecords.value.filter(r => r.status === 'APPROVED').length)
const rejectedCount = computed(() => allRecords.value.filter(r => r.status === 'REJECTED').length)
const borrowingCount = computed(() => allRecords.value.filter(r => r.status === 'BORROWING').length)

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await borrowRecordAPI.getByOwner(currentUserId)
    if (res.code === 200) {
      allRecords.value = res.data
      selectedIds.value = []
      updateSelectAllState()
    }
  } catch (e) {
    ElMessage.error('加载审批列表失败')
  } finally {
    loading.value = false
  }
}

const updateSelectAllState = () => {
  const pendingIds = pendingList.value.map(r => r.id)
  if (selectedIds.value.length === 0) {
    selectAll.value = false
    isIndeterminate.value = false
  } else if (selectedIds.value.length === pendingIds.length && pendingIds.length > 0) {
    selectAll.value = true
    isIndeterminate.value = false
  } else {
    selectAll.value = false
    isIndeterminate.value = true
  }
}

const handleSelectAll = (val) => {
  if (val) {
    selectedIds.value = pendingList.value.map(r => r.id)
  } else {
    selectedIds.value = []
  }
  isIndeterminate.value = false
}

const handleApprove = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定同意 ${record.borrower.nickname} 借阅《${record.book.title}》吗？`,
      '确认同意',
      { type: 'success' }
    )
    const res = await borrowRecordAPI.approve(record.id)
    if (res.code === 200) {
      ElMessage.success('已同意借阅申请')
      refreshBorrowRecords()
      loadData()
    }
  } catch {
  }
}

const handleReject = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定拒绝 ${record.borrower.nickname} 的借阅申请吗？`,
      '确认拒绝',
      { type: 'warning' }
    )
    const res = await borrowRecordAPI.reject(record.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝借阅申请')
      refreshBorrowRecords()
      loadData()
    }
  } catch {
  }
}

const handleBatchApprove = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定同意选中的 ${selectedIds.value.length} 项借阅申请吗？`,
      '批量确认同意',
      { type: 'success' }
    )
    const results = await Promise.allSettled(
      selectedIds.value.map(id => borrowRecordAPI.approve(id))
    )
    const successCount = results.filter(r => r.status === 'fulfilled' && r.value.code === 200).length
    ElMessage.success(`成功同意 ${successCount} 项申请`)
    refreshBorrowRecords()
    loadData()
  } catch {
  }
}

const handleBatchReject = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定拒绝选中的 ${selectedIds.value.length} 项借阅申请吗？`,
      '批量确认拒绝',
      { type: 'warning' }
    )
    const results = await Promise.allSettled(
      selectedIds.value.map(id => borrowRecordAPI.reject(id))
    )
    const successCount = results.filter(r => r.status === 'fulfilled' && r.value.code === 200).length
    ElMessage.success(`成功拒绝 ${successCount} 项申请`)
    refreshBorrowRecords()
    loadData()
  } catch {
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 28px;
}

.pending-count-tag {
  font-size: 14px;
  padding: 0 12px;
  height: 28px;
  line-height: 26px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
}

.stat-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.pending {
  background: linear-gradient(135deg, #fdf6ec, #faecd8);
}

.stat-icon.approved {
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
}

.stat-icon.rejected {
  background: linear-gradient(135deg, #fef0f0, #fde2e2);
}

.stat-icon.borrowing {
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  padding: 60px 0;
}

.empty-emoji {
  font-size: 72px;
  line-height: 1;
}

.approval-card {
  border-radius: 10px;
  transition: all 0.3s ease;
}

.approval-card:hover {
  transform: translateY(-2px);
}

.card-selected {
  border: 2px solid #409eff;
  background-color: #f5faff;
}

.card-inner {
  display: flex;
  gap: 16px;
}

.card-checkbox {
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.book-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.book-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.status-tag {
  font-weight: 500;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px 24px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.info-icon {
  font-size: 18px;
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
}

.info-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

.remark-section {
  background-color: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.remark-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  font-weight: 600;
  margin-bottom: 6px;
}

.remark-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  padding-left: 24px;
  word-break: break-word;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .card-inner {
    flex-direction: column;
  }

  .card-actions {
    justify-content: stretch;
  }

  .card-actions .el-button {
    flex: 1;
  }
}
</style>
