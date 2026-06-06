import { ref, reactive } from 'vue'
import { favoriteAPI } from '@/api'
import { ElMessage } from 'element-plus'

const DEFAULT_USER_ID = 1

export const favoriteStore = reactive({
  favoriteBookIds: new Set(),
  favoriteCount: 0,
  loading: false,
  initialized: false
})

const loadingPromise = ref(null)

export async function loadFavorites(userId = DEFAULT_USER_ID, force = false) {
  if (favoriteStore.initialized && !force) {
    return
  }
  if (loadingPromise.value && !force) {
    return loadingPromise.value
  }

  favoriteStore.loading = true
  loadingPromise.value = (async () => {
    try {
      const [booksRes, countRes] = await Promise.all([
        favoriteAPI.getByUser(userId),
        favoriteAPI.getCount(userId)
      ])
      if (booksRes.code === 200) {
        favoriteStore.favoriteBookIds = new Set(booksRes.data.map(b => b.id))
      }
      if (countRes.code === 200) {
        favoriteStore.favoriteCount = countRes.data.count
      }
      favoriteStore.initialized = true
    } catch (e) {
      console.error('加载收藏失败:', e)
    } finally {
      favoriteStore.loading = false
      loadingPromise.value = null
    }
  })()
  return loadingPromise.value
}

export async function toggleFavorite(bookId, userId = DEFAULT_USER_ID) {
  try {
    const res = await favoriteAPI.toggle(userId, bookId)
    if (res.code === 200) {
      const { favorited, count } = res.data
      if (favorited) {
        favoriteStore.favoriteBookIds.add(bookId)
        ElMessage.success('已加入收藏')
      } else {
        favoriteStore.favoriteBookIds.delete(bookId)
        ElMessage.success('已取消收藏')
      }
      favoriteStore.favoriteCount = count
      return favorited
    } else {
      ElMessage.error(res.message || '操作失败')
      return null
    }
  } catch (e) {
    ElMessage.error('操作失败')
    return null
  }
}

export function isFavorited(bookId) {
  return favoriteStore.favoriteBookIds.has(bookId)
}

export function getFavoriteCount() {
  return favoriteStore.favoriteCount
}

export async function refreshFavorites(userId = DEFAULT_USER_ID) {
  return loadFavorites(userId, true)
}
