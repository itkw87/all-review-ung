<template>
  <div class="map-page-wrapper">
    <nav class="icon-sidebar">
      <div
          class="menu-item"
          :class="{ active: activeMenu === 'home' }"
          @click="activeMenu = 'home'"
      >
        <span class="icon">🦉</span>
        <span class="label">홈</span>
      </div>
      <div
          class="menu-item"
          :class="{ active: activeMenu === 'save' }"
          @click="activeMenu = 'save'"
      >
        <span class="icon">⭐</span>
        <span class="label">저장</span>
      </div>
    </nav>

    <aside class="search-panel" :class="{ 'collapsed': !isPanelOpen }">
      <div class="search-header">
        <div class="search-input-wrapper">
          <input type="text" placeholder="맛집을 검색해보부엉! 🦉" class="main-search-input" />
          <button class="search-btn">검색</button>
        </div>
      </div>

      <div class="search-results">
        <div class="empty-state">
          <p class="main-text">
            {{ activeMenu === 'home' ? '주변의 숨은 맛집을 찾아보세요' : 'OO님이 찜한 소중한 맛집들' }}
          </p>
          <p class="sub-text">부엉이가 정성껏 리뷰를 모아왔어요 🦉</p>
        </div>
      </div>

      <button class="toggle-btn" @click="togglePanel">
        {{ isPanelOpen ? '◀' : '▶' }}
      </button>
    </aside>

    <div id="map" class="map-canvas"></div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';

const isPanelOpen = ref(true);
const togglePanel = () => { isPanelOpen.value = !isPanelOpen.value; };

// 핵심: 현재 활성화된 메뉴 상태 (기본값: 'home')
const activeMenu = ref('home');

onMounted(() => {
  if (window.kakao && window.kakao.maps) {
    window.kakao.maps.load(() => {
      const container = document.getElementById('map');
      const options = {
        center: new window.kakao.maps.LatLng(37.5665, 126.9780),
        level: 3
      };
      new window.kakao.maps.Map(container, options);
    });
  }
});
</script>

<style scoped>
/* (기존 스타일 그대로 유지하되, active 관련만 확인) */
.map-page-wrapper { position: relative; height: calc(100vh - 72px); width: 100%; overflow: hidden; background: #ffffff; }

.icon-sidebar {
  position: absolute; top: 0; left: 0; width: 72px; height: 100%;
  background: #ffffff; border-right: 1px solid #f3f4f6;
  display: flex; flex-direction: column; align-items: center; padding-top: 24px; z-index: 30;
}

.menu-item {
  display: flex; flex-direction: column; align-items: center;
  margin-bottom: 32px; cursor: pointer; color: #9ca3af; transition: all 0.2s;
  width: 100%; /* 너비 꽉 채워야 보더가 예쁘게 나옴 */
}

/* 활성 메뉴: 왼쪽 주황색 보더 포인트 */
.menu-item.active {
  color: #ea580c;
  position: relative;
}

.menu-item.active::before {
  content: "";
  position: absolute;
  left: 0; top: 50%; transform: translateY(-50%);
  width: 4px; height: 24px;
  background: #ea580c;
  border-radius: 0 4px 4px 0;
}

.menu-item .icon { font-size: 24px; margin-bottom: 4px; }
.menu-item .label { font-size: 11px; font-weight: 700; }

/* (이하 검색 패널 및 지도 스타일은 이전과 동일) */
.search-panel { position: absolute; top: 0; left: 72px; width: 360px; height: 100%; background: white; z-index: 20; transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); box-shadow: 10px 0 20px rgba(0, 0, 0, 0.03); }
.search-panel.collapsed { transform: translateX(-360px); }
.search-header { padding: 24px 20px; }
.search-input-wrapper { display: flex; align-items: center; background: #f9fafb; padding: 6px 6px 6px 16px; border-radius: 12px; border: 1px solid #e5e7eb; }
.search-input-wrapper:focus-within { border-color: #ea580c; box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.1); }
.main-search-input { background: transparent; border: none; outline: none; flex: 1; font-size: 14px; }
.search-btn { background: #111827; color: white; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; }
.toggle-btn { position: absolute; top: 50%; right: -24px; transform: translateY(-50%); width: 24px; height: 48px; background: white; border: 1px solid #e5e7eb; border-left: none; border-radius: 0 8px 8px 0; cursor: pointer; color: #ea580c; z-index: 21; display: flex; align-items: center; justify-content: center; font-size: 10px; }
.map-canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 10; }
.search-results { padding: 40px 20px; text-align: center; }
.empty-state .main-text { font-size: 16px; font-weight: 700; color: #374151; margin-bottom: 8px; }
.empty-state .sub-text { font-size: 13px; color: #9ca3af; }
</style>