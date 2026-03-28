package com.allreviewung.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidTime;
    private final long refreshTokenValidTime;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidSeconds,
                            @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidSeconds
                            ) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 만료 시간 설정 (초 -> 밀리초 변환)
        this.accessTokenValidTime = accessTokenValidSeconds * 1000;
        this.refreshTokenValidTime = refreshTokenValidSeconds * 1000;

        log.info("[JWT] JwtTokenProvider 초기화 완료.");
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(String snsId, String nickname) {
        Claims claims = Jwts.claims().setSubject(snsId);
        claims.put("nickname", nickname);

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)    // 데이터 담기
                .setIssuedAt(now)     // 발행 시간
                .setExpiration(new Date(now.getTime() + accessTokenValidTime))    // 만료 시간
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();    // 직렬화
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String snsId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(snsId)    // 토큰의 주인 구별을 위한 식별자
                .setIssuedAt(now)     // 발행 시간
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))    // 만료 시간
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();    // 직렬화
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e ){
            log.error("[JWT] 만료된 토큰입니다.");
        } catch (Exception e) {
            log.error("[JWT] 유효하지 않은 토큰입니다. (원인: {})", e.getMessage());
        }
        return false;
    }

    /**
     *  토큰에서 SND_ID 꺼냬기
     */
    public String getSnsId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     *  토큰에서 SND_ID 꺼냬기
     */
    public String getNickname(String token) {
        return (String)Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("nickname");
    }

    /**
     *  토큰 인증 정보(Authentication) 조회
     */
    public Authentication getAuthentication(String token) {
        String snsId = this.getSnsId(token);

        UserDetails userDetails = new User(snsId, "", Collections.emptyList());

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
    
}
