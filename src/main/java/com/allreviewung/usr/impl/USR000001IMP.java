package com.allreviewung.usr.impl;

import com.allreviewung.global.exception.ArvuBusinessException;
import com.allreviewung.security.JwtTokenProvider;
import com.allreviewung.usr.dao.USR000001DAO;
import com.allreviewung.usr.dao.USR000001DTO;
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
    public Map<String, Object> kakaoLogin(String code) {
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

        String kakaoId = String.valueOf(kakaoUser.getId());
        String email = kakaoId + "@kakao.user";    // 카카오 사용자 정보에서 이메일 정보를 주지 않으므로 해당 형식으로 임시 사용
        String nickname = "";

        USR00000101IN commonParam = new USR00000101IN();

        commonParam.setSnsId(kakaoId);
        commonParam.setSnsDvcd("KKO");

        USR000001DTO user = daoUSR000001.selectUser(commonParam);

        if (user != null) {
            commonParam.setUserId(user.getUserId());
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
        } // else
        // 토큰 생성
        String accessToken = tokenProvider.createAccessToken(kakaoId, nickname);
        String refreshToken = tokenProvider.createRefreshToken(kakaoId);

        // 리프레시 토큰 DB 저장
        commonParam.setRfrsTokn(refreshToken);
        int updateCount = daoUSR000001.updateRfrsTokn(commonParam);

        if (updateCount == 0) {
            log.error("[JWT] 리프레시 토큰 저장 실패! 카카오 고유 ID: {}", kakaoId);
            throw new ArvuBusinessException("토큰 발급에 실패하였습니다.");
        }

        log.info("[JWT] 리프레시 토큰 저장 완료. 카카오 고유 ID: {}", kakaoId);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);

        result.put("snsId", kakaoId);
        result.put("nickname", nickname);
        result.put("email", email);

        result.put("status", "SUCCESS");
        return result;
    }
}
