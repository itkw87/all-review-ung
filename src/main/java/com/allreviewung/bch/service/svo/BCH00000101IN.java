package com.allreviewung.bch.service.svo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BCH00000101IN {

    private String pblcDataMngNo;       // 공공데이터 관리번호
    private String srchKwd;             // 검색키워드
    private String pblcDataPlacNm;      // 공공데이터 가게명
    private String pblcDataAddr;        // 공공데이터 주소
    private String pblcDataTelNo;       // 공공데이터 전화번호
    private BigDecimal pblcDataLttd;    // 공공데이터 위도
    private BigDecimal pblcDataLgtd;    // 공공데이터 경도
    private String progStatCd;          // 수집상태(00:전체대기, 01:네이버진행중, 02:네이버완료, 03:카카오진행, 04:전체완료)

}
