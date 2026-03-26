package com.allreviewung.usr.dao;

import com.allreviewung.usr.vo.USR00000101IN;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface USR000001DAO {

    /*
     * 이메일 중복 확인
     * @param USR00000101IN
     * @return int
     */
    int selectEmilDupChk(String strParam);

    /*
     * 이메일 닉네임 중복 확인
     * @param USR00000101IN
     * @return int
     */
    int selectNkNmDupChk(String strParam);

    /*
     * 회원 등록
     * @param USR00000101IN
     * @return int
     */
    int insertUser(USR00000101IN inParam);

    /*
     * SNS ID 중복 확인
     * @param USR00000101IN
     * @return int
     */
    int selectSnsIdDupChk(USR00000101IN inParam);

}
