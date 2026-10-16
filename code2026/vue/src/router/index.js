import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/manager',
      component: () => import('@/views/Manager.vue'),
      redirect: '/manager/person',
      children: [
        { path: 'person', component: () => import('@/views/manager/Person.vue') },
        { path: 'password', component: () => import('@/views/manager/Password.vue') }
      ]
    },
    {
      path: '/front',
      component: () => import('@/views/Front.vue'),
      redirect: '/front/overview',
      children: [
        { path: 'overview', component: () => import('@/views/front/Overview.vue') },
        { path: 'memories', component: () => import('@/views/front/Memories.vue') },
        { path: 'sops', component: () => import('@/views/front/Sops.vue') },
        { path: 'person', component: () => import('@/views/front/Person.vue') },
        { path: 'password', component: () => import('@/views/front/Password.vue') }
      ]
    },
    { path: '/login', component: () => import('@/views/Login.vue') },
    { path: '/register', component: () => import('@/views/Register.vue') }
  ]
})

router.beforeEach(() => {
  window.scroll({ top: 0, behavior: 'smooth' })
})

export default router
