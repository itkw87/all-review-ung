import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import MapView from '../views/MapView.vue'
import LoginView from '../views/LoginView.vue'
import JoinView from '../views/JoinView.vue'
import KakaoRedirect from "@/views/KakaoRedirect.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/map',
      name: 'map',
      component: MapView,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/join',
      name: 'join',
      component: JoinView
    },
    {
      path: '/kakao/login',
      name: 'KakaoRedirect',
      component: KakaoRedirect
    },
  ],
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

  // 로그인이 필요한 페이지면
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 로그인이 되어있지 않으면 컷
    if (!isLoggedIn) {
      alert('로그인이 필요한 서비스 입니다.');
      next('/login');
    }
    // 로그인이 되어있으면 통과
    else {
      next();
    }
  }
  //로그인이 상관없는 페이지면
  else {
    // 통과
    next();
  }
});

export default router
