<template>
  <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50">
    <div class="animate-bounce mb-4">
      <span class="text-6xl">🦉</span>
    </div>
    <h2 class="text-2xl font-bold text-gray-800">카카오 로그인 중...</h2>
    <p class="text-gray-500 mt-2">잠시만 기다려주세요!</p>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

onMounted(async () => {
  // 1. URL 주소창에서 'code=' 뒤에 있는 인가 코드를 쏙 뽑아옵니다.
  const code = route.query.code

  if (code) {
    try {
      const response = await axios.get(`http://localhost:8080/api/auth/kakao/login?code=${code}`)

      // 로그인 성공시 메인으로 이동
      console.log('로그인 성공!', response.data)
      alert('로그인 성공! 🦉')
      router.push('/')

    } catch (error) {
      console.error('로그인 실패:', error)
      alert('로그인에 실패했습니다.')
      router.push('/login')
    }
  }
})
</script>