<script setup>
import { RouterView, RouterLink } from 'vue-router'
</script>

<template>
  <div class="app-container">
    <nav class="flex items-center justify-between px-8 py-4 bg-white border-b border-gray-100 shadow-sm sticky top-0 z-50">
      <div class="flex items-center space-x-2">
        <RouterLink to="/" class="text-2xl font-black text-orange-600 flex items-center">
          올리뷰엉 🦉
        </RouterLink>
      </div>

      <div class="flex items-center space-x-6">
        <RouterLink to="/" class="text-gray-600 hover:text-orange-600 font-medium">홈</RouterLink>
        <RouterLink to="/join" class="text-gray-600 hover:text-orange-600 font-medium">회원가입</RouterLink>
        <RouterLink
            to="/login"
            class="bg-orange-600 text-white px-5 py-2 rounded-full font-bold hover:bg-orange-700 transition-colors inline-block"
        >
          로그인
        </RouterLink>
      </div>
    </nav>

    <RouterView />
  </div>
</template>

<style>
/* 전역 초기화 */
body {
  margin: 0;
  padding: 0;
  font-family: 'Pretendard', sans-serif; /* 폰트 깔끔하게 */
}

.app-container {
  min-height: 100vh;
}
</style>