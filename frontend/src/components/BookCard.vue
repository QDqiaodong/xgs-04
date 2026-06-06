<template>
  <el-card class="book-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span class="book-title">{{ book.title }}</span>
        <el-tag :type="book.available ? 'success' : 'info'" size="small">
          {{ book.available ? '可借' : '已借出' }}
        </el-tag>
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
          @click="$emit('borrow', book)"
        >
          申请借阅
        </el-button>
      </slot>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

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
  }
})

const emit = defineEmits(['borrow'])

const isOwner = computed(() => {
  return props.currentUserId && props.book.owner?.id === props.currentUserId
})
</script>

<style scoped>
.book-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
