package com.allreviewung.bch.dao;

import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import com.allreviewung.bch.service.svo.BCH00000102IN;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BCH000001DAO {

    /*
     * 다음 수집 대상 조회
     * @return BCH00000101DTO
     */
    BCH00000101DTO selectNextScrpTrgt();

    /*
     * 수집 대상 상태 변경
     * @param BCH00000102IN
     * @return int
     */
    int updateScrpTrgtStat(BCH00000101IN inParam);

    /*
     * 외부 리뷰 정보 등록
     * @param BCH00000102IN
     * @return int
     */
    int insertPlac(BCH00000102IN inParam);

}
