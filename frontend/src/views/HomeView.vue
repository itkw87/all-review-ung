<script setup>
// 🦉 기현님의 정교한 데이터 로직 (Trust Score 반영)
const rankings = {
  overall: [
    { place: '제주 꽁치마을', category: '음식점', naverRate: 4.7, kakaoRate: 4.5, trustScore: 96, count: 210, photo: 'https://images.unsplash.com/photo-1512132411229-c30391241dd8?q=80&w=400&auto=format&fit=crop', description: '비린내 제로! 꽁치킬러들도 인정한 전국구 맛집.' },
    { place: '노트북 뷰 카페', category: '카페', naverRate: 4.85, kakaoRate: 4.2, trustScore: 92, count: 152, photo: 'https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/cEb3/image/yIY5O1qAPgCBTrMwl1Myx1kiC7g.jpg', description: '카공족들의 성지, 압도적인 뷰와 콘센트 보유량으로 통합 1위!' },
    { place: '순한맛 천국', category: '음식점', naverRate: 4.3, kakaoRate: 4.8, trustScore: 89, count: 189, photo: 'https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?q=80&w=400&auto=format&fit=crop', description: '위장보호가들의 안식처. 파모티딘 없이 즐기는 최고의 한 끼.' }
  ],
  restaurant: [
    { name: '꽁치킬러', place: '제주 꽁치마을', naverRate: 4.7, kakaoRate: 4.5, trustScore: 96, count: 210, photo: 'https://images.unsplash.com/photo-1512132411229-c30391241dd8?q=80&w=400&auto=format&fit=crop', quote: '꽁치조림 레시피 공유합니다.' },
    { name: '진라면순한맛', place: '순한맛 천국', naverRate: 4.3, kakaoRate: 4.8, trustScore: 89, photo: 'https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?q=80&w=400&auto=format&fit=crop', quote: '맵지 않은 맛집 전문가.' },
    { name: '위장보호가', place: '편안한 식탁', naverRate: 4.1, kakaoRate: 4.3, trustScore: 82, photo: 'https://images.unsplash.com/photo-1512132411229-c30391241dd8?q=80&w=400&auto=format&fit=crop', quote: '파모티딘 없이도 편한 식당.' }
  ],
  cafe: [
    { name: '디카페인파', place: '밤샘 전용 카페', naverRate: 4.9, kakaoRate: 4.4, trustScore: 94, count: 156, photo: 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?q=80&w=400&auto=format&fit=crop', quote: '밤샘 코딩엔 디카페인이 필수.' },
    { name: '뷰맛집부엉', place: '노트북 뷰', naverRate: 4.8, kakaoRate: 4.1, trustScore: 92, count: 152, photo: 'https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/cEb3/image/yIY5O1qAPgCBTrMwl1Myx1kiC7g.jpg', quote: '노트북 하기 좋은 카페 추천.' },
    { name: '에스프레소', place: '진한 한잔', naverRate: 4.2, kakaoRate: 4.7, trustScore: 88, count: 139, photo: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?q=80&w=400&auto=format&fit=crop', quote: '진한 커피만큼 진한 코드.' }
  ]
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 font-sans">
    <header class="bg-orange-600 py-24 px-4 text-center">
      <h1 class="text-6xl font-black text-white mb-8 tracking-tighter">올리뷰엉 🦉</h1>
      <p class="text-orange-100 mb-12 text-xl font-medium">기현님의 소중한 리뷰를 들려주세요</p>
      <div class="max-w-3xl mx-auto flex bg-white rounded-full shadow-2xl overflow-hidden p-1.5 border-4 border-orange-500/30">
        <input type="text" placeholder="가게 이름이나 지역을 검색해보세요" class="flex-grow px-8 py-4 outline-none text-gray-700 text-lg" />
        <button @click="$router.push('/map')" class="bg-gray-900 text-white px-12 py-4 rounded-full hover:bg-black transition-all font-bold text-lg cursor-pointer">검색</button>
      </div>
    </header>

    <main class="max-w-7xl mx-auto py-20 px-6 space-y-32 text-left">
      <section>
        <div class="flex items-center justify-between mb-12 text-left">
          <h2 class="text-3xl font-extrabold text-gray-800">🏆 통합 명예의 전당</h2>
          <a href="#" class="text-orange-600 font-bold hover:underline underline-offset-4">전체 랭킹 보기 →</a>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
          <div v-for="(item, index) in rankings.overall" :key="index"
               class="group relative h-[480px] bg-white rounded-[3rem] shadow-sm border border-gray-100 overflow-hidden hover:shadow-2xl transition-all duration-500 cursor-pointer">
            <img :src="item.photo" :alt="item.place" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
            <div class="absolute inset-0 bg-gradient-to-t from-black/95 via-black/40 to-transparent"></div>
            <div :class="[
       'absolute top-4 left-4 w-16 h-16 rounded-2xl flex items-center justify-center font-black text-3xl shadow-2xl transition-all duration-500 group-hover:rotate-12 group-hover:scale-110 border border-white/40 z-10',
       {
         'bg-gradient-to-br from-yellow-300 via-yellow-500 to-yellow-600 text-yellow-950': index === 0, /* 금 */
         'bg-gradient-to-br from-gray-100 via-gray-300 to-gray-400 text-gray-800': index === 1,       /* 은 */
         'bg-gradient-to-br from-amber-600 via-amber-700 to-amber-800 text-amber-100': index === 2   /* 동 (진짜 구리빛) */
       }
     ]">
              {{ index + 1 }}
            </div>

            <div class="absolute bottom-8 left-8 right-8 text-white">
              <div class="inline-block px-3 py-1 bg-white/20 backdrop-blur-md rounded-lg text-xs font-bold mb-3 uppercase tracking-wider">{{ item.category }} 부문 1위</div>
              <h3 class="text-3xl font-black mb-4">{{ item.place }}</h3>

              <div class="space-y-4 border-t border-white/20 pt-5">
                <div class="flex items-center justify-between font-bold text-sm">
                  <div class="flex space-x-4"><span class="text-green-400">N {{ item.naverRate }}</span><span class="text-yellow-400">K {{ item.kakaoRate }}</span></div>
                  <div class="text-xs text-gray-400">리뷰 {{ item.count }}개 분석</div>
                </div>
                <div class="flex items-center justify-between bg-orange-600 px-5 py-3 rounded-2xl shadow-xl border border-white/20">
                  <div class="flex flex-col">
                    <span class="text-[10px] font-black uppercase text-orange-200">Trust Score</span>
                    <span class="text-2xl font-black text-white">{{ item.trustScore }}점</span>
                  </div>
                  <div class="text-right">
                    <span class="text-[10px] block text-green-300 font-bold">검증 완료</span>
                    <span class="text-[9px] text-white/50">올리뷰엉 엔진</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div class="space-y-24">
        <section v-for="category in ['restaurant', 'cafe']" :key="category">
          <h2 class="text-3xl font-extrabold text-gray-800 mb-12 flex items-center">
            <span class="mr-4 text-4xl">{{ category === 'restaurant' ? '🍴' : '☕' }}</span>
            {{ category === 'restaurant' ? '음식점' : '카페' }} 부문 TOP 3
          </h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-10">
            <div v-for="(item, index) in rankings[category]" :key="index"
                 class="group bg-white rounded-[2.5rem] shadow-sm border border-gray-100 overflow-hidden hover:shadow-2xl transition-all duration-500 cursor-pointer">
              <div class="relative aspect-[16/10] overflow-hidden">
                <img :src="item.photo" :alt="item.place" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
                <div class="absolute top-4 left-4 w-10 h-10 bg-white text-orange-600 rounded-xl flex items-center justify-center font-black shadow-lg">{{ index + 1 }}</div>
              </div>
              <div class="p-8">
                <div class="mb-5"><p class="text-xs font-bold text-orange-600 mb-1">리뷰어 {{ item.name }} 추천</p><h4 class="text-2xl font-bold text-gray-800">{{ item.place }}</h4></div>

                <div class="bg-gray-50 rounded-2xl p-5 space-y-4 border border-gray-100">
                  <div class="flex items-center justify-between">
                    <div class="flex space-x-3 text-sm font-black">
                      <span class="text-green-600">N {{ item.naverRate }}</span>
                      <span class="text-yellow-600">K {{ item.kakaoRate }}</span>
                    </div>
                    <div class="text-orange-600 font-black">🏆 {{ item.trustScore }}점</div>
                  </div>
                  <div class="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div class="h-full bg-orange-500 rounded-full" :style="{ width: item.trustScore + '%' }"></div>
                  </div>
                </div>
                <p class="mt-5 text-gray-500 text-sm italic line-clamp-2">"{{ item.quote }}"</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>