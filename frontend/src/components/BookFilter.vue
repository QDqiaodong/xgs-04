<template>
  <el-card class="filter-card">
    <el-form :model="filterForm" inline class="filter-form">
      <el-form-item label="所在城市">
        <el-select
          v-model="filterForm.cityId"
          placeholder="选择城市"
          clearable
          style="width: 160px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="city in cities"
            :key="city.id"
            :label="city.cityName"
            :value="city.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="图书分类">
        <el-select
          v-model="filterForm.categoryId"
          placeholder="选择分类"
          clearable
          style="width: 160px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select
          v-model="filterForm.tagIds"
          placeholder="选择标签"
          multiple
          clearable
          style="width: 240px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="匹配方式">
        <el-switch
          v-model="filterForm.matchAllTags"
          active-text="全部匹配"
          inactive-text="任一匹配"
          @change="handleFilterChange"
        />
      </el-form-item>
      <el-form-item label="可出借">
        <el-switch
          v-model="filterForm.available"
          @change="handleFilterChange"
        />
      </el-form-item>
      <el-form-item label="搜索">
        <el-input
          v-model="filterForm.keyword"
          placeholder="书名/作者"
          clearable
          style="width: 200px"
          @keyup.enter="handleFilterChange"
        />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { cityAPI, categoryAPI, tagAPI } from '@/api'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  cacheKey: {
    type: String,
    default: 'book_filter'
  }
})

const emit = defineEmits(['update:modelValue', 'filter-change'])

const cities = ref([])
const categories = ref([])
const tags = ref([])
const filterForm = ref({
  cityId: null,
  categoryId: null,
  tagIds: [],
  matchAllTags: false,
  available: true,
  keyword: ''
})

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

const loadCachedFilter = () => {
  const cached = localStorage.getItem(props.cacheKey)
  if (cached) {
    try {
      const parsed = JSON.parse(cached)
      Object.assign(filterForm.value, parsed)
    } catch (e) {
      console.error('Failed to parse cached filter')
    }
  }
}

const saveFilterToCache = () => {
  localStorage.setItem(props.cacheKey, JSON.stringify(filterForm.value))
}

const handleFilterChange = () => {
  saveFilterToCache()
  emit('update:modelValue', { ...filterForm.value })
  emit('filter-change', { ...filterForm.value })
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    Object.assign(filterForm.value, newVal)
  }
}, { deep: true })

onMounted(() => {
  loadCities()
  loadCategories()
  loadTags()
  loadCachedFilter()
  setTimeout(() => handleFilterChange(), 100)
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
