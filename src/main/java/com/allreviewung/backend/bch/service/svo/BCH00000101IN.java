package com.allreviewung.backend.bch.service.svo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BCH00000101IN {

  private long extlRevwId;   // 외부리뷰ID
  private String sorcDvcd;     // 출처구분코드
  private String cntn;         // 내용
  private Double rtng;         // 별점 (숫자니까 Integer 추천)
  private int placId;       // 장소ID
  private String fileGrpId;    // 파일그룹ID
  private String regId;        // 등록자ID
  private String regDtm;       // 등록일시
  private String chngId;       // 수정자ID
  private String chngDtm;      // 수정일시

}
