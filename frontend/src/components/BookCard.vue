<template>
  <el-card class="book-card" shadow="hover" @click="handleCardClick">
    <template #header>
      <div class="card-header">
        <span class="book-title">{{ book.title }}</span>
        <div class="header-actions">
          <el-tooltip :content="favorited ? '取消收藏' : '收藏'" placement="top">
            <el-button
              class="favorite-btn"
              :type="favorited ? 'danger' : 'default'"
              :icon="favorited ? StarFilled : Star"
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
      <p><strong>作者：</strong>{{ book.author }}</p>
      <p><strong>分类：</strong>{{ book.category?.name }}</p>
      <p><strong>成色：</strong>{{ book.conditionLevel || '未填写' }}</p>
      <p><strong>城市：</strong>{{ book.city?.cityName }}</p>
      <p><strong>所有者：</strong>{{ book.owner?.nickname }}</p>
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { isFavorited, toggleFavorite } from '@/store/favorites'

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

const isOwner = computed(() => {
  return props.currentUserId && props.book.owner?.id === props.currentUserId
})

const favorited = computed(() => isFavorited(props.book.id))

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

.favorite-btn:hover {
  transform: scale(1.15);
}

.book-title {
  font-weight: bold;
  font-size: 16px;
}

.book-info {
  flex: 1;
}

.book-info p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
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
