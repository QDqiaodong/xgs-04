<template>
  <div class="book-list-item" @click="handleCardClick">
    <div class="book-cover">
      <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.title" />
      <div v-else class="cover-placeholder">
        <span>{{ book.title.charAt(0) }}</span>
      </div>
    </div>
    <div class="book-info">
      <div class="book-title-row">
        <span class="book-title">{{ book.title }}</span>
        <div class="title-actions">
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
      <div class="book-meta">
        <span class="meta-item rating-meta">
          <template v-if="book.reviewCount > 0">
            <el-rate
              :model-value="book.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
              :max="5"
              size="small"
            />
            <span class="review-count-inline">({{ book.reviewCount }}条)</span>
          </template>
          <span v-else class="no-rating-inline">暂无评分</span>
        </span>
        <span class="meta-item">
          <strong>作者：</strong>{{ book.author }}
        </span>
        <span class="meta-item">
          <strong>分类：</strong>{{ book.category?.name }}
        </span>
        <span class="meta-item">
          <strong>成色：</strong>{{ book.conditionLevel || '未填写' }}
        </span>
        <span class="meta-item">
          <strong>城市：</strong>{{ book.city?.cityName }}
        </span>
        <span class="meta-item">
          <strong>所有者：</strong>{{ book.owner?.nickname }}
        </span>
      </div>
      <div v-if="book.tags && book.tags.length > 0" class="book-tags">
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
      <p v-if="book.description" class="book-description">
        {{ book.description }}
      </p>
    </div>
    <div class="list-actions" v-if="showActions">
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
  </div>
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
.book-list-item {
  display: flex;
  align-items: stretch;
  padding: 16px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  gap: 16px;
}

.book-list-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.book-cover {
  width: 80px;
  min-width: 80px;
  height: 110px;
  border-radius: 4px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  color: #fff;
  font-size: 32px;
  font-weight: bold;
}

.book-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.book-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

.book-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.favorite-btn {
  padding: 4px;
  font-size: 18px;
  transition: transform 0.2s ease;
}

.favorite-btn:hover:not(:disabled) {
  transform: scale(1.15);
}

.book-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 8px;
}

.meta-item {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.rating-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.review-count-inline {
  color: #909399;
  font-size: 12px;
}

.no-rating-inline {
  color: #c0c4cc;
  font-size: 12px;
}

.book-description {
  font-size: 13px;
  color: #909399;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.list-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}
</style>
