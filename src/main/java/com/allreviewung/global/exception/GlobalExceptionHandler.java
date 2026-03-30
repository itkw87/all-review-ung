package com.allreviewung.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. [Valid 검증 오류] 처리 (비밀번호 형식, 필수값 누락 등)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("[Validation Warning] : {}", msg);
        return buildResponse("FAIL", msg, HttpStatus.BAD_REQUEST);
    }

    /**
     * 2. [ArvuBusinessException] 처리
     */
    @ExceptionHandler(ArvuBusinessException.class)
    public ResponseEntity<?> handleArvuBusinessException(ArvuBusinessException e) {
        log.warn("[Business Warning] : {}", e.getMessage());
        return buildResponse("FAIL", e.getMessage(), e.getHttpStatus());
    }

    /**
     * 3. [그 외 모든 Exception] 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("[Critical System Error] : ", e);
        return buildResponse("ERROR", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 응답 객체를 생성하는 공통 메소드
     */
    private ResponseEntity<?> buildResponse(String status, String message, HttpStatus httpStatus) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        return ResponseEntity.status(httpStatus).body(response);
    }

}
