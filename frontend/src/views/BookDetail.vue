<template>
  <div class="book-detail-page">
    <div class="detail-header-bar">
      <el-button text class="back-btn" @click="goBack">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <span class="header-title">图书详情</span>
      <el-button text class="favorite-btn-header" @click="handleToggleFavorite">
        <el-tooltip :content="favorited ? '取消收藏' : '收藏'" placement="bottom">
          <el-icon :size="22" :class="{ 'is-favorited': favorited }">
            <component :is="favorited ? StarFilled : Star" />
          </el-icon>
        </el-tooltip>
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="10" animated class="skeleton-wrapper" />

    <div v-else-if="book" class="detail-content">
      <div class="book-cover-section">
        <div class="cover-wrapper">
          <div class="book-cover-placeholder">
            <el-icon :size="80" class="cover-icon"><Reading /></el-icon>
            <div class="cover-category">{{ book.category?.name || '图书' }}</div>
          </div>
        </div>
        <div class="book-basic-info">
          <h1 class="book-title">{{ book.title }}</h1>
          <div class="book-author">
            <el-icon><User /></el-icon>
            <span>{{ book.author }}</span>
          </div>
          <div class="book-tags-wrap">
            <el-tag :type="book.available ? 'success' : 'info'" size="large" effect="dark">
              {{ book.available ? '可借' : '已借出' }}
            </el-tag>
            <el-tag v-if="book.canBorrow" type="primary" size="large" effect="plain">
              可出借
            </el-tag>
            <el-tag v-else type="warning" size="large" effect="plain">
              暂不可借
            </el-tag>
          </div>
          <div class="rating-wrap" v-if="book.reviewCount > 0">
            <el-rate
              :model-value="book.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
              :max="5"
              size="large"
            />
            <span class="review-count-text">{{ book.reviewCount }} 条评价</span>
          </div>
          <div class="rating-wrap" v-else>
            <span class="no-rating-text">暂无评分</span>
          </div>
          <div v-if="book.tags && book.tags.length > 0" class="book-tags">
            <el-tag
              v-for="tag in book.tags"
              :key="tag.id"
              size="large"
              :style="tag.color ? { borderColor: tag.color, color: tag.color, backgroundColor: tag.color + '15' } : {}"
              effect="light"
            >
              {{ tag.name }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-card class="info-section" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><InfoFilled /></el-icon>
            <span>基本信息</span>
          </div>
        </template>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="ISBN">{{ book.isbn || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ book.category?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ book.conditionLevel || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="所在城市">
            <el-icon><Location /></el-icon>
            {{ book.city?.cityName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatDate(book.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(book.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="info-section" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><UserFilled /></el-icon>
            <span>所有者信息</span>
            <el-tag v-if="isOwner" type="warning" size="small" effect="dark">我发布的</el-tag>
          </div>
        </template>
        <div class="owner-info">
          <div class="owner-avatar">
            <el-icon :size="32"><Avatar /></el-icon>
          </div>
          <div class="owner-detail">
            <div class="owner-name">{{ book.owner?.nickname || '未知用户' }}</div>
            <div class="owner-username">@{{ book.owner?.username || '-' }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="info-section" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>详细描述</span>
          </div>
        </template>
        <div v-if="book.description" class="description-content">
          {{ book.description }}
        </div>
        <el-empty v-else description="暂无详细描述" :image-size="60" />
      </el-card>

      <el-card class="info-section" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><Clock /></el-icon>
            <span>借阅历史</span>
            <el-tag size="small" type="info">{{ borrowHistoryTotal }} 条记录</el-tag>
          </div>
        </template>
        <div v-loading="borrowHistoryLoading" class="borrow-history-wrap">
          <div v-if="borrowHistoryList.length > 0" class="borrow-history-list">
            <div v-for="record in borrowHistoryList" :key="record.id" class="borrow-history-item">
              <div class="history-item-left">
                <el-avatar :size="36" class="history-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="history-info">
                  <div class="history-borrower">
                    {{ record.borrower?.nickname || '匿名用户' }}
                  </div>
                  <div class="history-time">
                    {{ formatDate(record.borrowTime || record.createTime) }}
                  </div>
                </div>
              </div>
              <div class="history-item-right">
                <el-tag :type="getStatusType(record.status)" size="small">
                  {{ getStatusText(record.status) }}
                </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无借阅记录" :image-size="60" />
          <el-pagination
            v-if="borrowHistoryTotal > borrowHistoryPageSize"
            v-model:current-page="borrowHistoryPage"
            v-model:page-size="borrowHistoryPageSize"
            :total="borrowHistoryTotal"
            layout="prev, pager, next"
            small
            @current-change="loadBorrowHistory"
            @size-change="handleBorrowHistorySizeChange"
          />
        </div>
      </el-card>

      <el-card class="info-section review-section-card" shadow="never">
        <template #header>
          <div class="section-header">
            <el-icon><ChatDotRound /></el-icon>
            <span>读者评价</span>
            <el-tag size="small" type="info">{{ reviewTotal }} 条</el-tag>
          </div>
        </template>

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
              <el-rate v-model="reviewForm.rating" :max="5" size="large" />
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

        <div class="review-sort-bar">
          <span class="sort-label">排序方式：</span>
          <el-radio-group v-model="reviewSortBy" size="small" @change="loadReviews">
            <el-radio-button value="latest">最新</el-radio-button>
            <el-radio-button value="rating">评分最高</el-radio-button>
          </el-radio-group>
        </div>

        <div v-loading="reviewsLoading" class="review-list-wrap">
          <div v-if="reviewList.length > 0" class="review-list">
            <div v-for="review in reviewList" :key="review.id" class="review-item">
              <div class="review-item-header">
                <el-avatar :size="36" class="review-avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="reviewer-info">
                  <span class="reviewer-name">{{ review.user?.nickname || '匿名用户' }}</span>
                  <span class="review-time">{{ formatDate(review.createTime) }}</span>
                </div>
                <el-rate :model-value="review.rating" disabled :max="5" size="small" class="review-rate" />
              </div>
              <div class="review-item-content" v-if="review.content">
                {{ review.content }}
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评价，快来抢沙发吧！" :image-size="60" />

          <el-pagination
            v-if="reviewTotal > reviewPageSize"
            v-model:current-page="reviewPage"
            v-model:page-size="reviewPageSize"
            :total="reviewTotal"
            layout="prev, pager, next"
            small
            @current-change="loadReviews"
            @size-change="handleReviewPageSizeChange"
          />
        </div>
      </el-card>

      <div class="bottom-placeholder"></div>
    </div>

    <el-card v-else description="图书不存在或已被删除" class="not-found-card" />

    <div class="bottom-action-bar" v-if="book">
      <div class="action-left">
        <div class="action-item" @click="handleToggleFavorite">
          <el-icon :size="20" :class="{ 'is-favorited': favorited }">
            <component :is="favorited ? StarFilled : Star" />
          </el-icon>
          <span>{{ favorited ? '已收藏' : '收藏' }}</span>
        </div>
        <div class="action-item" @click="handleShare">
          <el-icon :size="20"><Share /></el-icon>
          <span>分享</span>
        </div>
      </div>
      <div class="action-right">
        <el-button
          v-if="isOwner"
          type="warning"
          size="large"
          class="action-btn edit-btn"
          @click="handleEditBook"
        >
          <el-icon><Edit /></el-icon>
          编辑图书
        </el-button>
        <template v-else>
          <el-button
            v-if="book.available && book.canBorrow"
            type="primary"
            size="large"
            class="action-btn primary-btn"
            :loading="borrowSubmitting"
            :disabled="borrowSubmitting"
            @click="handleBorrow"
          >
            <el-icon><Reading /></el-icon>
            申请借阅
          </el-button>
          <el-button
            v-else-if="book.canBorrow && !book.available"
            type="success"
            size="large"
            class="action-btn reserve-btn"
            :loading="reservationSubmitting"
            :disabled="reservationSubmitting"
            @click="handleReserve"
          >
            <el-icon><Clock /></el-icon>
            预约排队
          </el-button>
          <el-button
            v-else
            type="info"
            size="large"
            class="action-btn"
            disabled
          >
            暂不可借
          </el-button>
        </template>
      </div>
    </div>

    <BorrowDialog
      v-model="borrowDialogVisible"
      :book="book"
      @submit="submitBorrow"
    />

    <BookForm
      v-model="editDialogVisible"
      :book="book"
      :owner-id="currentUserId"
      @submit="handleBookUpdated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Star, StarFilled, User, Location, InfoFilled,
  Document, Clock, ChatDotRound, Share, Edit,
  Reading, UserFilled, Avatar
} from '@element-plus/icons-vue'
import { bookAPI, borrowRecordAPI, reviewAPI, reservationAPI } from '@/api'
import {
  isFavorited,
  toggleFavorite,
  loadFavorites,
  checkFavorite,
  favoriteStore,
  ensureFavoriteCount
} from '@/store/favorites'
import BorrowDialog from '@/components/BorrowDialog.vue'
import BookForm from '@/components/BookForm.vue'

const route = useRoute()
const router = useRouter()

const currentUserId = 1
const book = ref(null)
const loading = ref(false)
const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const reservationSubmitting = ref(false)
const checkingFavorite = ref(false)
const editDialogVisible = ref(false)

const borrowHistoryList = ref([])
const borrowHistoryLoading = ref(false)
const borrowHistoryPage = ref(1)
const borrowHistoryPageSize = ref(5)
const borrowHistoryTotal = ref(0)

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
  favoriteStore.updateVersion
  return book.value ? isFavorited(book.value.id) : false
})

const lazyCheckFavorite = async () => {
  if (!book.value) return
  if (favoriteStore.listLoaded) return
  if (book.value.id in favoriteStore.perBookStatus) return
  checkingFavorite.value = true
  try {
    await checkFavorite(book.value.id, currentUserId)
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

const loadBorrowHistory = async () => {
  if (!book.value) return
  borrowHistoryLoading.value = true
  try {
    const res = await borrowRecordAPI.query({
      bookId: book.value.id,
      pageNum: borrowHistoryPage.value,
      pageSize: borrowHistoryPageSize.value
    })
    if (res.code === 200) {
      borrowHistoryList.value = res.data.list
      borrowHistoryTotal.value = res.data.total
    }
  } catch (e) {
    console.error('加载借阅历史失败', e)
  } finally {
    borrowHistoryLoading.value = false
  }
}

const handleBorrowHistorySizeChange = () => {
  borrowHistoryPage.value = 1
  loadBorrowHistory()
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
      ElMessage.success(result ? '收藏成功' : '取消收藏成功')
    }
  }
}

const handleBorrow = () => {
  borrowDialogVisible.value = true
}

const submitBorrow = async (data) => {
  if (borrowSubmitting.value) return
  borrowSubmitting.value = true
  try {
    const res = await borrowRecordAPI.create({
      ...data,
      borrowerId: currentUserId
    })
    if (res.code === 200) {
      ElMessage.success('借阅申请已提交，请等待所有者审核')
      loadBook()
      return true
    } else {
      ElMessage.error(res.message || '申请失败')
      return false
    }
  } catch (e) {
    ElMessage.error('申请失败')
    return false
  } finally {
    borrowSubmitting.value = false
  }
}

const handleReserve = async () => {
  if (reservationSubmitting.value) return
  reservationSubmitting.value = true
  try {
    const res = await reservationAPI.create({
      bookId: book.value.id,
      userId: currentUserId,
      ownerId: book.value.owner?.id
    })
    if (res.code === 200) {
      ElMessage.success('预约成功，请耐心等待通知')
      loadBook()
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch (e) {
    ElMessage.error('预约失败')
  } finally {
    reservationSubmitting.value = false
  }
}

const handleEditBook = () => {
  editDialogVisible.value = true
}

const handleBookUpdated = () => {
  editDialogVisible.value = false
  ElMessage.success('图书信息更新成功')
  loadBook()
}

const handleShare = () => {
  ElMessage.info('分享功能开发中...')
}

const goBack = () => {
  router.back()
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusType = (status) => {
  const statusMap = {
    PENDING: 'warning',
    APPROVED: 'primary',
    BORROWING: 'success',
    RETURNED: 'info',
    REJECTED: 'danger',
    OVERDUE: 'danger',
    CANCELLED: 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    PENDING: '待审核',
    APPROVED: '已同意',
    BORROWING: '借阅中',
    RETURNED: '已归还',
    REJECTED: '已拒绝',
    OVERDUE: '已逾期',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

onMounted(async () => {
  const success = await loadFavorites()
  if (!success) {
    ensureFavoriteCount()
  }
  loadBook()
  loadReviews()
  loadBorrowHistory()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadBook()
    reviewPage.value = 1
    borrowHistoryPage.value = 1
    loadReviews()
    loadBorrowHistory()
  }
})
</script>

<style scoped>
.book-detail-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f7ff 0%, #f5f7fa 300px);
  position: relative;
  padding-bottom: 80px;
}

.detail-header-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.back-btn,
.favorite-btn-header {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #303133;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.skeleton-wrapper {
  padding: 16px;
}

.detail-content {
  padding: 16px;
}

.book-cover-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.06);
}

.cover-wrapper {
  flex-shrink: 0;
}

.book-cover-placeholder {
  width: 120px;
  height: 160px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.book-cover-placeholder::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%);
}

.cover-icon {
  opacity: 0.9;
  margin-bottom: 8px;
}

.cover-category {
  font-size: 14px;
  font-weight: 500;
  opacity: 0.95;
  max-width: 100px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-basic-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.book-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  color: #303133;
  line-height: 1.3;
  word-break: break-word;
}

.book-author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 15px;
}

.book-tags-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.rating-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-count-text {
  color: #909399;
  font-size: 13px;
}

.no-rating-text {
  color: #c0c4cc;
  font-size: 14px;
}

.book-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.info-section {
  margin-bottom: 16px;
  border-radius: 12px !important;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.section-header .el-tag {
  margin-left: auto;
}

.description-content {
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 14px;
}

.owner-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.owner-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f0f7ff 0%, #e0eaf5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}

.owner-detail {
  flex: 1;
}

.owner-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.owner-username {
  font-size: 13px;
  color: #909399;
}

.borrow-history-wrap {
  min-height: 100px;
}

.borrow-history-list {
  display: flex;
  flex-direction: column;
}

.borrow-history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.borrow-history-item:last-child {
  border-bottom: none;
}

.history-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-avatar {
  background: #f0f2f5;
  color: #909399;
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-borrower {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.history-time {
  color: #909399;
  font-size: 12px;
}

.review-section-card {
  margin-bottom: 20px;
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

.review-sort-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.sort-label {
  font-size: 14px;
  color: #606266;
}

.review-list-wrap {
  min-height: 100px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 14px;
  background: #fafafa;
  border-radius: 8px;
}

.review-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.review-avatar {
  background: #f0f2f5;
  color: #909399;
  flex-shrink: 0;
}

.reviewer-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.reviewer-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.review-time {
  color: #909399;
  font-size: 12px;
}

.review-rate {
  flex-shrink: 0;
}

.review-item-content {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 14px;
  padding-left: 48px;
}

.bottom-placeholder {
  height: 20px;
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  padding-bottom: calc(10px + env(safe-area-inset-bottom));
}

.action-left {
  display: flex;
  gap: 20px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: #606266;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
}

.action-item:hover {
  color: #409eff;
}

.action-item .is-favorited {
  color: #f56c6c;
}

.action-right {
  flex: 1;
  max-width: 280px;
}

.action-btn {
  width: 100%;
  font-weight: 600;
  border-radius: 24px !important;
}

.primary-btn {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border: none;
}

.reserve-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border: none;
}

.edit-btn {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
  border: none;
}

.not-found-card {
  margin: 20px 16px;
  border-radius: 12px !important;
}

.bottom-action-bar .el-pagination {
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 480px) {
  .book-cover-section {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .book-tags-wrap,
  .rating-wrap,
  .book-tags {
    justify-content: center;
  }

  .book-author {
    justify-content: center;
  }

  .review-item-content {
    padding-left: 0;
  }
}
</style>
