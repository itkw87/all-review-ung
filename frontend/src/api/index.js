import axios from 'axios';
import router from '@/router';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const api = axios.create({
    baseURL: API_BASE_URL, // 요청 주소를 환경 변수 기준 주소로 자동 연동
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
}); // axios.create => Axios 인스턴스 반환 api({ url: '/test', method: 'get' })처럼 사용가능

// 요청 인터셉터(모든 요청에 토큰을 자동으로 포함)
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 응답 인터셉터(토큰 만료(401)시 자동 갱신 로직)
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 401 에러이고, 재시도한 적이 없을 때만 실행
        if (error.response && error.response.status === 401 && ! originalRequest._retry) {
            originalRequest._retry = true;

            try {
                // 토큰 재발급 요청 주소도 환경 변수 기준 주소로 자동 연동
                const response = await axios.post(`${API_BASE_URL}/api/user/refresh`, {
                    refreshToken: localStorage.getItem('refreshToken')
                });

                const { accessToken, refreshToken } = response.data;

                // 엑세스 토큰과 리프레시 토큰 모두 업데이트
                localStorage.setItem('accessToken', accessToken);
                localStorage.setItem('refreshToken', refreshToken);

                // 요청에 실패했던 헤더만 새 엑세스 토큰으로 변경
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;

                // 재요청
                return api(originalRequest);
            } catch (refreshError) {
                // 리프레시 토큰마저 유효하지 않으면 재로그인 하도록 유도
                console.log('[유효하지 않은 리프레시 토큰] 다시 로그인해야 합니다.');
                localStorage.clear();
                router.push("/");
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
)

export default api;