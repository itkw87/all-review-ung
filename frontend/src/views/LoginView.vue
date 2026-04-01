<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-gray-50 px-4">
    <div class="max-w-md w-full bg-white rounded-[2.5rem] shadow-xl p-10 border border-gray-100">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-black text-gray-800 mb-2">로그인 🦉</h2>
        <p class="text-gray-500 font-medium text-sm">OO님의 정직한 리뷰를 들려주세요.</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">이메일 주소</label>
          <input
              v-model="email"
              type="email"
              placeholder="example@email.com"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              required
          />
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">비밀번호</label>
          <input
              v-model="password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              required
          />
        </div>

        <button
            type="submit"
            class="w-full bg-orange-600 text-white font-black py-4 rounded-2xl hover:bg-orange-700 transition-all shadow-lg shadow-orange-200"
        >
          로그인
        </button>
      </form>

      <div class="mt-8">
        <div class="relative flex items-center mb-8">
          <div class="flex-grow border-t border-gray-200"></div>
          <span class="flex-shrink mx-4 text-gray-400 text-xs font-bold uppercase">또는</span>
          <div class="flex-grow border-t border-gray-200"></div>
        </div>

        <div class="flex justify-center">
          <button @click="handleKakaoLogin" class="cursor-pointer transition-transform hover:scale-105 active:scale-95">
            <img
                :src="kakaoLoginBtn"
                alt="카카오 로그인"
                class="w-[180px] drop-shadow-md"
            />
          </button>
        </div>
      </div>

      <div class="mt-8 text-center text-sm font-medium text-gray-600">
        아직 회원이 아니신가요?
        <RouterLink to="/join" class="text-orange-600 hover:underline font-bold ml-1">회원 가입하기</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/index.js';
import kakaoLoginBtn from '@/assets/kakao_login.png'

const router = useRouter();
const email = ref('')
const password = ref('')

const handleLogin = async () => {
  try {
    // 로그인 요청
    const response = await api.post('/api/user/login', {
      emil: email.value,
      pswd: password.value
    });

    // 로그인 성공시
    if (response.data.status === 'SUCCESS') {
      const { accessToken, refreshToken, nickname, email: userEmail } = response.data;

      // localStorage 값 셋팅
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('nickname', nickname);
      localStorage.setItem('email', userEmail);
      localStorage.setItem('isLoggedIn', 'true'); // 로그인 플래그

      alert('로그인 성공! 🦉')
      // 로그인 성공시 메인으로 이동
      router.push('/')
    }
  } catch (error) {
    console.error('로그인 에러:', error);
    // 백엔드에서 보낸 에러 메시지가 있으면 띄워주고, 없으면 기본 메시지!
    const errorMsg = error.response?.data?.message || '아이디 또는 비밀번호를 확인해주세요! 🦉';
    alert(errorMsg);
  }
}
const handleKakaoLogin = () => {
  const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=${import.meta.env.VITE_KAKAO_REST_API_KEY}&redirect_uri=${import.meta.env.VITE_KAKAO_REDIRECT_URI}`;

  window.location.href = KAKAO_AUTH_URL;
};

</script>