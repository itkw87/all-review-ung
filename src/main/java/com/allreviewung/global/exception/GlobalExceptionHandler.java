package com.allreviewung.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리 핸들러
     */
    @ExceptionHandler(ArvuBusinessException.class)
    public ResponseEntity<?> handleArvuBusinessException(ArvuBusinessException e) {
        log.warn("[Business Warning] : {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAIL");
        response.put("message", e.getMessage());

        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }

    /**
     * 그 외 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("[Critical System Error] : ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
