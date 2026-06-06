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
  delete: (id) => api.delete(`/books/${id}`)
}

export const borrowRecordAPI = {
  getByBorrower: (borrowerId) => api.get(`/borrow-records/borrower/${borrowerId}`),
  getByOwner: (ownerId) => api.get(`/borrow-records/owner/${ownerId}`),
  getById: (id) => api.get(`/borrow-records/${id}`),
  create: (data) => api.post('/borrow-records', data),
  approve: (id) => api.put(`/borrow-records/${id}/approve`),
  reject: (id) => api.put(`/borrow-records/${id}/reject`),
  confirmBorrow: (id) => api.put(`/borrow-records/${id}/confirm-borrow`),
  confirmReturn: (id) => api.put(`/borrow-records/${id}/confirm-return`),
  saveFilter: (userId, filter) => api.post(`/borrow-records/filter/${userId}`, filter),
  getFilter: (userId) => api.get(`/borrow-records/filter/${userId}`)
}

export default api
