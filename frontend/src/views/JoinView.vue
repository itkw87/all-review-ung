<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-gray-50 px-4 py-12">
    <div class="max-w-md w-full bg-white rounded-[2.5rem] shadow-xl p-10 border border-gray-100">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-black text-gray-800 mb-2">회원가입 🦉</h2>
        <p class="text-gray-500 font-medium text-sm">정직한 리뷰 문화를 함께 만들어가요.</p>
      </div>

      <form @submit.prevent="handleJoin" class="space-y-6">
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">이메일 주소</label>
          <input
              v-model="email"
              type="text"
              placeholder="example@email.com"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              :class="{ 'border-red-500 focus:ring-red-100': email != '' && !isEmailValid }"
              required
          />
          <p v-if="email != '' && !isEmailValid" class="text-red-500 text-xs mt-2 ml-2 font-bold">
            올바른 이메일 형식이 아닙니다! 🦉❌
          </p>
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">닉네임</label>
          <input
              v-model="nickname"
              type="text"
              placeholder="멋진 별명을 지어주세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              required
          />
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">비밀번호</label>
          <input
              v-model="password"
              type="password"
              placeholder="8자 이상 입력해주세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              :class="{ 'border-red-500 focus:ring-red-100': password && password.length < 8 }"
              required
          />
          <p v-if="password && password.length < 8" class="text-red-500 text-xs mt-2 ml-2 font-bold">
            비밀번호는 최소 8자 이상이어야 합니다! 🦉❌
          </p>
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">비밀번호 확인</label>
          <input
              v-model="passwordConfirm"
              type="password"
              placeholder="비밀번호를 한 번 더 입력하세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              :class="{ 'border-red-500 focus:ring-red-100': password && passwordConfirm && password !== passwordConfirm }"
              required
          />
          <p v-if="password && passwordConfirm && password !== passwordConfirm" class="text-red-500 text-xs mt-2 ml-2 font-bold">
            비밀번호가 일치하지 않습니다! 🦉❌
          </p>
        </div>

        <button
            type="submit"
            class="w-full bg-orange-600 text-white font-black py-4 rounded-2xl hover:bg-orange-700 transition-all shadow-lg shadow-orange-200 cursor-pointer"
        >
          회원가입 하기
        </button>
      </form>

      <div class="mt-8 text-center text-sm font-medium text-gray-600">
        이미 회원이신가요?
        <RouterLink to="/login" class="text-orange-600 hover:underline font-bold ml-1">로그인하러 가기</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter();

const email = ref('');
const nickname = ref('');
const password = ref('');
const passwordConfirm = ref('');

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// 이메일 유효성 검사 로직 (정규식)
const isEmailValid = computed(() => {
  if (email.value === '') {
    return true;
  }
  return emailRegex.test(email.value);
});

// 환경 변수에서 기준 주소 가져오기
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const handleJoin = async () => {
  if (password.value !== passwordConfirm.value) {
    alert('비밀번호가 서로 다릅니다! 다시 확인해주시기 바랍니다.');
    return;
  }

  try {
    // 백엔드로 데이터 전송
    const response = await axios.post(`${API_BASE_URL}/api/user/join`, {
      emil: email.value,
      nkNm: nickname.value,
      pswd: password.value
    });

    // 성공처리
    if (response.status === 200 || response.status === 201) {
      alert(`${nickname.value}님, 회원가입을 축하드립니다! 로그인 페이지로 이동합니다.`);
      router.push('/login');
    }
  } catch (error) {
    // 에러 처리
    console.error('회원가입 실패:', error)
    const errorMsg = error.response?.data?.message || '회원가입에 실패했습니다. 잠시후 다시 시도해주시기 바랍니다.';
    debugger;
    alert(errorMsg);
  }

  console.log('회원가입 데이터 확인:', {
    emil: email.value,
    nkNm: nickname.value,
    pswd: password.value
  })
}
</script>