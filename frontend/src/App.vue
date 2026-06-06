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
          <el-menu-item index="/borrow-apply">借阅申请</el-menu-item>
          <el-menu-item index="/borrow-history">借阅记录</el-menu-item>
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
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { loadFavorites, getFavoriteCount } from '@/store/favorites'

const route = useRoute()
const activeMenu = computed(() => route.path)

const favoriteCount = computed(() => getFavoriteCount())

onMounted(() => {
  loadFavorites()
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
