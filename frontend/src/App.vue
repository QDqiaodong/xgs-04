<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <h1 class="logo">📚 图书互换借阅平台</h1>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="nav-menu"
        >
          <el-menu-item index="/">图书广场</el-menu-item>
          <el-menu-item index="/my-books">我的藏书</el-menu-item>
          <el-menu-item index="/my-favorites">
            <el-badge
              :value="favoriteCount > 0 ? favoriteCount : undefined"
              :max="99"
              :hidden="favoriteCount === 0"
              class="favorite-badge"
            >
              我的收藏
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/borrow-approval">
            <el-badge
              :value="pendingApprovalCount > 0 ? pendingApprovalCount : undefined"
              :max="99"
              :hidden="pendingApprovalCount === 0"
              type="danger"
              class="approval-badge"
            >
              借阅审批
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/borrow-apply">借阅申请</el-menu-item>
          <el-menu-item index="/borrow-history">借阅记录</el-menu-item>
          <el-menu-item index="/overdue-management">
            <el-badge
              :value="overdueCount > 0 ? overdueCount : undefined"
              :max="99"
              :hidden="overdueCount === 0"
              type="danger"
              class="overdue-badge"
            >
              逾期管理
            </el-badge>
          </el-menu-item>
          <el-menu-item index="/notifications">
            <el-badge
              :value="notificationUnreadCount > 0 ? notificationUnreadCount : undefined"
              :max="99"
              :hidden="notificationUnreadCount === 0"
              type="danger"
              class="notification-badge"
            >
              消息中心
            </el-badge>
          </el-menu-item>
        </el-menu>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
    <el-footer class="footer">
      <p>© 2024 图书互换借阅平台 - 同城书友线下流转</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { loadFavorites, getFavoriteCount, ensureFavoriteCount } from '@/store/favorites'
import { loadPendingApprovalCount, getPendingApprovalCount, borrowRecordStore } from '@/store/borrowRecord'
import { loadUnreadCount as loadNotificationUnreadCount, getUnreadCount as getNotificationUnreadCount, notificationStore } from '@/store/notification'
import { borrowRecordAPI } from '@/api'

const route = useRoute()
const activeMenu = computed(() => route.path)

const favoriteCount = computed(() => getFavoriteCount())
const pendingApprovalCount = computed(() => {
  borrowRecordStore.updateVersion
  return getPendingApprovalCount()
})

const overdueCount = ref(0)

const notificationUnreadCount = computed(() => {
  notificationStore.updateVersion
  return getNotificationUnreadCount()
})

const loadOverdueCount = async () => {
  try {
    const res = await borrowRecordAPI.queryOverdue({ pageNum: 1, pageSize: 1 })
    if (res.code === 200) {
      overdueCount.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载逾期数量失败:', e)
  }
}

onMounted(async () => {
  const success = await loadFavorites()
  if (!success) {
    ensureFavoriteCount()
  }
  loadPendingApprovalCount()
  loadOverdueCount()
  loadNotificationUnreadCount()
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.header {
  background-color: #409eff;
  padding: 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.logo {
  color: white;
  font-size: 20px;
  margin: 0;
}

.nav-menu {
  background-color: transparent;
  border: none;
}

.nav-menu .el-menu-item {
  color: white;
}

.nav-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.favorite-badge :deep(.el-badge__content) {
  border: none;
}

.approval-badge :deep(.el-badge__content) {
  border: none;
}

.overdue-badge :deep(.el-badge__content) {
  border: none;
}

.notification-badge :deep(.el-badge__content) {
  border: none;
}

.main-content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.footer {
  text-align: center;
  padding: 20px;
  background-color: #f0f2f5;
  color: #666;
}
</style>
