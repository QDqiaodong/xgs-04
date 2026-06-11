<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">📖 图书广场</h2>
      <div class="header-actions">
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-radio-group v-model="viewModeStore.viewMode" size="default" class="view-switch">
          <el-radio-button :value="VIEW_MODES.GRID">
            <el-icon><Grid /></el-icon>
            <span class="view-switch-label">网格</span>
          </el-radio-button>
          <el-radio-button :value="VIEW_MODES.LIST">
            <el-icon><List /></el-icon>
            <span class="view-switch-label">列表</span>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <BookFilter
      v-model="filterParams"
      @filter-change="handleFilterChange"
    />

    <el-row v-if="viewModeStore.isGridView()" :gutter="20">
      <el-col
        v-for="book in bookList"
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
          @borrow="handleBorrow"
        />
      </el-col>
    </el-row>

    <div v-else class="book-list-container">
      <BookListItem
        v-for="book in bookList"
        :key="book.id"
        :book="book"
        :current-user-id="currentUserId"
        @borrow="handleBorrow"
      />
    </div>

    <el-empty v-if="bookList.length === 0 && !loading" description="暂无符合条件的图书" />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: center"
      @current-change="loadBooks"
    />

    <BorrowDialog
      v-model="borrowDialogVisible"
      :book="selectedBook"
      @submit="submitBorrow"
    />

    <ExportDialog
      v-model="exportDialogVisible"
      type="book"
      :export-params="exportParams"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Grid, List, Download } from '@element-plus/icons-vue'
import { bookAPI, borrowRecordAPI } from '@/api'
import { loadFavorites, ensureFavoriteCount } from '@/store/favorites'
import { useBookSquareViewMode, VIEW_MODES } from '@/store/viewMode'
import BookFilter from '@/components/BookFilter.vue'
import BookCard from '@/components/BookCard.vue'
import BookListItem from '@/components/BookListItem.vue'
import BorrowDialog from '@/components/BorrowDialog.vue'
import ExportDialog from '@/components/ExportDialog.vue'

const viewModeStore = useBookSquareViewMode()

const currentUserId = 1
const bookList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const filterParams = ref({})

const borrowDialogVisible = ref(false)
const selectedBook = ref(null)
const exportDialogVisible = ref(false)

const exportParams = computed(() => {
  return {
    cityId: filterParams.value.cityId || null,
    categoryId: filterParams.value.categoryId || null,
    available: filterParams.value.available || null,
    keyword: filterParams.value.keyword || '',
    tagIds: filterParams.value.tagIds || [],
    matchAllTags: filterParams.value.matchAllTags || false
  }
})

const loadBooks = async () => {
  loading.value = true
  try {
    const res = await bookAPI.query({
      ...filterParams.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      bookList.value = res.data.content
      total.value = res.data.totalElements
    }
  } catch (e) {
    ElMessage.error('加载图书列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadBooks()
}

const handleBorrow = (book) => {
  selectedBook.value = book
  borrowDialogVisible.value = true
}

const handleExport = () => {
  exportDialogVisible.value = true
}

const submitBorrow = async (data) => {
  try {
    const res = await borrowRecordAPI.create({
      ...data,
      borrowerId: currentUserId
    })
    if (res.code === 200) {
      ElMessage.success('借阅申请已提交，请等待所有者审核')
      loadBooks()
    } else {
      ElMessage.error(res.message || '申请失败')
    }
  } catch (e) {
    ElMessage.error('申请失败')
  }
}

onMounted(async () => {
  const success = await loadFavorites()
  if (!success) {
    ensureFavoriteCount()
  }
  loadBooks()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-switch {
  display: flex;
  align-items: center;
}

.view-switch :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-switch-label {
  font-size: 14px;
}

.book-list-container {
  margin-bottom: 20px;
}
</style>
