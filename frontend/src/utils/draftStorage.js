const DRAFT_PREFIX = 'book_form_draft'

const getDraftKey = (ownerId, bookId = null) => {
  const suffix = bookId ? `_${bookId}` : '_new'
  return `${DRAFT_PREFIX}_${ownerId}${suffix}`
}

export const saveDraft = (ownerId, bookId, formData) => {
  try {
    const key = getDraftKey(ownerId, bookId)
    const draft = {
      data: { ...formData },
      savedAt: Date.now()
    }
    localStorage.setItem(key, JSON.stringify(draft))
    return true
  } catch (e) {
    console.error('Failed to save draft:', e)
    return false
  }
}

export const getDraft = (ownerId, bookId = null) => {
  try {
    const key = getDraftKey(ownerId, bookId)
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const draft = JSON.parse(raw)
    return draft
  } catch (e) {
    console.error('Failed to get draft:', e)
    return null
  }
}

export const removeDraft = (ownerId, bookId = null) => {
  try {
    const key = getDraftKey(ownerId, bookId)
    localStorage.removeItem(key)
    return true
  } catch (e) {
    console.error('Failed to remove draft:', e)
    return false
  }
}

export const hasDraft = (ownerId, bookId = null) => {
  return getDraft(ownerId, bookId) !== null
}
