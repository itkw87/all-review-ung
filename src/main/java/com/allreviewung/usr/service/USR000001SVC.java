package com.allreviewung.usr.service;

import com.allreviewung.usr.vo.USR00000101IN;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface USR000001SVC {

    /*
     * 회원 등록
     * @param USR00000101IN
     * @return int
     */
    int insertUser(USR00000101IN inParam);

    /*
     * 로그인
     * @param USR00000101IN
     * @return Map<String, Object>
     */
    Map<String, String> login(USR00000101IN inParam);

    /*
     * 카카오 로그인
     * @param String
     * @return Map<String, Object>
     */
    Map<String, String> kakaoLogin(String strParam);

    /*
     * 토큰 재발행
     * @param String
     * @Map<String, String>
     */
    Map<String, String> reissueTokens(String refreshToken);

    /*
     * 로그아웃
     * @param int
     * @void
     */
    void logout(int userId);
}
