import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import NotFoundView from '@/views/NotFoundView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
   // {
   //   path: '/home',
   //   name: 'home',
   //   component: HomeView,
   // },
    {
      path: '/',
      name: 'register',
      alias: '/register', 
      //Load component lazily /KrasiG
      component: () => import('@/views/RegisterView.vue'),
      meta: { keepAlive: true },
    },
    {
      path: '/announcements',
      name: 'announcements',
      //Load component lazily /KrasiG
      component: () => import('@/views/AnnouncementsView.vue'),
      
    },
    //{
    //  path: '/about',
    //  name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
    //  component: () => import('@/views/AboutView.vue'),
    //},
    {
      //This route must be LAST - it catches all undefined routes (KrasiG)
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: NotFoundView
    }
  ],
})

export default router
