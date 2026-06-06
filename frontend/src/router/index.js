import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'BookSquare',
    component: () => import('@/views/BookSquare.vue')
  },
  {
    path: '/my-books',
    name: 'MyBooks',
    component: () => import('@/views/MyBooks.vue')
  },
  {
    path: '/borrow-apply',
    name: 'BorrowApply',
    component: () => import('@/views/BorrowApply.vue')
  },
  {
    path: '/borrow-history',
    name: 'BorrowHistory',
    component: () => import('@/views/BorrowHistory.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
