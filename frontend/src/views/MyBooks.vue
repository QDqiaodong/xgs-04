<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">📚 我的藏书</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加图书
      </el-button>
    </div>

    <el-row :gutter="20">
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

    <el-empty v-if="bookList.length === 0" description="暂无藏书，快去添加吧" />

    <BookForm
      v-model="formDialogVisible"
      :book="editingBook"
      :owner-id="currentUserId"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { bookAPI } from '@/api'
import BookCard from '@/components/BookCard.vue'
import BookForm from '@/components/BookForm.vue'

const currentUserId = 1
const bookList = ref([])
const showAddDialog = ref(false)
const formDialogVisible = ref(false)
const editingBook = ref(null)

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
</style>
