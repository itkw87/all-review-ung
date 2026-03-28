package com.allreviewung.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ArvuBusinessException extends RuntimeException {

    private final HttpStatus httpStatus;

    public ArvuBusinessException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public ArvuBusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

}
