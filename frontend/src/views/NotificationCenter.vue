<template>
  <div>
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <span class="title-icon">🔔</span>
          消息中心
        </h2>
        <el-tag v-if="unreadCount > 0" type="danger" size="large" effect="dark" class="unread-tag">
          {{ unreadCount }} 条未读
        </el-tag>
        <el-tag v-else type="success" size="large" class="unread-tag">
          全部已读
        </el-tag>
      </div>
      <div class="header-actions">
        <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 150px" @change="handleFilter">
          <el-option label="借阅申请" value="BORROW_REQUEST" />
          <el-option label="审批结果" value="APPROVAL_RESULT" />
          <el-option label="到期提醒" value="DUE_REMINDER" />
          <el-option label="归还确认" value="RETURN_CONFIRM" />
        </el-select>
        <el-button type="primary" :disabled="unreadCount === 0" @click="handleMarkAllRead">
          <el-icon><Check /></el-icon>
          全部已读
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <el-card class="stat-card" shadow="hover" :class="{ active: activeStat === 'all' }" @click="switchStat('all')">
        <div class="stat-content">
          <div class="stat-icon all">📬</div>
          <div class="stat-info">
            <div class="stat-number">{{ totalCount }}</div>
            <div class="stat-label">全部消息</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :class="{ active: activeStat === 'unread' }" @click="switchStat('unread')">
        <div class="stat-content">
          <div class="stat-icon unread">🔴</div>
          <div class="stat-info">
            <div class="stat-number">{{ unreadCount }}</div>
            <div class="stat-label">未读消息</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :class="{ active: activeStat === 'request' }" @click="switchStat('request')">
        <div class="stat-content">
          <div class="stat-icon request">📋</div>
          <div class="stat-info">
            <div class="stat-number">{{ requestCount }}</div>
            <div class="stat-label">借阅申请</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover" :class="{ active: activeStat === 'reminder' }" @click="switchStat('reminder')">
        <div class="stat-content">
          <div class="stat-icon reminder">⏰</div>
          <div class="stat-info">
            <div class="stat-number">{{ reminderCount }}</div>
            <div class="stat-label">到期提醒</div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-loading="loading" class="notification-list">
      <div v-if="notifications.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无消息通知">
          <template #image>
            <div class="empty-emoji">📭</div>
          </template>
        </el-empty>
      </div>

      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleItemClick(item)"
      >
        <div class="item-dot" :class="{ 'is-unread': !item.isRead }"></div>
        <div class="item-icon">
          <span class="icon-emoji">{{ getTypeIcon(item.type) }}</span>
        </div>
        <div class="item-content">
          <div class="item-header">
            <span class="item-title">{{ item.title }}</span>
            <el-tag :type="getTypeTagColor(item.type)" size="small" effect="light">
              {{ getTypeLabel(item.type) }}
            </el-tag>
          </div>
          <div class="item-body">{{ item.content }}</div>
          <div class="item-footer">
            <span class="item-time">{{ formatTime(item.createTime) }}</span>
            <span v-if="!item.isRead" class="mark-read-btn" @click.stop="handleMarkRead(item)">标记已读</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { notificationAPI } from '@/api'
import { refreshNotifications, decrementUnreadCount, resetUnreadCount } from '@/store/notification'

const currentUserId = 1
const loading = ref(false)
const notifications = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterType = ref('')
const activeStat = ref('all')

const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)
const requestCount = computed(() => notifications.value.filter(n => n.type === 'BORROW_REQUEST').length)
const reminderCount = computed(() => notifications.value.filter(n => n.type === 'DUE_REMINDER').length)
const totalCount = computed(() => notifications.value.length)

const getTypeIcon = (type) => {
  const icons = {
    BORROW_REQUEST: '📋',
    APPROVAL_RESULT: '✅',
    DUE_REMINDER: '⏰',
    RETURN_CONFIRM: '📦'
  }
  return icons[type] || '🔔'
}

const getTypeLabel = (type) => {
  const labels = {
    BORROW_REQUEST: '借阅申请',
    APPROVAL_RESULT: '审批结果',
    DUE_REMINDER: '到期提醒',
    RETURN_CONFIRM: '归还确认'
  }
  return labels[type] || '通知'
}

const getTypeTagColor = (type) => {
  const colors = {
    BORROW_REQUEST: 'warning',
    APPROVAL_RESULT: 'success',
    DUE_REMINDER: 'danger',
    RETURN_CONFIRM: 'info'
  }
  return colors[type] || ''
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      userId: currentUserId,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (filterType.value) {
      params.type = filterType.value
    }
    if (activeStat.value === 'unread') {
      params.isRead = false
    }
    const res = await notificationAPI.query(params)
    if (res.code === 200) {
      notifications.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载消息列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  loadData()
}

const switchStat = (stat) => {
  activeStat.value = stat
  filterType.value = stat === 'request' ? 'BORROW_REQUEST' : stat === 'reminder' ? 'DUE_REMINDER' : ''
  currentPage.value = 1
  loadData()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadData()
}

const handleItemClick = async (item) => {
  if (!item.isRead) {
    await handleMarkRead(item)
  }
}

const handleMarkRead = async (item) => {
  try {
    const res = await notificationAPI.markAsRead(item.id, currentUserId)
    if (res.code === 200) {
      item.isRead = true
      decrementUnreadCount()
    }
  } catch (e) {
    ElMessage.error('标记已读失败')
  }
}

const handleMarkAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定将所有消息标记为已读吗？', '确认操作', { type: 'info' })
    const res = await notificationAPI.markAllAsRead(currentUserId)
    if (res.code === 200) {
      ElMessage.success(`已将 ${res.data.updatedCount} 条消息标记为已读`)
      resetUnreadCount()
      loadData()
    }
  } catch {
  }
}

onMounted(() => {
  loadData()
  refreshNotifications(currentUserId)
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

.unread-tag {
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
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card.active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
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

.stat-icon.all {
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
}

.stat-icon.unread {
  background: linear-gradient(135deg, #fef0f0, #fde2e2);
}

.stat-icon.request {
  background: linear-gradient(135deg, #fdf6ec, #faecd8);
}

.stat-icon.reminder {
  background: linear-gradient(135deg, #fef0f0, #fde2e2);
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

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  padding: 60px 0;
}

.empty-emoji {
  font-size: 72px;
  line-height: 1;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.notification-item.unread {
  background: #f5faff;
  border-color: #d9ecff;
}

.item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dcdfe6;
  margin-top: 8px;
  flex-shrink: 0;
}

.item-dot.is-unread {
  background: #f56c6c;
}

.item-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-emoji {
  font-size: 20px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 10px;
  flex-wrap: wrap;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.item-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  word-break: break-word;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.mark-read-btn {
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
  transition: color 0.2s;
}

.mark-read-btn:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
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

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .notification-item {
    padding: 14px;
  }
}
</style>
