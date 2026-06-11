<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="exportStatus === 'idle'" class="export-form">
      <div class="export-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>将按照当前筛选条件导出数据，请选择需要导出的字段：</span>
      </div>

      <div class="field-list">
        <div class="field-actions">
          <el-button size="small" type="primary" link @click="selectAll">全选</el-button>
          <el-button size="small" link @click="selectNone">清空</el-button>
        </div>
        <el-checkbox-group v-model="selectedFields">
          <el-row :gutter="16">
            <el-col :span="12" v-for="field in fields" :key="field.key">
              <el-checkbox :label="field.key" class="field-checkbox">
                {{ field.label }}
              </el-checkbox>
            </el-col>
          </el-row>
        </el-checkbox-group>
      </div>
    </div>

    <div v-else-if="exportStatus === 'processing'" class="export-processing">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p class="processing-text">正在生成导出文件，请稍候...</p>
      <el-progress
        :percentage="progressPercent"
        :status="exportStatus === 'failed' ? 'exception' : ''"
      />
      <p class="progress-text">
        {{ processedCount }} / {{ totalCount }} 条
      </p>
    </div>

    <div v-else-if="exportStatus === 'completed'" class="export-completed">
      <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
      <p class="success-text">导出成功！</p>
      <p class="export-count">共导出 {{ totalCount }} 条数据</p>
      <el-button type="primary" @click="handleDownload">
        <el-icon><Download /></el-icon>
        下载文件
      </el-button>
    </div>

    <div v-else-if="exportStatus === 'failed'" class="export-failed">
      <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
      <p class="error-text">导出失败</p>
      <p class="error-message">{{ errorMessage }}</p>
    </div>

    <template #footer>
      <el-button v-if="exportStatus === 'idle'" @click="visible = false">取消</el-button>
      <el-button
        v-if="exportStatus === 'idle'"
        type="primary"
        :disabled="selectedFields.length === 0"
        @click="handleStartExport"
      >
        开始导出
      </el-button>
      <el-button v-if="exportStatus === 'completed' || exportStatus === 'failed'" @click="handleClose">
        关闭
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  InfoFilled,
  Loading,
  CircleCheckFilled,
  CircleCloseFilled,
  Download
} from '@element-plus/icons-vue'
import { exportAPI } from '@/api'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    required: true
  },
  exportParams: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() => {
  return props.type === 'book' ? '导出图书列表' : '导出借阅记录'
})

const fields = ref([])
const selectedFields = ref([])
const exportStatus = ref('idle')
const taskId = ref('')
const totalCount = ref(0)
const processedCount = ref(0)
const errorMessage = ref('')
let pollTimer = null

const progressPercent = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.min(Math.round((processedCount.value / totalCount.value) * 100), 100)
})

const loadFields = async () => {
  try {
    const res = props.type === 'book'
      ? await exportAPI.getBookFields()
      : await exportAPI.getBorrowRecordFields()
    if (res.code === 200) {
      fields.value = res.data
      selectedFields.value = res.data.map(f => f.key)
    }
  } catch (e) {
    ElMessage.error('加载导出字段失败')
  }
}

const selectAll = () => {
  selectedFields.value = fields.value.map(f => f.key)
}

const selectNone = () => {
  selectedFields.value = []
}

const handleStartExport = async () => {
  if (selectedFields.value.length === 0) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }

  exportStatus.value = 'processing'
  totalCount.value = 0
  processedCount.value = 0

  try {
    const params = {
      ...props.exportParams,
      fields: selectedFields.value
    }

    const res = props.type === 'book'
      ? await exportAPI.createBookExport(params)
      : await exportAPI.createBorrowRecordExport(params)

    if (res.code === 200) {
      taskId.value = res.data.taskId
      totalCount.value = res.data.totalCount || 0
      startPolling()
    } else {
      exportStatus.value = 'failed'
      errorMessage.value = res.message || '创建导出任务失败'
    }
  } catch (e) {
    exportStatus.value = 'failed'
    errorMessage.value = '创建导出任务失败'
  }
}

const startPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }

  pollTimer = setInterval(async () => {
    try {
      const res = await exportAPI.getTaskStatus(taskId.value)
      if (res.code === 200) {
        const task = res.data
        totalCount.value = task.totalCount || 0
        processedCount.value = task.processedCount || 0

        if (task.status === 'COMPLETED') {
          exportStatus.value = 'completed'
          stopPolling()
        } else if (task.status === 'FAILED') {
          exportStatus.value = 'failed'
          errorMessage.value = task.errorMessage || '导出失败'
          stopPolling()
        }
      }
    } catch (e) {
      console.error('查询导出状态失败', e)
    }
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleDownload = () => {
  const url = exportAPI.getDownloadUrl(taskId.value)
  window.open(url, '_blank')
}

const handleClose = () => {
  stopPolling()
  exportStatus.value = 'idle'
  taskId.value = ''
  totalCount.value = 0
  processedCount.value = 0
  errorMessage.value = ''
  visible.value = false
}

watch(() => props.modelValue, (val) => {
  if (val) {
    loadFields()
  } else {
    stopPolling()
  }
})
</script>

<style scoped>
.export-form {
  padding: 10px 0;
}

.export-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  color: #409eff;
  font-size: 14px;
  margin-bottom: 16px;
}

.export-tip .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.field-list {
  max-height: 300px;
  overflow-y: auto;
}

.field-actions {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.field-checkbox {
  margin-bottom: 12px;
}

.export-processing,
.export-completed,
.export-failed {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 0;
  text-align: center;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.success-icon {
  font-size: 48px;
  color: #67c23a;
  margin-bottom: 16px;
}

.error-icon {
  font-size: 48px;
  color: #f56c6c;
  margin-bottom: 16px;
}

.processing-text,
.success-text,
.error-text {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 12px 0;
}

.processing-text {
  color: #303133;
}

.success-text {
  color: #67c23a;
}

.error-text {
  color: #f56c6c;
}

.progress-text {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

.export-count {
  font-size: 14px;
  color: #606266;
  margin: 0 0 20px 0;
}

.error-message {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

:deep(.el-progress) {
  width: 300px;
  margin-top: 10px;
}
</style>
