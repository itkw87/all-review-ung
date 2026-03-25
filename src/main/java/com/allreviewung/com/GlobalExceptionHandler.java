package com.allreviewung.com;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. [Valid 검증 오류]를 잡는 곳 (비밀번호 8자 미만 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return buildResponse(msg, HttpStatus.BAD_REQUEST);
    }

    // 2. [기존 RuntimeException]을 잡는 곳 (서비스에서 던진 중복 체크 등)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        return buildResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 3. [그 외 모든 Exception]을 잡는 곳 (서버 터졌을 때)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllException(Exception e) {
        return buildResponse("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요;", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 에러 메세지 Map에 담아도록 처리하는 메소드
    private ResponseEntity<?> buildResponse(String message, HttpStatus status) {
        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("message", message);
        return ResponseEntity.status(status).body(errorMap);
    }

}
