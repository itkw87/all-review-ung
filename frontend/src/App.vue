<template>
  <div class="app-container">
    <nav
        class="flex items-center justify-between px-8 py-4 bg-white border-b border-gray-100 shadow-sm sticky top-0 z-50">
      <div class="flex items-center space-x-2">
        <RouterLink to="/" class="text-2xl font-black text-orange-600 flex items-center">
          올리뷰엉 🦉
        </RouterLink>
      </div>

      <template v-if="!isLoggedIn">
        <div class="flex items-center space-x-6">
          <RouterLink to="/" class="text-gray-600 hover:text-orange-600 font-medium">홈</RouterLink>
          <RouterLink to="/join" class="text-gray-600 hover:text-orange-600 font-medium">회원가입
          </RouterLink>
          <RouterLink
              to="/login"
              class="bg-orange-600 text-white px-5 py-2 rounded-full font-bold hover:bg-orange-700 transition-colors inline-block"
          >
            로그인
          </RouterLink>
        </div>
      </template>

      <template v-else>
        <div class="flex items-center space-x-4">
              <span class="text-gray-800 font-bold">
                <span class="text-orange-600">{{ nickname }}</span>님 환영합니다! 🦉
              </span>
          <button
              @click="handleLogout"
              class="text-sm text-gray-400 hover:text-red-500 font-medium transition-colors cursor-pointer"
          >
            로그아웃
          </button>
        </div>
      </template>
    </nav>
    <RouterView/>
  </div>
</template>

<script setup>
import {RouterView, RouterLink, useRoute, useRouter} from 'vue-router';
import {ref, watch} from 'vue';
import axios from 'axios';

const route = useRoute(); // 현재 주소 정보를 가져옴
const router = useRouter();
const isLoggedIn = ref(false);
const nickname = ref('');

const checkLoginStatus = () => {
  if (localStorage.getItem('isLoggedIn') === 'true') {
    isLoggedIn.value = true;
    nickname.value = localStorage.getItem('nickname');
  } else {
    isLoggedIn.value = false;
    nickname.value = '';
  }
}

watch(() => route.path, () => {
  checkLoginStatus();
},{ immediate: true }); // 감시 시작시 일단 한번 실행하는 옵션

const handleLogout = async () => {
  try {
    const token = localStorage.getItem('accessToken');

    await axios.post('http://localhost:8080/api/user/logout', {}, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });

    console.log('[Logout] 서버에서 정상적으로 로그아웃 처리되었습니다.');

  } catch (error) {
    console.error('[Logout] 서버 통신 실패', error)
  } finally {
    localStorage.clear();
    isLoggedIn.value = false;
    nickname.value = '';

    alert("로그아웃 되었습니다.");

    // 메인화면으로 이동
    router.push('/');
  }
}
</script>

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