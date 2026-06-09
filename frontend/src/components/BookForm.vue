<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑图书' : '添加图书'"
    width="500px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item label="书名" prop="title">
        <el-input v-model="form.title" placeholder="请输入书名" />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input v-model="form.author" placeholder="请输入作者" />
      </el-form-item>
      <el-form-item label="ISBN">
        <el-input v-model="form.isbn" placeholder="请输入ISBN（选填）" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="成色">
        <el-select v-model="form.conditionLevel" placeholder="请选择成色" style="width: 100%">
          <el-option label="全新" value="全新" />
          <el-option label="九成新" value="九成新" />
          <el-option label="八成新" value="八成新" />
          <el-option label="七成新" value="七成新" />
          <el-option label="六成新及以下" value="六成新及以下" />
        </el-select>
      </el-form-item>
      <el-form-item label="城市" prop="cityId">
        <el-select v-model="form.cityId" placeholder="请选择城市" style="width: 100%">
          <el-option
            v-for="city in cities"
            :key="city.id"
            :label="city.cityName"
            :value="city.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" placeholder="请选择标签" multiple style="width: 100%">
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="可出借">
        <el-switch v-model="form.canBorrow" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入图书简介（选填）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cityAPI, categoryAPI, tagAPI } from '@/api'
import { getDraft, removeDraft, hasDraft } from '@/utils/draftStorage'
import { useAutoSaveDraft } from '@/composables/useAutoSaveDraft'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  book: {
    type: Object,
    default: null
  },
  ownerId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const visible = ref(false)
const formRef = ref(null)
const cities = ref([])
const categories = ref([])
const tags = ref([])

const form = ref({
  title: '',
  author: '',
  isbn: '',
  categoryId: null,
  conditionLevel: '',
  cityId: null,
  canBorrow: true,
  description: '',
  tagIds: []
})

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  cityId: [{ required: true, message: '请选择城市', trigger: 'change' }]
}

const isEdit = ref(false)

const currentBookId = computed(() => (isEdit.value && props.book ? props.book.id : null))
const ownerIdRef = computed(() => props.ownerId)

const onDraftSaveSuccess = () => {
  ElMessage({
    message: '草稿已自动保存',
    type: 'success',
    duration: 1500,
    showClose: false
  })
}

const { startTimer, stopTimer, forceSave, resetBaseline } = useAutoSaveDraft(
  form,
  ownerIdRef,
  currentBookId,
  {
    interval: 30000,
    onSaveSuccess: onDraftSaveSuccess
  }
)

const clearCurrentDraft = () => {
  removeDraft(props.ownerId, currentBookId.value)
}

const checkAndRestoreDraft = async () => {
  const bookId = currentBookId.value
  if (!hasDraft(props.ownerId, bookId)) return

  try {
    await ElMessageBox.confirm(
      '检测到未提交的草稿，是否恢复？',
      '提示',
      {
        confirmButtonText: '恢复',
        cancelButtonText: '丢弃',
        type: 'info'
      }
    )
    const draft = getDraft(props.ownerId, bookId)
    if (draft && draft.data) {
      form.value = { ...form.value, ...draft.data }
    }
  } catch {
    removeDraft(props.ownerId, bookId)
  }
}

const loadCities = async () => {
  const res = await cityAPI.getAll()
  if (res.code === 200) {
    cities.value = res.data
  }
}

const loadCategories = async () => {
  const res = await categoryAPI.getAll()
  if (res.code === 200) {
    categories.value = res.data
  }
}

const loadTags = async () => {
  const res = await tagAPI.getAll()
  if (res.code === 200) {
    tags.value = res.data
  }
}

const resetForm = () => {
  form.value = {
    title: '',
    author: '',
    isbn: '',
    categoryId: null,
    conditionLevel: '',
    cityId: null,
    canBorrow: true,
    description: '',
    tagIds: []
  }
  isEdit.value = false
  formRef.value?.resetFields()
}

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val) {
    if (props.book) {
      isEdit.value = true
      form.value = {
        title: props.book.title,
        author: props.book.author,
        isbn: props.book.isbn || '',
        categoryId: props.book.category?.id,
        conditionLevel: props.book.conditionLevel || '',
        cityId: props.book.city?.id,
        canBorrow: props.book.canBorrow,
        description: props.book.description || '',
        tagIds: props.book.tags?.map(t => t.id) || []
      }
    } else {
      resetForm()
    }
    await nextTick()
    resetBaseline()
    await checkAndRestoreDraft()
    resetBaseline()
    startTimer()
  } else {
    stopTimer()
    forceSave()
  }
})

const handleClose = () => {
  stopTimer()
  forceSave()
  emit('update:modelValue', false)
  resetForm()
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  stopTimer()
  emit('submit', {
    ...form.value,
    ownerId: props.ownerId
  })
}

defineExpose({
  clearDraft: clearCurrentDraft
})

onMounted(() => {
  loadCities()
  loadCategories()
  loadTags()
})
</script>
