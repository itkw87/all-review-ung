package com.allreviewung.usr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class KakaoTokenDTO {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("refresh_token")
    private String refreshToken;            // 엑세스 초큰 갱신용 토큰
    @JsonProperty("expires_in")
    private int expiresIn;                  // 엑세스 토큰 만료 시간(초 단위)
    @JsonProperty("scope")
    private String scope;                    // 인증된 사용자의 정보 조회 권한 범위
    @JsonProperty("refresh_token_expires_in")
    private int refreshTokenExpiresIn;    // 리프레시 토큰 만료 시간



}
