<template>
  <el-dialog
    v-model="visible"
    title="申请借阅"
    width="500px"
    @close="handleClose"
  >
    <el-descriptions :column="1" border>
      <el-descriptions-item label="书名">{{ book?.title }}</el-descriptions-item>
      <el-descriptions-item label="作者">{{ book?.author }}</el-descriptions-item>
      <el-descriptions-item label="所有者">{{ book?.owner?.nickname }}</el-descriptions-item>
    </el-descriptions>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="margin-top: 20px"
    >
      <el-form-item label="借用开始日期" prop="startDate">
        <el-date-picker
          v-model="form.startDate"
          type="date"
          placeholder="选择开始日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="借用结束日期" prop="endDate">
        <el-date-picker
          v-model="form.endDate"
          type="date"
          placeholder="选择结束日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注信息（选填）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose" :disabled="submitting">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="submitting">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  book: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const form = ref({
  startDate: null,
  endDate: null,
  remark: ''
})

const rules = {
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    form.value = {
      startDate: null,
      endDate: null,
      remark: ''
    }
    formRef.value?.resetFields()
  }
})

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    const result = emit('submit', {
      bookId: props.book.id,
      ...form.value
    })
    if (result instanceof Promise) {
      await result
    }
    handleClose()
  } finally {
    submitting.value = false
  }
}
</script>
