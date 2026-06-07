import { ref, watch, onUnmounted } from 'vue'
import { saveDraft } from '@/utils/draftStorage'

const deepClone = (obj) => JSON.parse(JSON.stringify(obj))
const isEqual = (a, b) => JSON.stringify(a) === JSON.stringify(b)

export const useAutoSaveDraft = (formRef, ownerId, bookIdRef, options = {}) => {
  const {
    interval = 30000,
    onSaveSuccess = null,
    onSaveError = null
  } = options

  const lastSavedAt = ref(null)
  const baseline = ref(null)
  let timerId = null

  const isDirty = () => {
    if (!baseline.value || !formRef.value) return false
    return !isEqual(baseline.value, formRef.value)
  }

  const resetBaseline = () => {
    baseline.value = formRef.value ? deepClone(formRef.value) : null
  }

  const performSave = () => {
    if (!isDirty() || !formRef.value) return
    const bookId = bookIdRef.value
    const success = saveDraft(ownerId.value, bookId, formRef.value)
    if (success) {
      lastSavedAt.value = Date.now()
      resetBaseline()
      onSaveSuccess && onSaveSuccess()
    } else {
      onSaveError && onSaveError()
    }
  }

  const startTimer = () => {
    stopTimer()
    timerId = setInterval(performSave, interval)
  }

  const stopTimer = () => {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
  }

  const forceSave = () => {
    performSave()
  }

  watch(formRef, () => {
    if (!baseline.value) {
      resetBaseline()
    }
  }, { deep: true })

  onUnmounted(() => {
    stopTimer()
    performSave()
  })

  return {
    lastSavedAt,
    isDirty,
    startTimer,
    stopTimer,
    resetBaseline,
    forceSave
  }
}
