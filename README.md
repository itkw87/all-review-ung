# 🦉 올리뷰엉 (All-Review-Ung)

네이버 & 카카오 리뷰 통합 기반 평균 평점 제공 서비스

### 🌟 Key Feature

* 리뷰 데이터 통합: 네이버와 카카오맵의 리뷰 데이터를 수집 및 분석
* 객관적 지표 제공: 두 플랫폼의 리뷰를 합산/평균하여 사용자에게 더 신뢰도 높은 평점 제공
* 위치 기반 서비스: 카카오맵 API를 활용한 장소 검색 및 시각화
* 배치 자동화: Spring Batch를 이용한 정기적인 리뷰 데이터 크롤링 및 저장 (추가)

### 🛠 Tech Stack

* Frontend: Vue 3, Vite
* Backend: Java Spring Boot 3.3.5, Spring Batch 5.1.2 (수정)
* Database: MariaDB (MyBatis 연동) (수정)
* API: Kakao Maps API, Selenium (추가)

### 🚧 현재 진행 상황 (Current Progress)

* ✅ 프로젝트 초기 환경 설정 (Vite + Vue 3)
* ✅ 카카오맵 API 연동 및 기본 지도 레이아웃 구현
* ✅ Spring Boot & Spring Batch 백엔드 통합 환경 구축 (추가)
* ✅ MariaDB 연동 및 배치 메타 데이터 테이블 설정 (추가)
* ✅ 외부 리뷰 수집 배치 프로세스(BCH000001) 기본 구현 (추가)
* ⬜ 네이버/카카오 실제 리뷰 크롤링 상세 로직 고도화 (진행중)
* ⬜ 플랫폼별 평점 합산 및 평균 계산 알고리즘 구현

© 2026 Choi Ki-hyeon.