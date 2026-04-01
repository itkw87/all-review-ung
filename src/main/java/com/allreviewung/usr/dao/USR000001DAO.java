package com.allreviewung.usr.dao;

import com.allreviewung.usr.dto.USR00000101DTO;
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
     * 회원 조회
     * @param String
     * @return USR00000101DTO
     */
    USR00000101DTO selectUser(String strParam);

    /*
     * SNS 회원 조회
     * @param USR00000101IN
     * @return USR00000101DTO
     */
    USR00000101DTO selectSnsUser(USR00000101IN inParam);

    /*
     * 리프레시 토큰 변경
     * @param USR00000101IN
     * @return int
     */
    int updateRfrsTokn(USR00000101IN inParam);

    /*
     * 리프레시 토큰 조회
     * @param USR00000101IN
     * @return USR00000101DTO
     */
    USR00000101DTO selectRfrsTokn(USR00000101IN inParam);

}
