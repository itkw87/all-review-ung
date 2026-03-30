package com.allreviewung.usr.controller;

import com.allreviewung.usr.service.USR000001SVC;
import com.allreviewung.usr.vo.USR00000101IN;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class USR000001CTL {

    private final USR000001SVC svcUSR000001;

    @PostMapping("/join")
    public ResponseEntity<?> join(@Valid @RequestBody USR00000101IN inParam) {
        log.info("@PostMapping /join join - inParam: {}", inParam);

        int result = svcUSR000001.insertUser(inParam);

        if (result > 0) {
            // 성공 시 201 Created;
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공했습니다! 🦉");
        } else {
            // 로직상 실패 시 400 Bad Request;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원가입에 실패했습니다. 데이터를 확인해주시기 바랍니다.");
        }
    }

    @GetMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code) {
        log.info("[CTL] 카카오 로그인 시도 - code: {}", code);

        Map<String, Object> result = svcUSR000001.kakaoLogin(code);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        Map<String, String> result = svcUSR000001.reissueTokens(refreshToken);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            log.warn("[Logout:Unauthorized] 인증 정보 없이 로그아웃 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 정보가 없습니다.");
        }

        try {
            // 현재 로그인한 회원ID 가져오기
            int userId = Integer.parseInt(userDetails.getUsername());
            // 로그아웃 처리
            svcUSR000001.logout(userId);
            return ResponseEntity.ok("로그아웃 성공");
        } catch (Exception e) {
            log.error("[Logout:Error] 로그아웃 중 예상치 못한 에러 발생: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그아웃 처리 중 서버 에러가 발생했습니다.");
        }
    }
}
