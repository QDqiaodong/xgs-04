import { ref, watch } from 'vue'

export const VIEW_MODES = {
  GRID: 'grid',
  LIST: 'list'
}

const STORAGE_KEY = 'book_view_mode'

function getStoredViewMode(pageKey) {
  try {
    const stored = localStorage.getItem(`${STORAGE_KEY}_${pageKey}`)
    if (stored && (stored === VIEW_MODES.GRID || stored === VIEW_MODES.LIST)) {
      return stored
    }
  } catch (e) {
    console.warn('读取视图模式失败:', e)
  }
  return VIEW_MODES.GRID
}

function createViewModeStore(pageKey) {
  const viewMode = ref(getStoredViewMode(pageKey))

  watch(viewMode, (newVal) => {
    try {
      localStorage.setItem(`${STORAGE_KEY}_${pageKey}`, newVal)
    } catch (e) {
      console.warn('保存视图模式失败:', e)
    }
  })

  const setViewMode = (mode) => {
    if (mode === VIEW_MODES.GRID || mode === VIEW_MODES.LIST) {
      viewMode.value = mode
    }
  }

  const toggleViewMode = () => {
    viewMode.value = viewMode.value === VIEW_MODES.GRID ? VIEW_MODES.LIST : VIEW_MODES.GRID
  }

  const isGridView = () => viewMode.value === VIEW_MODES.GRID
  const isListView = () => viewMode.value === VIEW_MODES.LIST

  return {
    viewMode,
    setViewMode,
    toggleViewMode,
    isGridView,
    isListView
  }
}

const bookSquareView = createViewModeStore('bookSquare')
const myBooksView = createViewModeStore('myBooks')

export function useBookSquareViewMode() {
  return bookSquareView
}

export function useMyBooksViewMode() {
  return myBooksView
}
