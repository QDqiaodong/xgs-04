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
          <div class="header-tags">
            <el-tag :type="book.available ? 'success' : 'info'" size="large">
              {{ book.available ? '可借' : '已借出' }}
            </el-tag>
            <div class="rating-summary" v-if="book.reviewCount > 0">
              <el-rate
                :model-value="book.averageRating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
                :max="5"
              />
              <span class="review-count-total">{{ book.reviewCount }} 条评价</span>
            </div>
            <span class="no-rating" v-else>暂无评分</span>
          </div>
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

    <el-card v-else description="图书不存在或已被删除" />

    <el-card v-if="book" class="review-section" shadow="never">
      <div class="review-header">
        <h3>读者评价</h3>
        <el-radio-group v-model="reviewSortBy" size="default" @change="loadReviews">
          <el-radio-button value="latest">最新</el-radio-button>
          <el-radio-button value="rating">评分最高</el-radio-button>
        </el-radio-group>
      </div>

      <el-alert
        v-if="canReview && !hasReviewed"
        type="success"
        :closable="false"
        show-icon
        class="review-alert"
      >
        <template #title>
          <div class="review-alert-content">
            <span>您已完成借阅，请对这本图书进行评价</span>
            <el-button type="primary" size="small" @click="showReviewForm = true">去评价</el-button>
          </div>
        </template>
      </el-alert>

      <el-alert
        v-if="hasReviewed"
        type="info"
        :closable="false"
        show-icon
        class="review-alert"
      >
        <template #title>您已评价过这本图书</template>
      </el-alert>

      <div v-if="showReviewForm" class="review-form-wrapper">
        <el-divider />
        <h4>发表评价</h4>
        <el-form :model="reviewForm" label-width="80px" ref="reviewFormRef">
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" :max="5" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              placeholder="请分享您的借阅体验和对图书的评价（选填）"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
            <el-button @click="showReviewForm = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div v-loading="reviewsLoading">
        <div v-if="reviewList.length > 0" class="review-list">
          <div v-for="review in reviewList" :key="review.id" class="review-item">
            <div class="review-item-header">
              <span class="reviewer-name">{{ review.user?.nickname || '匿名用户' }}</span>
              <el-rate :model-value="review.rating" disabled :max="5" size="small" />
              <span class="review-time">{{ formatDate(review.createTime) }}</span>
            </div>
            <div class="review-item-content" v-if="review.content">
              {{ review.content }}
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无评价，快来抢沙发吧！" :image-size="80" />

        <el-pagination
          v-if="reviewTotal > 0"
          v-model:current-page="reviewPage"
          v-model:page-size="reviewPageSize"
          :total="reviewTotal"
          layout="total, prev, pager, next, jumper"
          style="margin-top: 20px; justify-content: center"
          @current-change="loadReviews"
          @size-change="handleReviewPageSizeChange"
        />
      </div>
    </el-card>

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
import { bookAPI, borrowRecordAPI, reviewAPI } from '@/api'
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

const reviewList = ref([])
const reviewsLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = ref(5)
const reviewTotal = ref(0)
const reviewSortBy = ref('latest')

const showReviewForm = ref(false)
const submittingReview = ref(false)
const reviewFormRef = ref(null)
const reviewForm = ref({
  rating: 5,
  content: ''
})

const canReview = ref(false)
const hasReviewed = ref(false)
const borrowRecordForReview = ref(null)

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
      checkReviewEligibility()
    } else {
      ElMessage.error(res.message || '加载图书详情失败')
    }
  } catch (e) {
    ElMessage.error('加载图书详情失败')
  } finally {
    loading.value = false
  }
}

const checkReviewEligibility = async () => {
  if (!book.value) return
  try {
    const res = await borrowRecordAPI.getByBorrower(currentUserId)
    if (res.code === 200) {
      const returnedRecords = res.data.filter(
        r => r.book?.id === book.value.id && r.status === 'RETURNED'
      )
      if (returnedRecords.length > 0) {
        canReview.value = true
        borrowRecordForReview.value = returnedRecords[0]
        const reviewRes = await reviewAPI.hasReviewedBorrowRecord(returnedRecords[0].id)
        if (reviewRes.code === 200) {
          hasReviewed.value = reviewRes.data
        }
      }
    }
  } catch (e) {
    console.error('检查评价资格失败', e)
  }
}

const loadReviews = async () => {
  if (!book.value) return
  reviewsLoading.value = true
  try {
    const sortParam = reviewSortBy.value === 'rating' ? 'rating' : 'latest'
    const res = await reviewAPI.query({
      bookId: book.value.id,
      pageNum: reviewPage.value,
      pageSize: reviewPageSize.value,
      sortBy: sortParam
    })
    if (res.code === 200) {
      reviewList.value = res.data.list
      reviewTotal.value = res.data.total
    }
  } catch (e) {
    ElMessage.error('加载评价失败')
  } finally {
    reviewsLoading.value = false
  }
}

const handleReviewPageSizeChange = () => {
  reviewPage.value = 1
  loadReviews()
}

const submitReview = async () => {
  if (!reviewForm.value.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingReview.value = true
  try {
    const res = await reviewAPI.create({
      bookId: book.value.id,
      userId: currentUserId,
      borrowRecordId: borrowRecordForReview.value?.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content
    })
    if (res.code === 200) {
      ElMessage.success('评价提交成功')
      showReviewForm.value = false
      hasReviewed.value = true
      reviewForm.value = { rating: 5, content: '' }
      reviewPage.value = 1
      loadBook()
      loadReviews()
    } else {
      ElMessage.error(res.message || '评价提交失败')
    }
  } catch (e) {
    ElMessage.error('评价提交失败')
  } finally {
    submittingReview.value = false
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
  loadReviews()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadBook()
    reviewPage.value = 1
    loadReviews()
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

.header-tags {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.rating-summary {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-count-total {
  color: #909399;
  font-size: 14px;
}

.no-rating {
  color: #c0c4cc;
  font-size: 14px;
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

.review-section {
  margin-top: 20px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.review-header h3 {
  margin: 0;
  color: #303133;
}

.review-alert {
  margin-bottom: 16px;
}

.review-alert-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.review-form-wrapper h4 {
  margin-bottom: 16px;
  color: #303133;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.review-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.reviewer-name {
  font-weight: 600;
  color: #303133;
}

.review-time {
  color: #909399;
  font-size: 13px;
  margin-left: auto;
}

.review-item-content {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
