package com.allreviewung.bch.dao;

import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import com.allreviewung.bch.service.svo.BCH00000201IN;
import com.allreviewung.bch.service.svo.BCH00000202IN;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BCH000001DAO {

    /*
     * 수집 대상 정보 등록
     * @param BCH00000101IN
     * @return int
     */
    int insertScrpTrgt(BCH00000101IN inParam);

    /*
     * 다음 수집 대상 조회
     * @return List<BCH00000101DTO>
     */
    List<BCH00000101DTO> selectScrpTrgtList();

    /*
     * 수집 대상 상태 변경
     * @param BCH00000202IN
     * @return int
     */
    int updateScrpTrgtStat(BCH00000201IN inParam);

    /*
     * 외부 리뷰 정보 등록
     * @param BCH00000202IN
     * @return int
     */
    int insertPlac(BCH00000202IN inParam);

}
