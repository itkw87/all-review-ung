package com.allreviewung.usr.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class USR00000101DTO {
    
    private int userId;         // 회원ID(시퀀스)
    private String nkNm;        // 닉네임
    private String rfrsTokn;    // refreshToken
    
}
