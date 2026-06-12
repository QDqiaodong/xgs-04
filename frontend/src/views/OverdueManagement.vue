<template>
  <div>
    <h2 class="page-title">⚠️ 逾期管理</h2>

    <el-card class="summary-card" shadow="never">
      <div class="summary-content">
        <div class="summary-item">
          <span class="summary-label">逾期记录数</span>
          <span class="summary-value">{{ total }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">每日罚金费率</span>
          <span class="summary-value">{{ dailyFineRate }} 元/天</span>
        </div>
        <div class="summary-actions">
          <el-button type="primary" :loading="identifyLoading" @click="handleIdentify">
            <el-icon><Refresh /></el-icon>
            识别逾期记录
          </el-button>
          <el-button @click="showRuleDialog = true">
            <el-icon><Setting /></el-icon>
            罚金费率设置
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card" shadow="never" style="margin-top: 16px">
      <div class="filter-header" @click="filterCollapsed = !filterCollapsed">
        <div class="filter-title">
          <el-icon><Filter /></el-icon>
          <span>筛选条件</span>
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

          <el-form-item label="图书名称">
            <el-input
              v-model="filterForm.bookTitleKeyword"
              placeholder="输入图书名称"
              clearable
              style="width: 200px"
              @input="debouncedFilterChange"
              @clear="handleFilterChange"
            />
          </el-form-item>

          <el-form-item label="排序方式">
            <el-select v-model="filterForm.sortBy" style="width: 160px" @change="handleFilterChange">
              <el-option label="逾期天数降序" value="overdueDays_desc" />
              <el-option label="逾期天数升序" value="overdueDays_asc" />
            </el-select>
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

    <el-table :data="overdueList" v-loading="loading" style="width: 100%; margin-top: 16px" stripe>
      <el-table-column prop="book.title" label="图书名称" min-width="150" />
      <el-table-column label="图书分类" width="120">
        <template #default="{ row }">
          {{ row.book?.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="borrower.nickname" label="借阅人" width="120" />
      <el-table-column prop="owner.nickname" label="所有者" width="120" />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="应还日期" width="120">
        <template #default="{ row }">
          <span class="overdue-date">{{ row.endDate }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="borrowTime" label="借出时间" width="180" />
      <el-table-column label="逾期天数" width="110" sortable>
        <template #default="{ row }">
          <el-tag type="danger" size="small">{{ row.overdueDays }} 天</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预估罚金" width="120">
        <template #default="{ row }">
          <span class="fine-amount">¥{{ calculateFine(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="danger" size="small">逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="handleReturn(row)">
            确认归还
          </el-button>
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
      v-model="returnDialogVisible"
      title="确认归还 - 逾期结算"
      width="550px"
      :close-on-click-modal="false"
    >
      <div v-if="currentReturnRecord" class="return-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="图书名称">{{ currentReturnRecord.book?.title }}</el-descriptions-item>
          <el-descriptions-item label="借阅人">{{ currentReturnRecord.borrower?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="应还日期">{{ currentReturnRecord.endDate }}</el-descriptions-item>
          <el-descriptions-item label="逾期天数">
            <el-tag type="danger">{{ currentReturnRecord.overdueDays }} 天</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="每日罚金费率">{{ dailyFineRate }} 元/天</el-descriptions-item>
          <el-descriptions-item label="应缴罚金">
            <span class="fine-amount-large">¥{{ calculateFine(currentReturnRecord) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returnLoading" @click="confirmReturnAction">
          确认归还并结算
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showRuleDialog"
      title="罚金费率设置"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="ruleForm" label-width="120px">
        <el-form-item label="每日罚金费率">
          <el-input-number
            v-model="ruleForm.dailyFineRate"
            :min="0"
            :max="100"
            :precision="2"
            :step="0.1"
          />
          <span style="margin-left: 8px; color: #909399">元/天</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRuleDialog = false">取消</el-button>
        <el-button type="primary" :loading="ruleLoading" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Filter, ArrowDown, Search, Refresh, Setting } from '@element-plus/icons-vue'
import { borrowRecordAPI, borrowRuleAPI } from '@/api'

const overdueList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterCollapsed = ref(false)
const identifyLoading = ref(false)
const dailyFineRate = ref(0.5)

const returnDialogVisible = ref(false)
const currentReturnRecord = ref(null)
const returnLoading = ref(false)

const showRuleDialog = ref(false)
const ruleLoading = ref(false)
const ruleForm = ref({
  dailyFineRate: 0.5
})

const defaultFilter = () => ({
  borrowerKeyword: '',
  ownerKeyword: '',
  bookTitleKeyword: '',
  sortBy: 'overdueDays_desc'
})

const filterForm = ref(defaultFilter())

let debounceTimer = null
const debouncedFilterChange = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    handleFilterChange()
  }, 300)
}

const activeFilterCount = computed(() => {
  let count = 0
  if (filterForm.value.borrowerKeyword) count++
  if (filterForm.value.ownerKeyword) count++
  if (filterForm.value.bookTitleKeyword) count++
  if (filterForm.value.sortBy !== 'overdueDays_desc') count++
  return count
})

const calculateFine = (row) => {
  if (!row || !row.overdueDays) return '0.00'
  return (row.overdueDays * dailyFineRate.value).toFixed(2)
}

const buildQueryParams = () => {
  const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value
  }
  if (filterForm.value.borrowerKeyword && filterForm.value.borrowerKeyword.trim()) {
    params.borrowerKeyword = filterForm.value.borrowerKeyword.trim()
  }
  if (filterForm.value.ownerKeyword && filterForm.value.ownerKeyword.trim()) {
    params.ownerKeyword = filterForm.value.ownerKeyword.trim()
  }
  if (filterForm.value.bookTitleKeyword && filterForm.value.bookTitleKeyword.trim()) {
    params.bookTitleKeyword = filterForm.value.bookTitleKeyword.trim()
  }
  if (filterForm.value.sortBy) {
    const parts = filterForm.value.sortBy.split('_')
    params.sortBy = parts[0]
    params.sortOrder = parts[1] || 'desc'
  }
  return params
}

const loadOverdueList = async (resetPage = false) => {
  if (resetPage) {
    currentPage.value = 1
  }
  loading.value = true
  try {
    const params = buildQueryParams()
    const res = await borrowRecordAPI.queryOverdue(params)
    if (res.code === 200) {
      const newTotal = res.data.total
      const maxPage = Math.max(1, Math.ceil(newTotal / pageSize.value))
      if (currentPage.value > maxPage) {
        currentPage.value = maxPage
        const adjustedParams = buildQueryParams()
        const adjustedRes = await borrowRecordAPI.queryOverdue(adjustedParams)
        if (adjustedRes.code === 200) {
          overdueList.value = adjustedRes.data.list
          total.value = adjustedRes.data.total
          return
        }
      }
      overdueList.value = res.data.list
      total.value = newTotal
    } else {
      ElMessage.error(res.message || '加载逾期记录失败')
    }
  } catch (e) {
    ElMessage.error('加载逾期记录失败')
  } finally {
    loading.value = false
  }
}

const loadBorrowRule = async () => {
  try {
    const res = await borrowRuleAPI.get()
    if (res.code === 200 && res.data) {
      dailyFineRate.value = res.data.dailyFineRate || 0.5
      ruleForm.value.dailyFineRate = res.data.dailyFineRate || 0.5
    }
  } catch (e) {
    console.error('加载借阅规则失败', e)
  }
}

const handleIdentify = async () => {
  identifyLoading.value = true
  try {
    const res = await borrowRecordAPI.identifyOverdue()
    if (res.code === 200) {
      const count = res.data || 0
      if (count > 0) {
        ElMessage.success(`识别到 ${count} 条新的逾期记录`)
      } else {
        ElMessage.info('暂无新的逾期记录')
      }
      loadOverdueList()
    } else {
      ElMessage.error(res.message || '识别逾期记录失败')
    }
  } catch (e) {
    ElMessage.error('识别逾期记录失败')
  } finally {
    identifyLoading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadOverdueList()
}

const resetFilter = () => {
  filterForm.value = defaultFilter()
  currentPage.value = 1
  loadOverdueList()
}

const handlePageChange = () => {
  loadOverdueList()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadOverdueList()
}

const handleReturn = (record) => {
  currentReturnRecord.value = record
  returnDialogVisible.value = true
}

const confirmReturnAction = async () => {
  if (!currentReturnRecord.value) return
  returnLoading.value = true
  try {
    const res = await borrowRecordAPI.confirmReturn(currentReturnRecord.value.id)
    if (res.code === 200) {
      ElMessage.success('归还成功，逾期罚金已结算')
      returnDialogVisible.value = false
      loadOverdueList(true)
    } else {
      ElMessage.error(res.message || '归还失败')
    }
  } catch (e) {
    ElMessage.error('归还操作失败')
  } finally {
    returnLoading.value = false
  }
}

const saveRule = async () => {
  ruleLoading.value = true
  try {
    const res = await borrowRuleAPI.update({
      dailyFineRate: ruleForm.value.dailyFineRate
    })
    if (res.code === 200) {
      dailyFineRate.value = ruleForm.value.dailyFineRate
      ElMessage.success('罚金费率更新成功')
      showRuleDialog.value = false
      loadOverdueList()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新罚金费率失败')
  } finally {
    ruleLoading.value = false
  }
}

onMounted(() => {
  loadBorrowRule()
  loadOverdueList()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 16px;
  font-size: 24px;
  color: #303133;
}

.summary-card {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.summary-content {
  display: flex;
  align-items: center;
  gap: 40px;
  flex-wrap: wrap;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.summary-actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
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

.overdue-date {
  color: #f56c6c;
  font-weight: 500;
}

.fine-amount {
  color: #e6a23c;
  font-weight: 600;
}

.fine-amount-large {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 700;
}

.return-info {
  margin-bottom: 16px;
}

.return-info :deep(.el-descriptions) {
  margin-top: 8px;
}
</style>
