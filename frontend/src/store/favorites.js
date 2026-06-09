import { ref, reactive } from 'vue'
import { favoriteAPI } from '@/api'
import { ElMessage } from 'element-plus'

const DEFAULT_USER_ID = 1

export const favoriteStore = reactive({
  favoriteBookIds: {},
  perBookStatus: {},
  favoriteCount: 0,
  listLoaded: false,
  listLoadFailed: false,
  loading: false,
  updateVersion: 0
})

function bumpVersion() {
  favoriteStore.updateVersion++
}

const loadingPromise = ref(null)

export async function loadFavorites(userId = DEFAULT_USER_ID, force = false) {
  if (favoriteStore.listLoaded && !force) {
    return true
  }
  if (loadingPromise.value && !force) {
    return loadingPromise.value
  }

  favoriteStore.loading = true
  favoriteStore.listLoadFailed = false

  loadingPromise.value = (async () => {
    try {
      const [booksRes, countRes] = await Promise.all([
        favoriteAPI.getByUser(userId),
        favoriteAPI.getCount(userId)
      ])

      const idObj = {}
      const perBookObj = {}
      if (booksRes.code === 200) {
        booksRes.data.forEach(b => {
          idObj[b.id] = true
          perBookObj[b.id] = true
        })
      }
      favoriteStore.favoriteBookIds = idObj
      favoriteStore.perBookStatus = perBookObj

      if (countRes.code === 200) {
        favoriteStore.favoriteCount = countRes.data.count
      }

      favoriteStore.listLoaded = true
      favoriteStore.listLoadFailed = false
      bumpVersion()
      return true
    } catch (e) {
      console.error('加载收藏列表失败，降级为单本查询模式:', e)
      favoriteStore.listLoaded = false
      favoriteStore.listLoadFailed = true
      try {
        const countRes = await favoriteAPI.getCount(userId)
        if (countRes.code === 200) {
          favoriteStore.favoriteCount = countRes.data.count
        }
      } catch (e2) {
        console.error('加载收藏数量也失败:', e2)
      }
      bumpVersion()
      return false
    } finally {
      favoriteStore.loading = false
      loadingPromise.value = null
    }
  })()
  return loadingPromise.value
}

export async function ensureFavoriteCount(userId = DEFAULT_USER_ID, force = false) {
  if (favoriteStore.listLoaded && !force) {
    return favoriteStore.favoriteCount
  }
  try {
    const res = await favoriteAPI.getCount(userId)
    if (res.code === 200) {
      favoriteStore.favoriteCount = res.data.count
      bumpVersion()
    }
  } catch (e) {
    console.error('获取收藏数量失败:', e)
  }
  return favoriteStore.favoriteCount
}

export async function checkFavorite(bookId, userId = DEFAULT_USER_ID) {
  if (favoriteStore.listLoaded) {
    return !!favoriteStore.favoriteBookIds[bookId]
  }
  if (bookId in favoriteStore.perBookStatus) {
    return favoriteStore.perBookStatus[bookId]
  }
  favoriteStore.perBookStatus[bookId] = false
  try {
    const res = await favoriteAPI.check(userId, bookId)
    if (res.code === 200) {
      const favorited = !!res.data.favorited
      favoriteStore.perBookStatus[bookId] = favorited
      if (favorited) {
        favoriteStore.favoriteBookIds[bookId] = true
      }
      bumpVersion()
      return favorited
    }
  } catch (e) {
    console.error('检查收藏状态失败', bookId, e)
  }
  return false
}

export async function toggleFavorite(bookId, userId = DEFAULT_USER_ID) {
  try {
    const res = await favoriteAPI.toggle(userId, bookId)
    if (res.code === 200) {
      const { favorited, count } = res.data
      if (favorited) {
        favoriteStore.favoriteBookIds[bookId] = true
        favoriteStore.perBookStatus[bookId] = true
        ElMessage.success('已加入收藏')
      } else {
        delete favoriteStore.favoriteBookIds[bookId]
        favoriteStore.perBookStatus[bookId] = false
        ElMessage.success('已取消收藏')
      }
      favoriteStore.favoriteCount = count
      bumpVersion()
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
  if (bookId in favoriteStore.perBookStatus) {
    return favoriteStore.perBookStatus[bookId]
  }
  return !!favoriteStore.favoriteBookIds[bookId]
}

export function getFavoriteCount() {
  return favoriteStore.favoriteCount
}

export async function refreshFavorites(userId = DEFAULT_USER_ID) {
  return loadFavorites(userId, true)
}
