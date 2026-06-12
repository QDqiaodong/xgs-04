import { reactive, ref } from 'vue'
import { notificationAPI } from '@/api'

const DEFAULT_USER_ID = 1

export const notificationStore = reactive({
  unreadCount: 0,
  updateVersion: 0,
  loading: false
})

function bumpVersion() {
  notificationStore.updateVersion++
}

const loadingPromise = ref(null)

export async function loadUnreadCount(userId = DEFAULT_USER_ID, force = false) {
  if (notificationStore.loading && !force) {
    return loadingPromise.value
  }
  if (loadingPromise.value && !force) {
    return loadingPromise.value
  }

  notificationStore.loading = true
  loadingPromise.value = (async () => {
    try {
      const res = await notificationAPI.getUnreadCount(userId)
      if (res.code === 200) {
        notificationStore.unreadCount = res.data.count || 0
        bumpVersion()
        return true
      }
      return false
    } catch (e) {
      console.error('加载未读消息数量失败:', e)
      return false
    } finally {
      notificationStore.loading = false
      loadingPromise.value = null
    }
  })()
  return loadingPromise.value
}

export function getUnreadCount() {
  return notificationStore.unreadCount
}

export function decrementUnreadCount() {
  if (notificationStore.unreadCount > 0) {
    notificationStore.unreadCount--
    bumpVersion()
  }
}

export function resetUnreadCount() {
  notificationStore.unreadCount = 0
  bumpVersion()
}

export async function refreshNotifications(userId = DEFAULT_USER_ID) {
  return loadUnreadCount(userId, true)
}
