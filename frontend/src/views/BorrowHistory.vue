<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">📋 借阅历史记录</h2>
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-header" @click="filterCollapsed = !filterCollapsed">
        <div class="filter-title">
          <el-icon><Filter /></el-icon>
          <span>高级筛选</span>
          <el-tag v-if="activeFilterCount > 0" type="danger" size="small" class="filter-count">
            {{ activeFilterCount }} 个条件
          </el-tag>
        </div>
        <el-icon class="collapse-icon" :class="{ rotated: !filterCollapsed }">
          <ArrowDown />
        </el-icon>
      </div>

      <div v-show="!filterCollapsed" class="filter-body">
        <el-form :model="filterForm" label-width="90px" class="filter-form">
          <el-form-item label="借阅状态">
            <el-select
              v-model="filterForm.statuses"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择借阅状态"
              clearable
              style="width: 320px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              >
                <el-tag :type="item.type" size="small">{{ item.label }}</el-tag>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="借阅日期">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 320px"
              @change="handleFilterChange"
            />
          </el-form-item>

          <el-form-item label="图书分类">
            <el-select
              v-model="filterForm.categoryId"
              placeholder="请选择图书分类"
              clearable
              style="width: 200px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="借阅人">
            <el-input
              v-model="filterForm.borrowerKeyword"
              placeholder="输入昵称或用户名"
              clearable
              style="width: 200px"
              @input="debouncedFilterChange"
              @clear="handleFilterChange"
            />
          </el-form-item>

          <el-form-item label="所有者">
            <el-input
              v-model="filterForm.ownerKeyword"
              placeholder="输入昵称或用户名"
              clearable
              style="width: 200px"
              @input="debouncedFilterChange"
              @clear="handleFilterChange"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleFilterChange">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="resetFilter">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <div v-if="activeFilterTags.length > 0" class="active-tags">
      <span class="tags-label">已选条件：</span>
      <el-tag
        v-for="tag in activeFilterTags"
        :key="tag.key"
        closable
        type="info"
        size="small"
        class="filter-tag"
        @close="removeFilterTag(tag.key)"
      >
        {{ tag.label }}
      </el-tag>
      <el-button link type="primary" size="small" @click="resetFilter">
        一键清除
      </el-button>
    </div>

    <el-table :data="historyList" v-loading="loading" style="width: 100%; margin-top: 16px">
      <el-table-column prop="book.title" label="图书名称" min-width="150" />
      <el-table-column label="图书分类" width="120">
        <template #default="{ row }">
          {{ row.book?.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="borrower.nickname" label="借阅人" width="120" />
      <el-table-column prop="owner.nickname" label="所有者" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="borrowTime" label="借出时间" width="180" />
      <el-table-column prop="returnTime" label="归还时间" width="180">
        <template #default="{ row }">
          {{ row.returnTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="逾期天数" width="100">
        <template #default="{ row }">
          <template v-if="row.status === 'OVERDUE'">
            <el-tag type="danger" size="small">{{ row.overdueDays || 0 }} 天</el-tag>
          </template>
          <template v-else-if="row.status === 'RETURNED' && row.overdueDays > 0">
            <el-tag type="warning" size="small">{{ row.overdueDays }} 天</el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="逾期罚金" width="100">
        <template #default="{ row }">
          <template v-if="row.overdueFine != null && row.overdueFine > 0">
            <span class="fine-amount">¥{{ row.overdueFine }}</span>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="评价" width="120" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'RETURNED' && row.borrower?.id === currentUserId">
            <el-button
              v-if="!row.reviewed"
              type="primary"
              size="small"
              link
              @click="openReviewDialog(row)"
            >
              去评价
            </el-button>
            <el-tag v-else type="success" size="small">已评价</el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="[10, 20, 50, 100]"
      style="margin-top: 20px; justify-content: center"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <el-dialog
      v-model="reviewDialogVisible"
      title="评价图书"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="reviewForm" label-width="80px" ref="reviewFormRef">
        <el-form-item label="图书">
          <span>{{ currentReviewRecord?.book?.title }}</span>
        </el-form-item>
        <el-form-item label="评分" required prop="rating">
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
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>

    <ExportDialog
      v-model="exportDialogVisible"
      type="borrowRecord"
      :export-params="exportParams"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Filter, ArrowDown, Search, Refresh, Download } from '@element-plus/icons-vue'
import { borrowRecordAPI, categoryAPI, reviewAPI } from '@/api'
import { borrowRecordStore } from '@/store/borrowRecord'
import ExportDialog from '@/components/ExportDialog.vue'

const currentUserId = 1
const historyList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterCollapsed = ref(false)
const categories = ref([])

const reviewDialogVisible = ref(false)
const currentReviewRecord = ref(null)
const submittingReview = ref(false)
const reviewFormRef = ref(null)
const reviewForm = ref({
  rating: 5,
  content: ''
})

const exportDialogVisible = ref(false)

const exportParams = computed(() => {
  const params = {
    currentUserId
  }
  if (filterForm.value.statuses && filterForm.value.statuses.length > 0) {
    params.statuses = filterForm.value.statuses
  }
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    params.startDateFrom = filterForm.value.dateRange[0]
    params.startDateTo = filterForm.value.dateRange[1]
  }
  if (filterForm.value.categoryId) {
    params.categoryId = filterForm.value.categoryId
  }
  if (filterForm.value.borrowerKeyword && filterForm.value.borrowerKeyword.trim()) {
    params.borrowerKeyword = filterForm.value.borrowerKeyword.trim()
  }
  if (filterForm.value.ownerKeyword && filterForm.value.ownerKeyword.trim()) {
    params.ownerKeyword = filterForm.value.ownerKeyword.trim()
  }
  return params
})

const statusMap = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已同意', type: 'primary' },
  REJECTED: { text: '已拒绝', type: 'danger' },
  BORROWING: { text: '借阅中', type: 'info' },
  OVERDUE: { text: '已逾期', type: 'danger' },
  RETURNED: { text: '已归还', type: 'success' }
}

const statusOptions = Object.entries(statusMap).map(([value, info]) => ({
  value,
  label: info.text,
  type: info.type
}))

const defaultFilter = () => ({
  statuses: [],
  dateRange: [],
  categoryId: null,
  borrowerKeyword: '',
  ownerKeyword: ''
})

const filterForm = ref(defaultFilter())

let debounceTimer = null
const debouncedFilterChange = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    handleFilterChange()
  }, 300)
}

const activeFilterTags = computed(() => {
  const tags = []
  if (filterForm.value.statuses && filterForm.value.statuses.length > 0) {
    const labels = filterForm.value.statuses.map(s => statusMap[s]?.text || s).join('、')
    tags.push({ key: 'statuses', label: `状态：${labels}` })
  }
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    tags.push({
      key: 'dateRange',
      label: `日期：${filterForm.value.dateRange[0]} 至 ${filterForm.value.dateRange[1]}`
    })
  }
  if (filterForm.value.categoryId) {
    const cat = categories.value.find(c => c.id === filterForm.value.categoryId)
    tags.push({ key: 'categoryId', label: `分类：${cat?.name || filterForm.value.categoryId}` })
  }
  if (filterForm.value.borrowerKeyword) {
    tags.push({ key: 'borrowerKeyword', label: `借阅人：${filterForm.value.borrowerKeyword}` })
  }
  if (filterForm.value.ownerKeyword) {
    tags.push({ key: 'ownerKeyword', label: `所有者：${filterForm.value.ownerKeyword}` })
  }
  return tags
})

const activeFilterCount = computed(() => activeFilterTags.value.length)

const getStatusText = (status) => statusMap[status]?.text || status
const getStatusType = (status) => statusMap[status]?.type || 'info'

const buildQueryParams = () => {
  const params = {
    currentUserId,
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }
  if (filterForm.value.statuses && filterForm.value.statuses.length > 0) {
    params.statuses = filterForm.value.statuses
  }
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    params.startDateFrom = filterForm.value.dateRange[0]
    params.startDateTo = filterForm.value.dateRange[1]
  }
  if (filterForm.value.categoryId) {
    params.categoryId = filterForm.value.categoryId
  }
  if (filterForm.value.borrowerKeyword && filterForm.value.borrowerKeyword.trim()) {
    params.borrowerKeyword = filterForm.value.borrowerKeyword.trim()
  }
  if (filterForm.value.ownerKeyword && filterForm.value.ownerKeyword.trim()) {
    params.ownerKeyword = filterForm.value.ownerKeyword.trim()
  }
  return params
}

const loadHistory = async (resetPage = false) => {
  if (resetPage) {
    currentPage.value = 1
  }
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await borrowRecordAPI.query(params)
    if (res.code === 200) {
      const newTotal = res.data.total
      const maxPage = Math.max(1, Math.ceil(newTotal / pageSize.value))
      if (currentPage.value > maxPage) {
        currentPage.value = maxPage
        const adjustedParams = buildQueryParams()
        const adjustedRes = await borrowRecordAPI.query(adjustedParams)
        if (adjustedRes.code === 200) {
          historyList.value = adjustedRes.data.list
          total.value = adjustedRes.data.total
          return
        }
      }
      historyList.value = res.data.list
      total.value = newTotal
    } else {
      ElMessage.error(res.message || '加载历史记录失败')
    }
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadHistory()
}

const resetFilter = () => {
  filterForm.value = defaultFilter()
  currentPage.value = 1
  loadHistory()
}

const removeFilterTag = (key) => {
  switch (key) {
    case 'statuses':
      filterForm.value.statuses = []
      break
    case 'dateRange':
      filterForm.value.dateRange = []
      break
    case 'categoryId':
      filterForm.value.categoryId = null
      break
    case 'borrowerKeyword':
      filterForm.value.borrowerKeyword = ''
      break
    case 'ownerKeyword':
      filterForm.value.ownerKeyword = ''
      break
  }
  handleFilterChange()
}

const handlePageChange = () => {
  loadHistory()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadHistory()
}

const loadCategories = async () => {
  try {
    const res = await categoryAPI.getAll()
    if (res.code === 200) {
      categories.value = res.data
    }
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const openReviewDialog = (record) => {
  currentReviewRecord.value = record
  reviewForm.value = {
    rating: 5,
    content: ''
  }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.value.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingReview.value = true
  try {
    const res = await reviewAPI.create({
      bookId: currentReviewRecord.value.book.id,
      userId: currentUserId,
      borrowRecordId: currentReviewRecord.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content
    })
    if (res.code === 200) {
      ElMessage.success('评价提交成功')
      reviewDialogVisible.value = false
      loadHistory()
    } else {
      ElMessage.error(res.message || '评价提交失败')
    }
  } catch (e) {
    ElMessage.error('评价提交失败')
  } finally {
    submittingReview.value = false
  }
}

const handleExport = () => {
  exportDialogVisible.value = true
}

onMounted(() => {
  loadCategories()
  loadHistory()
})

watch(() => borrowRecordStore.updateVersion, () => {
  loadHistory(true)
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.filter-card {
  margin-bottom: 0;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.filter-count {
  margin-left: 8px;
}

.collapse-icon {
  transition: transform 0.3s ease;
  color: #909399;
  font-size: 16px;
}

.collapse-icon.rotated {
  transform: rotate(180deg);
}

.filter-body {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 24px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
  margin-right: 0;
}

.active-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.tags-label {
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}

.filter-tag {
  margin: 0;
}

.fine-amount {
  color: #e6a23c;
  font-weight: 600;
}
</style>
