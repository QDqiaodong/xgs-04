<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">📚 我的藏书</h2>
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
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加图书
        </el-button>
      </div>
    </div>

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
        <BookCard :book="book" :current-user-id="currentUserId">
          <template #actions="{ book }">
            <el-button size="small" @click="handleEdit(book)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(book)">删除</el-button>
          </template>
        </BookCard>
      </el-col>
    </el-row>

    <div v-else class="book-list-container">
      <BookListItem
        v-for="book in bookList"
        :key="book.id"
        :book="book"
        :current-user-id="currentUserId"
      >
        <template #actions="{ book }">
          <el-button size="small" @click="handleEdit(book)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(book)">删除</el-button>
        </template>
      </BookListItem>
    </div>

    <el-empty v-if="bookList.length === 0" description="暂无藏书，快去添加吧" />

    <BookForm
      ref="bookFormRef"
      v-model="formDialogVisible"
      :book="editingBook"
      :owner-id="currentUserId"
      @submit="handleSubmit"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Grid, List, Download } from '@element-plus/icons-vue'
import { bookAPI } from '@/api'
import { useMyBooksViewMode, VIEW_MODES } from '@/store/viewMode'
import BookCard from '@/components/BookCard.vue'
import BookListItem from '@/components/BookListItem.vue'
import BookForm from '@/components/BookForm.vue'
import ExportDialog from '@/components/ExportDialog.vue'

const viewModeStore = useMyBooksViewMode()

const currentUserId = 1
const bookList = ref([])
const formDialogVisible = ref(false)
const editingBook = ref(null)
const bookFormRef = ref(null)
const exportDialogVisible = ref(false)

const exportParams = computed(() => ({
  ownerId: currentUserId
}))

const handleAdd = () => {
  editingBook.value = null
  formDialogVisible.value = true
}

const handleExport = () => {
  exportDialogVisible.value = true
}

const loadBooks = async () => {
  try {
    const res = await bookAPI.getByOwner(currentUserId)
    if (res.code === 200) {
      bookList.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载藏书列表失败')
  }
}

const handleEdit = (book) => {
  editingBook.value = book
  formDialogVisible.value = true
}

const handleDelete = async (book) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${book.title}》吗？`,
      '提示',
      { type: 'warning' }
    )
    const res = await bookAPI.delete(book.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadBooks()
    }
  } catch {
  }
}

const handleSubmit = async (data) => {
  try {
    let res
    if (editingBook.value) {
      res = await bookAPI.update(editingBook.value.id, data)
    } else {
      res = await bookAPI.create(data)
    }
    if (res.code === 200) {
      ElMessage.success(editingBook.value ? '更新成功' : '添加成功')
      bookFormRef.value?.clearDraft()
      formDialogVisible.value = false
      editingBook.value = null
      loadBooks()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
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
