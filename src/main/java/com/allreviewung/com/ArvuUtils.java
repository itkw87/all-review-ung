package com.allreviewung.com;

import com.allreviewung.bch.dao.BCH000001DAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArvuUtils {

    private final BCH000001DAO daoBCH000001; // 주입

    /**
     * 상태 업데이트 메서드
     */
//    private int updateStatus(String id, String statCd) {
//        BCH00000201IN param = new BCH00000201IN();
//        param.setScrpTrgtId(id);
//        param.setProgStatCd(statCd);
//        return daoBCH000001.updateScrpTrgtStat(param);
//    }

}
