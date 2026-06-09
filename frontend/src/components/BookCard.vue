<template>
  <el-card class="book-card" shadow="hover" @click="handleCardClick">
    <template #header>
      <div class="card-header">
        <span class="book-title">{{ book.title }}</span>
        <div class="header-actions">
          <el-tooltip :content="favorited ? '取消收藏' : '收藏'" placement="top">
            <el-button
              class="favorite-btn"
              :class="{ 'is-loading': checkingFavorite }"
              :type="favorited ? 'danger' : 'default'"
              :icon="favorited ? StarFilled : Star"
              :loading="checkingFavorite"
              circle
              size="small"
              text
              @click.stop="handleToggleFavorite"
            />
          </el-tooltip>
          <el-tag :type="book.available ? 'success' : 'info'" size="small">
            {{ book.available ? '可借' : '已借出' }}
          </el-tag>
        </div>
      </div>
    </template>
    <div class="book-info">
      <div class="rating-section" v-if="book.reviewCount > 0">
        <div class="rating-stars">
          <el-rate
            :model-value="book.averageRating"
            disabled
            show-score
            text-color="#ff9900"
            score-template="{value}"
            :max="5"
          />
        </div>
        <span class="review-count">{{ book.reviewCount }} 条评价</span>
      </div>
      <div class="rating-section" v-else>
        <span class="no-rating">暂无评分</span>
      </div>
      <p><strong>作者：</strong>{{ book.author }}</p>
      <p><strong>分类：</strong>{{ book.category?.name }}</p>
      <p><strong>成色：</strong>{{ book.conditionLevel || '未填写' }}</p>
      <p><strong>城市：</strong>{{ book.city?.cityName }}</p>
      <p><strong>所有者：</strong>{{ book.owner?.nickname }}</p>
      <div v-if="book.tags && book.tags.length > 0" class="card-book-tags">
        <el-tag
          v-for="tag in book.tags"
          :key="tag.id"
          size="small"
          :style="tag.color ? { borderColor: tag.color, color: tag.color } : {}"
          effect="light"
        >
          {{ tag.name }}
        </el-tag>
      </div>
      <p v-if="book.description" class="description">
        <strong>简介：</strong>{{ book.description }}
      </p>
    </div>
    <div class="card-actions" v-if="showActions">
      <slot name="actions" :book="book">
        <el-button
          v-if="book.available && book.canBorrow && !isOwner"
          type="primary"
          size="small"
          @click.stop="$emit('borrow', book)"
        >
          申请借阅
        </el-button>
      </slot>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Star, StarFilled } from '@element-plus/icons-vue'
import {
  isFavorited,
  toggleFavorite,
  checkFavorite,
  favoriteStore
} from '@/store/favorites'

const props = defineProps({
  book: {
    type: Object,
    required: true
  },
  currentUserId: {
    type: Number,
    default: null
  },
  showActions: {
    type: Boolean,
    default: true
  },
  clickable: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['borrow', 'favorite-change'])

const router = useRouter()
const checkingFavorite = ref(false)

const isOwner = computed(() => {
  return props.currentUserId && props.book.owner?.id === props.currentUserId
})

const favorited = computed(() => {
  favoriteStore.updateVersion
  return isFavorited(props.book.id)
})

const lazyCheckFavorite = async () => {
  if (favoriteStore.listLoaded) {
    return
  }
  if (props.book.id in favoriteStore.perBookStatus) {
    return
  }
  checkingFavorite.value = true
  try {
    await checkFavorite(props.book.id, props.currentUserId || 1)
  } finally {
    checkingFavorite.value = false
  }
}

const handleToggleFavorite = async () => {
  const result = await toggleFavorite(props.book.id, props.currentUserId || 1)
  if (result !== null) {
    emit('favorite-change', { bookId: props.book.id, favorited: result })
  }
}

const handleCardClick = () => {
  if (props.clickable) {
    router.push(`/book/${props.book.id}`)
  }
}

onMounted(() => {
  lazyCheckFavorite()
})

watch(() => props.book?.id, () => {
  if (props.book?.id != null) {
    lazyCheckFavorite()
  }
})
</script>

<style scoped>
.book-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.book-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.favorite-btn {
  padding: 4px;
  font-size: 18px;
  transition: transform 0.2s ease;
}

.favorite-btn:hover:not(:disabled) {
  transform: scale(1.15);
}

.book-title {
  font-weight: bold;
  font-size: 16px;
}

.book-info {
  flex: 1;
}

.rating-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.rating-stars :deep(.el-rate) {
  display: inline-flex;
  align-items: center;
}

.review-count {
  color: #909399;
  font-size: 13px;
}

.no-rating {
  color: #c0c4cc;
  font-size: 13px;
}

.book-info p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
}

.card-book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0;
}

.description {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  text-align: right;
}
</style>
