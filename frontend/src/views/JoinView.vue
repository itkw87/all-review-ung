<template>
  <div class="min-h-[80vh] flex items-center justify-center bg-gray-50 px-4 py-12">
    <div class="max-w-md w-full bg-white rounded-[2.5rem] shadow-xl p-10 border border-gray-100">
      <div class="text-center mb-10">
        <h2 class="text-3xl font-black text-gray-800 mb-2">회원가입 🦉</h2>
        <p class="text-gray-500 font-medium text-sm">정직한 리뷰 문화를 함께 만들어가요.</p>
      </div>

      <form @submit.prevent="handleJoin" class="space-y-6">
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
              placeholder="8자 이상 입력해주세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              required
          />
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-2">비밀번호 확인</label>
          <input
              v-model="passwordConfirm"
              type="passwordConfirm"
              placeholder="비밀번호를 한 번 더 입력하세요"
              class="w-full px-5 py-3 rounded-2xl border border-gray-200 focus:border-orange-500 focus:ring-4 focus:ring-orange-100 outline-none transition-all"
              required
          />
          <p v-if="password && passwordConfirm && password !== passwordConfirm" class="text-red-500 text-xs mt-2 ml-2 font-bold">
            비밀번호가 일치하지 않습니다! 🦉❌
          </p>
        </div>

        <button
            type="submit"
            class="w-full bg-orange-600 text-white font-black py-4 rounded-2xl hover:bg-orange-700 transition-all shadow-lg shadow-orange-200"
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
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter();

// 데이터 바구니들
const nickname = ref('');
const email = ref('');
const password = ref('');
const passwordConfirm = ref('');

// 환경 변수에서 기준 주소 가져오기
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const handleJoin = async () => {
  if (password.value !== passwordConfirm.value) {
    alert('비밀번호가 서로 다릅니다! 다시 확인해주시기 바랍니다.');
    return;
  }
  // 근데 왜 세미콜론 안찍어 마지막에?

  try {
    // 백엔드로 데이터 전송
    const response = await axios.post(`${API_BASE_URL}/api/user/join`, {
      nkNm: nickname.value,
      emil: email.value,
      pswd: password.value
    });

    // 성공처리
    // 이에러가 어떤것들이지 ?
    if (response.status === 200 || response.status === 201) {
      alert(`${nickname.value}님, 회원가입을 축하드립니다! 로그인 페이지로 이동합니다.`);
      router.push('/login');
    }
  } catch (error) {
    // 에러 처리
    console.error('회원가입 실패:', error)
    const errorMsg = error.response?.data?.message || '회원가입에 실패했습니다. 잠시후 다시 시도해주시기 바랍니다.';
    alert(errorMsg);
  }

  console.log('회원가입 데이터 확인:', {
    nickname: nickname.value,
    email: email.value,
    password: password.value
  })
  alert(`${nickname.value}님, 가입 신청됐부엉! 🦉`);
}
</script>