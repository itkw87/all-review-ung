package com.allreviewung.bch.service.svo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BCH00000202IN {

    private int placId;           // 장소ID
    private String placNm;        // 장소명
    private String extlPlacId;    // 외부장소ID
    private String sorcDvcd;      // 출처구분코드
    private String ctgrDvcd;      // 카테고리구분코드
    private String addr;          // 주소
    private String telNo;         // 전화번호
    private double lttd;          // 위도
    private double lgtd;          // 경도
    
}
