import { createRouter, createWebHistory } from 'vue-router'
import ChatWindow from '@/components/ChatWindow.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: ChatWindow
  },
  {
    path: '/chat/:contactId',
    name: 'Chat',
    component: ChatWindow,
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 