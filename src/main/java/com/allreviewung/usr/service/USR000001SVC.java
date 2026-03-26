package com.allreviewung.usr.service;

import com.allreviewung.usr.vo.USR00000101IN;

import java.util.Map;

public interface USR000001SVC {
    int insertUser(USR00000101IN inParam);

    Map<String, Object> kakaoLogin(String strParam);
}
