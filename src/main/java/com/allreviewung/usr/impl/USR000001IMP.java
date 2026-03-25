package com.allreviewung.usr.impl;

import com.allreviewung.usr.dao.USR000001DAO;
import com.allreviewung.usr.service.USR000001SVC;
import com.allreviewung.usr.vo.USR00000101IN;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class USR000001IMP implements USR000001SVC {

    private final USR000001DAO daoUSR000001;

    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public int insertUser(USR00000101IN inParam) {
        log.info("[IMP] 회원가입 처리 시작 - 데이터: {}", inParam);

        if (daoUSR000001.selectEmilDupChk(inParam.getEmil()) > 0) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        if (daoUSR000001.selectNkNmDupChk(inParam.getNkNm()) > 0) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(inParam.getPswd());
        inParam.setPswd(encodedPassword);

        // 회원 등록(회원 가입)
        int result = daoUSR000001.insertUser(inParam);

        log.info("[IMP] DB 처리 결과 (행 개수): {}", result);
        return result;
    }
}
