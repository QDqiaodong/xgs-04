import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'BookSquare',
    component: () => import('@/views/BookSquare.vue')
  },
  {
    path: '/book/:id',
    name: 'BookDetail',
    component: () => import('@/views/BookDetail.vue')
  },
  {
    path: '/my-books',
    name: 'MyBooks',
    component: () => import('@/views/MyBooks.vue')
  },
  {
    path: '/my-favorites',
    name: 'MyFavorites',
    component: () => import('@/views/MyFavorites.vue')
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
  },
  {
    path: '/borrow-approval',
    name: 'BorrowApproval',
    component: () => import('@/views/BorrowApproval.vue')
  },
  {
    path: '/overdue-management',
    name: 'OverdueManagement',
    component: () => import('@/views/OverdueManagement.vue')
  },
  {
    path: '/notifications',
    name: 'NotificationCenter',
    component: () => import('@/views/NotificationCenter.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
