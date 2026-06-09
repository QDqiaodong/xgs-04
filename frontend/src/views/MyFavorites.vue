<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">⭐ 我的收藏 ({{ favoriteCount }})</h2>
    </div>

    <el-card class="filter-card">
      <el-form :model="filterForm" inline class="filter-form">
        <el-form-item label="所在城市">
          <el-select
            v-model="filterForm.cityId"
            placeholder="选择城市"
            clearable
            style="width: 160px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="city in cities"
              :key="city.id"
              :label="city.cityName"
              :value="city.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图书分类">
          <el-select
            v-model="filterForm.categoryId"
            placeholder="选择分类"
            clearable
            style="width: 160px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input
            v-model="filterForm.keyword"
            placeholder="书名/作者"
            clearable
            style="width: 200px"
            @keyup.enter="handleFilterChange"
            @clear="handleFilterChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilterChange">筛选</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-skeleton v-if="loading" :rows="4" animated style="margin-top: 20px" />

    <el-row v-else :gutter="20">
      <el-col
        v-for="book in filteredBooks"
        :key="book.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        style="margin-bottom: 20px"
      >
        <BookCard
          :book="book"
          :current-user-id="currentUserId"
          @favorite-change="handleFavoriteChange"
        />
      </el-col>
    </el-row>

    <el-empty
      v-if="!loading && favoriteBooks.length === 0"
      description="还没有收藏任何图书，去图书广场看看吧"
    >
      <el-button type="primary" @click="goToSquare">去逛逛</el-button>
    </el-empty>

    <el-empty
      v-if="!loading && favoriteBooks.length > 0 && filteredBooks.length === 0"
      description="没有符合条件的收藏图书"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cityAPI, categoryAPI, favoriteAPI } from '@/api'
import { loadFavorites, refreshFavorites, getFavoriteCount, favoriteStore } from '@/store/favorites'
import BookCard from '@/components/BookCard.vue'

const router = useRouter()
const route = useRoute()
const currentUserId = 1

const favoriteBooks = ref([])
const loading = ref(false)
const cities = ref([])
const categories = ref([])
const filterForm = ref({
  cityId: null,
  categoryId: null,
  keyword: ''
})

const favoriteCount = computed(() => {
  favoriteStore.updateVersion
  return getFavoriteCount()
})

const filteredBooks = computed(() => {
  let result = favoriteBooks.value
  if (filterForm.value.cityId) {
    result = result.filter(b => b.city?.id === filterForm.value.cityId)
  }
  if (filterForm.value.categoryId) {
    result = result.filter(b => b.category?.id === filterForm.value.categoryId)
  }
  if (filterForm.value.keyword) {
    const kw = filterForm.value.keyword.toLowerCase()
    result = result.filter(b =>
      b.title.toLowerCase().includes(kw) ||
      b.author.toLowerCase().includes(kw)
    )
  }
  return result
})

const loadCities = async () => {
  const res = await cityAPI.getAll()
  if (res.code === 200) {
    cities.value = res.data
  }
}

const loadCategories = async () => {
  const res = await categoryAPI.getAll()
  if (res.code === 200) {
    categories.value = res.data
  }
}

const loadFavoriteBooks = async () => {
  loading.value = true
  try {
    await loadFavorites(currentUserId, true)
    const res = await favoriteAPI.getByUser(currentUserId)
    if (res.code === 200) {
      favoriteBooks.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载收藏列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
}

const resetFilter = () => {
  filterForm.value = {
    cityId: null,
    categoryId: null,
    keyword: ''
  }
}

const handleFavoriteChange = async ({ bookId, favorited }) => {
  if (!favorited) {
    favoriteBooks.value = favoriteBooks.value.filter(b => b.id !== bookId)
    await refreshFavorites(currentUserId)
  }
}

const goToSquare = () => {
  router.push('/')
}

onMounted(async () => {
  await Promise.all([loadCities(), loadCategories()])
  loadFavoriteBooks()
})

watch(() => favoriteStore.updateVersion, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== oldVal) {
    loadFavoriteBooks()
  }
})

watch(() => route.fullPath, () => {
  if (route.path === '/my-favorites') {
    loadFavoriteBooks()
  }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
