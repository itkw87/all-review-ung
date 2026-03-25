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

      <div class="mt-8 text-center text-sm font-medium text-gray-600">
        아직 회원이 아니신가요?
        <RouterLink to="/join" class="text-orange-600 hover:underline font-bold ml-1">회원 가입하기</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 입력받을 변수 선언 (바구니 만들기)
const email = ref('')
const password = ref('')

const handleLogin = () => {
  // 나중에 백엔드 API 연결할 곳입니다.
  console.log('로그인 정보:', email.value, password.value)
  alert('로그인 시도 중부엉! 🦉')
}
</script>