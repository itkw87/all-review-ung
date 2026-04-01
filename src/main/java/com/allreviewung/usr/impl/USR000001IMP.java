package com.allreviewung.usr.impl;

import com.allreviewung.global.exception.ArvuBusinessException;
import com.allreviewung.security.JwtTokenProvider;
import com.allreviewung.usr.dao.USR000001DAO;
import com.allreviewung.usr.dto.USR00000101DTO;
import com.allreviewung.usr.dto.KakaoTokenDTO;
import com.allreviewung.usr.dto.KakaoUserDTO;
import com.allreviewung.usr.service.USR000001SVC;
import com.allreviewung.usr.vo.USR00000101IN;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class USR000001IMP implements USR000001SVC {

    private final USR000001DAO daoUSR000001;

    private final BCryptPasswordEncoder passwordEncoder;

    private final RestTemplate restTemplate;

    private final JwtTokenProvider tokenProvider;

    // yml 설정값 불러오기
    @Value("${kakao.kakao-rest-api-key}")
    private String clientId;

    @Value("${kakao.kakao-client-secret-key}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Override
    @Transactional
    public int insertUser(USR00000101IN inParam) {
        log.info("[IMP] 회원가입 처리 시작 - 데이터: {}", inParam);

        if (daoUSR000001.selectEmilDupChk(inParam.getEmil()) > 0) {
            throw new ArvuBusinessException("이미 사용 중인 이메일입니다");
        }

        if (daoUSR000001.selectNkNmDupChk(inParam.getNkNm()) > 0) {
            throw new ArvuBusinessException("이미 사용 중인 닉네임입니다");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(inParam.getPswd());
        inParam.setPswd(encodedPassword);

        // 회원 등록(회원 가입)
        int result = daoUSR000001.insertUser(inParam);

        log.info("[IMP] DB 처리 결과 (행 개수): {}", result);
        return result;
    }

    @Override
    @Transactional
    public Map<String, String> login(USR00000101IN inParam) {
        // 회원정보 조회
        USR00000101DTO user = daoUSR000001.selectUser(inParam.getEmil());

        // 비밀번호 불일치시
        if (user == null || !passwordEncoder.matches(inParam.getPswd(), user.getPswd())) {
            throw new ArvuBusinessException("아이디 또는 비밀번호가 일치하지 않습니다!", HttpStatus.UNAUTHORIZED);
        }
        
        int userId = user.getUserId();
        String email = user.getEmil();
        String nickname = user.getNkNm();

        String accessToken = tokenProvider.createAccessToken(userId, user.getNkNm());
        String refreshToken = tokenProvider.createRefreshToken(userId);

        // 리프레시 토큰 업데이트
        USR00000101IN updateParam = new USR00000101IN();
        updateParam.setUserId(userId);
        updateParam.setRfrsTokn(refreshToken);
        daoUSR000001.updateRfrsTokn(updateParam);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        result.put("nickname", nickname);
        result.put("email", email);
        result.put("status", "SUCCESS");

        return result;
    }


    @Override
    @Transactional
    public Map<String, String> kakaoLogin(String code) {
        log.info("[IMP] 카카오 로그인 code: {}", code);

        // 카카오 토큰 받기
        String tokenUrl = "https://kauth.kakao.com/oauth/token";
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 토큰 param들
        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("grant_type", "authorization_code");
        tokenParams.add("client_id", clientId);           // 올리뷰엉 REST API키
        tokenParams.add("client_secret", clientSecret);   // 올리뷰엉 비밀키
        tokenParams.add("redirect_uri", redirectUri);     // 로그인 후 돌아올 올리뷰엉 주소
        tokenParams.add("code", code);

        // 카카오 서버에 토큰 요청
        KakaoTokenDTO tokenDto = restTemplate.postForObject(tokenUrl, new HttpEntity<>(tokenParams, tokenHeaders), KakaoTokenDTO.class);

        if (tokenDto == null || tokenDto.getAccessToken() == null) {
            throw new ArvuBusinessException("카카오 토큰을 받아오지 못했습니다.");
        }

        String userUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(tokenDto.getAccessToken());

        KakaoUserDTO kakaoUser = restTemplate.exchange(userUrl, HttpMethod.GET, new HttpEntity<>(userHeaders), KakaoUserDTO.class).getBody();

        if (kakaoUser == null) {
            throw new ArvuBusinessException("카카오 유저 정보를 가져오지 못했습니다.");
        }

        log.info("[IMP] 카카오 사용자 정보 응답 확인");
        log.info("[IMP] ------------------------------------------------------");
        log.info("[IMP] 1. 카카오 고유 ID (id): {}", kakaoUser.getId());
        log.info("[IMP] 2. 카카오 닉네임 (nickname): {}", kakaoUser.getKakaoAccount().getProfile().getNickname());
        log.info("[IMP] ------------------------------------------------------");

        int userId = 0;
        String kakaoId = String.valueOf(kakaoUser.getId());
        String email = kakaoId + "@kakao.user";    // 카카오 사용자 정보에서 이메일 정보를 주지 않으므로 해당 형식으로 임시 사용
        String nickname = "";

        USR00000101IN commonParam = new USR00000101IN();

        commonParam.setSnsId(kakaoId);
        commonParam.setSnsDvcd("KKO");

        USR00000101DTO user = daoUSR000001.selectSnsUser(commonParam);

        if (user != null) {
            userId = user.getUserId();
            commonParam.setUserId(userId);
            nickname = user.getNkNm();

            log.info("[IMP] 기존 가입 유저 로그인 진행: {}", nickname);
        } // if (user != null)
        else {
            log.info("[IMP] 신규 유저임. 자동 회원가입 진행: {}", commonParam);

            commonParam.setEmil(email);

            String baseNickName = kakaoUser.getKakaoAccount().getProfile().getNickname();
            nickname = baseNickName;

            // 닉네임이 중복되지 않을 때까지 새로 생성하여 할당
            while (daoUSR000001.selectNkNmDupChk(nickname) > 0) {
                int randomNum = (int)(Math.random() * 9000) + 1000;    // 1000 - 9999
                nickname = baseNickName + "#" + randomNum;
            }
            commonParam.setNkNm(nickname);
            commonParam.setSnsId(kakaoId);
            // 일반 로그인 뚫림 방지용 비밀번호 난수로 생성하여 암호화
            commonParam.setPswd(passwordEncoder.encode("KKO_USER_" + UUID.randomUUID()));

            int result = this.insertUser(commonParam);

            if (result != 1) {
                throw new ArvuBusinessException("간편 로그인에 실패하였습니다.");
            }
            userId = commonParam.getUserId();
        } // else
        // 토큰 생성
        String accessToken = tokenProvider.createAccessToken(userId, nickname);
        String refreshToken = tokenProvider.createRefreshToken(userId);

        // 리프레시 토큰 DB 저장
        commonParam.setRfrsTokn(refreshToken);
        int updateCount = daoUSR000001.updateRfrsTokn(commonParam);

        if (updateCount == 0) {
            log.error("[JWT] 리프레시 토큰 저장 실패! 카카오 고유 ID: {}", kakaoId);
            throw new ArvuBusinessException("토큰 발급에 실패하였습니다.");
        }

        log.info("[JWT] 리프레시 토큰 저장 완료. 카카오 고유 ID: {}", kakaoId);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        result.put("nickname", nickname);
        result.put("email", email);

        result.put("status", "SUCCESS");
        return result;
    }

    @Transactional
    public Map<String, String> reissueTokens(String refreshToken) {
        // 토큰 유효성 검사
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new ArvuBusinessException("리프레시 토큰이 만료되었습니다. 다시 로그인해주세요", HttpStatus.UNAUTHORIZED);
        }
        USR00000101IN commonParam = new USR00000101IN();
        commonParam.setUserId(tokenProvider.getUserId(refreshToken));

        USR00000101DTO user = daoUSR000001.selectRfrsTokn(commonParam);

        if (user == null) {
            throw new ArvuBusinessException("인증 정보에 해당하는 회원을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        int userId = user.getUserId();
        String dbRefreshToken = user.getRfrsTokn();
        if (dbRefreshToken == null || !dbRefreshToken.equals(refreshToken)) {
            log.warn("[Security Alert] 토큰 불일치 유저 ID: {}", commonParam.getUserId());
            throw new ArvuBusinessException("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }
        // 새로운 토큰 발급하여 반환
        String newAccessToken = tokenProvider.createAccessToken(userId, user.getNkNm());
        String newRefreshToken = tokenProvider.createRefreshToken(userId);

        commonParam.setRfrsTokn(newRefreshToken);
        int result = daoUSR000001.updateRfrsTokn(commonParam);

        if (result == 0) {
            throw new ArvuBusinessException("인증 정보 갱신에 실패했습니다;", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", newAccessToken);
        tokenMap.put("refreshToken", newRefreshToken);

        log.info("[JWT] 토큰 2종 세트 재발급 완료. userId: {}, nkNm: {}", userId, user.getNkNm());

        return tokenMap;
    }

    @Override
    @Transactional
    public void logout(int userId) {
        USR00000101IN updateParam = new USR00000101IN();
        updateParam.setUserId(userId);
        // 리프레쉬 토큰 값 NULL로 초기화
        updateParam.setRfrsTokn(null);

        int result = daoUSR000001.updateRfrsTokn(updateParam);
        if (result == 0) {
            log.warn("[Logout:Failure] 로그아웃 실패 - 존재하지 않는 사용자 ID: {}", userId);
            throw new ArvuBusinessException("로그아웃 처리 중 사용자를 찾을 수 없습니다.");
        }
        log.info("[Logout:Success] DB 리프레시 토큰 삭제 완료. userId: {}", userId);
    }

}
