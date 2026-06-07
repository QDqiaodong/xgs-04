import { ref, watch, onUnmounted } from 'vue'
import { saveDraft } from '@/utils/draftStorage'

export const useAutoSaveDraft = (formRef, ownerId, bookIdRef, options = {}) => {
  const {
    interval = 30000,
    onSaveSuccess = null,
    onSaveError = null
  } = options

  const lastSavedAt = ref(null)
  const isDirty = ref(false)
  let timerId = null

  const performSave = () => {
    if (!isDirty.value || !formRef.value) return
    const bookId = bookIdRef.value
    const success = saveDraft(ownerId.value, bookId, formRef.value)
    if (success) {
      lastSavedAt.value = Date.now()
      isDirty.value = false
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

  const markDirty = () => {
    isDirty.value = true
  }

  const forceSave = () => {
    performSave()
  }

  watch(formRef, () => {
    markDirty()
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
    markDirty,
    forceSave
  }
}
