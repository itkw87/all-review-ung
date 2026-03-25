package com.allreviewung.usr.controller;

import com.allreviewung.usr.service.USR000001SVC;
import com.allreviewung.usr.vo.USR00000101IN;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
