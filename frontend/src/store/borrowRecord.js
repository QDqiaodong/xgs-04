import { ref, reactive } from 'vue'
import { borrowRecordAPI } from '@/api'

const DEFAULT_USER_ID = 1

export const borrowRecordStore = reactive({
  pendingApprovalCount: 0,
  updateVersion: 0,
  loading: false
})

function bumpVersion() {
  borrowRecordStore.updateVersion++
}

const loadingPromise = ref(null)

export async function loadPendingApprovalCount(userId = DEFAULT_USER_ID, force = false) {
  if (borrowRecordStore.loading && !force) {
    return loadingPromise.value
  }
  if (loadingPromise.value && !force) {
    return loadingPromise.value
  }

  borrowRecordStore.loading = true
  loadingPromise.value = (async () => {
    try {
      const res = await borrowRecordAPI.getByOwner(userId)
      if (res.code === 200) {
        borrowRecordStore.pendingApprovalCount = res.data.filter(r => r.status === 'PENDING').length
        bumpVersion()
        return true
      }
      return false
    } catch (e) {
      console.error('加载待审批数量失败:', e)
      return false
    } finally {
      borrowRecordStore.loading = false
      loadingPromise.value = null
    }
  })()
  return loadingPromise.value
}

export function getPendingApprovalCount() {
  return borrowRecordStore.pendingApprovalCount
}

export async function refreshBorrowRecords(userId = DEFAULT_USER_ID) {
  return loadPendingApprovalCount(userId, true)
}
