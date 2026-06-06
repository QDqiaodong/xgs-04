<template>
  <div class="book-detail">
    <el-button type="primary" text @click="goBack" style="margin-bottom: 20px">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <el-skeleton v-if="loading" :rows="8" animated />

    <el-card v-else-if="book" class="detail-card">
      <div class="detail-header">
        <div>
          <h2 class="book-title">
            {{ book.title }}
            <el-tooltip :content="favorited ? '取消收藏' : '收藏'" placement="top">
              <el-button
                class="favorite-btn"
                :type="favorited ? 'danger' : 'default'"
                :icon="favorited ? StarFilled : Star"
                :loading="checkingFavorite"
                circle
                size="large"
                text
                @click="handleToggleFavorite"
              />
            </el-tooltip>
          </h2>
          <el-tag :type="book.available ? 'success' : 'info'" size="large" style="margin-top: 8px">
            {{ book.available ? '可借' : '已借出' }}
          </el-tag>
        </div>
      </div>

      <el-descriptions :column="2" border style="margin-top: 24px">
        <el-descriptions-item label="作者">{{ book.author }}</el-descriptions-item>
        <el-descriptions-item label="ISBN">{{ book.isbn || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ book.category?.name }}</el-descriptions-item>
        <el-descriptions-item label="成色">{{ book.conditionLevel || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="所在城市">{{ book.city?.cityName }}</el-descriptions-item>
        <el-descriptions-item label="所有者">
          {{ book.owner?.nickname }}
          <el-tag v-if="isOwner" type="warning" size="small" style="margin-left: 8px">我发布的</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDate(book.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(book.updateTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="description-section">
        <h4>图书简介</h4>
        <p v-if="book.description" class="description-text">{{ book.description }}</p>
        <el-empty v-else description="暂无简介" :image-size="80" />
      </div>

      <div class="detail-actions">
        <el-button
          v-if="book.available && book.canBorrow && !isOwner"
          type="primary"
          size="large"
          @click="handleBorrow"
        >
          申请借阅
        </el-button>
        <el-button v-if="isOwner" size="large">我的图书</el-button>
      </div>
    </el-card>

    <el-empty v-else description="图书不存在或已被删除" />

    <BorrowDialog
      v-model="borrowDialogVisible"
      :book="book"
      @submit="submitBorrow"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Star, StarFilled } from '@element-plus/icons-vue'
import { bookAPI, borrowRecordAPI } from '@/api'
import {
  isFavorited,
  toggleFavorite,
  loadFavorites,
  checkFavorite,
  favoriteStore,
  ensureFavoriteCount
} from '@/store/favorites'
import BorrowDialog from '@/components/BorrowDialog.vue'

const route = useRoute()
const router = useRouter()

const currentUserId = 1
const book = ref(null)
const loading = ref(false)
const borrowDialogVisible = ref(false)
const checkingFavorite = ref(false)
const localFavorited = ref(null)

const isOwner = computed(() => {
  return currentUserId && book.value?.owner?.id === currentUserId
})

const favorited = computed(() => {
  if (localFavorited.value !== null) {
    return localFavorited.value
  }
  return book.value ? isFavorited(book.value.id) : false
})

const lazyCheckFavorite = async () => {
  if (!book.value) return
  if (favoriteStore.listLoaded) return
  if (favoriteStore.perBookStatus.has(book.value.id)) return
  checkingFavorite.value = true
  try {
    const result = await checkFavorite(book.value.id, currentUserId)
    localFavorited.value = result
  } finally {
    checkingFavorite.value = false
  }
}

const loadBook = async () => {
  loading.value = true
  try {
    const bookId = route.params.id
    const res = await bookAPI.getById(bookId)
    if (res.code === 200) {
      book.value = res.data
      localFavorited.value = null
      lazyCheckFavorite()
    } else {
      ElMessage.error(res.message || '加载图书详情失败')
    }
  } catch (e) {
    ElMessage.error('加载图书详情失败')
  } finally {
    loading.value = false
  }
}

const handleToggleFavorite = async () => {
  if (book.value) {
    const result = await toggleFavorite(book.value.id, currentUserId)
    if (result !== null) {
      localFavorited.value = result
    }
  }
}

const handleBorrow = () => {
  borrowDialogVisible.value = true
}

const submitBorrow = async (data) => {
  try {
    const res = await borrowRecordAPI.create({
      ...data,
      borrowerId: currentUserId
    })
    if (res.code === 200) {
      ElMessage.success('借阅申请已提交，请等待所有者审核')
      loadBook()
    } else {
      ElMessage.error(res.message || '申请失败')
    }
  } catch (e) {
    ElMessage.error('申请失败')
  }
}

const goBack = () => {
  router.back()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(async () => {
  const success = await loadFavorites()
  if (!success) {
    ensureFavoriteCount()
  }
  loadBook()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadBook()
  }
})
</script>

<style scoped>
.book-detail {
  max-width: 900px;
  margin: 0 auto;
}

.detail-card {
  padding: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.book-title {
  font-size: 28px;
  margin: 0;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 16px;
}

.favorite-btn {
  font-size: 28px;
  transition: transform 0.2s ease;
}

.favorite-btn:hover {
  transform: scale(1.15);
}

.description-section {
  margin: 20px 0;
}

.description-section h4 {
  margin-bottom: 12px;
  color: #303133;
}

.description-text {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-actions {
  margin-top: 30px;
  text-align: center;
}
</style>
