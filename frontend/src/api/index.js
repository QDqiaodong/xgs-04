import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export const cityAPI = {
  getAll: () => api.get('/cities'),
  getByProvince: (province) => api.get(`/cities/province/${province}`)
}

export const categoryAPI = {
  getAll: () => api.get('/categories')
}

export const tagAPI = {
  getAll: () => api.get('/tags'),
  getById: (id) => api.get(`/tags/${id}`),
  getByName: (name) => api.get(`/tags/name/${name}`),
  create: (data) => api.post('/tags', data),
  update: (id, data) => api.put(`/tags/${id}`, data),
  delete: (id) => api.delete(`/tags/${id}`)
}

export const userAPI = {
  getAll: () => api.get('/users'),
  getById: (id) => api.get(`/users/${id}`),
  getByUsername: (username) => api.get(`/users/username/${username}`),
  create: (data) => api.post('/users', data)
}

export const bookAPI = {
  query: (params) => api.post('/books/query', params),
  getById: (id) => api.get(`/books/${id}`),
  getByOwner: (ownerId) => api.get(`/books/owner/${ownerId}`),
  create: (data) => api.post('/books', data),
  update: (id, data) => api.put(`/books/${id}`, data),
  delete: (id) => api.delete(`/books/${id}`),
  addTags: (bookIds, tagIds) => api.post('/books/tags/add', { bookIds, tagIds }),
  removeTags: (bookIds, tagIds) => api.post('/books/tags/remove', { bookIds, tagIds }),
  batchOperate: (data) => api.post('/books/batch', data)
}

export const borrowRecordAPI = {
  query: (params) => api.post('/borrow-records/query', params),
  getByBorrower: (borrowerId) => api.get(`/borrow-records/borrower/${borrowerId}`),
  getByOwner: (ownerId) => api.get(`/borrow-records/owner/${ownerId}`),
  getById: (id) => api.get(`/borrow-records/${id}`),
  validateCreate: (data) => api.post('/borrow-records/validate-create', data),
  create: (data) => api.post('/borrow-records', data),
  validateApprove: (id) => api.get(`/borrow-records/${id}/validate-approve`),
  approve: (id) => api.put(`/borrow-records/${id}/approve`),
  reject: (id) => api.put(`/borrow-records/${id}/reject`),
  confirmBorrow: (id) => api.put(`/borrow-records/${id}/confirm-borrow`),
  confirmReturn: (id) => api.put(`/borrow-records/${id}/confirm-return`),
  saveFilter: (userId, filter) => api.post(`/borrow-records/filter/${userId}`, filter),
  getFilter: (userId) => api.get(`/borrow-records/filter/${userId}`),
  identifyOverdue: () => api.post('/borrow-records/overdue/identify'),
  queryOverdue: (params) => api.post('/borrow-records/overdue/query', params)
}

export const borrowRuleAPI = {
  get: () => api.get('/borrow-rules'),
  update: (data) => api.put('/borrow-rules', data)
}

export const favoriteAPI = {
  getByUser: (userId) => api.get(`/favorites/user/${userId}`),
  check: (userId, bookId) => api.get(`/favorites/user/${userId}/check/${bookId}`),
  getCount: (userId) => api.get(`/favorites/user/${userId}/count`),
  add: (userId, bookId) => api.post('/favorites', { userId, bookId }),
  remove: (userId, bookId) => api.delete(`/favorites/user/${userId}/book/${bookId}`),
  toggle: (userId, bookId) => api.post('/favorites/toggle', { userId, bookId })
}

export const reviewAPI = {
  query: (params) => api.post('/reviews/query', params),
  create: (data) => api.post('/reviews', data),
  getById: (id) => api.get(`/reviews/${id}`),
  getByBook: (bookId) => api.get(`/reviews/book/${bookId}`),
  getBookStats: (bookId) => api.get(`/reviews/book/${bookId}/stats`),
  getByUser: (userId) => api.get(`/reviews/user/${userId}`),
  getByBorrowRecord: (borrowRecordId) => api.get(`/reviews/borrow-record/${borrowRecordId}`),
  hasReviewedBorrowRecord: (borrowRecordId) => api.get(`/reviews/borrow-record/${borrowRecordId}/exists`)
}

export default api
