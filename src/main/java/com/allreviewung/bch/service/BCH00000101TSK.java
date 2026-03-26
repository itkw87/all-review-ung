package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BCH00000101TSK implements Tasklet {

    @Value("${public-data.api-key}")
    private String serviceKey;

    private final BCH000001DAO daoBCH000001;
    private final RestTemplate restTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int pageNo = 1;
        int totalInserted = 0;

        while (true) {
            // 1. URI 조립
            String url = "https://apis.data.go.kr/1741000/general_restaurants/info"
                    + "?serviceKey=" + serviceKey
                    + "&pageNo=" + pageNo
                    + "&numOfRows=100"
                    + "&returnType=json"
                    + "&cond[OPN_ATMY_GRP_CD::EQ]=3240000"
                    + "&cond[SALS_STTS_CD::EQ]=01";

            // 2. API 호출
            Map<String, Object> responseMap = restTemplate.getForObject(url, Map.class);

            // 3. 계단 내려가기 (꺼내고, 꺼내고, 또 꺼내기)
            Map<String, Object> response = (Map<String, Object>) responseMap.get("response");
            Map<String, Object> body = (Map<String, Object>) response.get("body");
            Map<String, Object> itemsWrapper = (Map<String, Object>) body.get("items");

            // 4. 진짜 데이터 리스트(item) 꺼내기
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsWrapper.get("item");

            // 5. null 체크
            if (items == null || items.isEmpty()) {
                log.info(">>> 더 이상 데이터가 없습니다. 수집 종료.");
                break;
            }

            // 3. DB 저장
            for (Map<String, Object> item : items) {
                // 관리번호
                String mngNo = (String) item.get("MNG_NO");
                // 카테고리명(업태명)
                String ctgrNm = (String) item.get("BZSTAT_SE_NM");
                // 가게명
                String pblcDataPlacNm = (String) item.get("BPLC_NM");

                // 도로명 주소
                String roadNmAddr = (String) item.get("ROAD_NM_ADDR");
                // 지번주소
                String lotnoAddr = (String) item.get("LOTNO_ADDR");
                // 주소 (도로명 주소 미존재시 지번주소 사용)
                String pblcDataAddr = roadNmAddr != null && !roadNmAddr.isEmpty() ? roadNmAddr : lotnoAddr;

                // 전화번호
                String pblcDataTelNo = (item.get("TELNO") != null) ? (String) item.get("TELNO") : "";

                // 위도
                BigDecimal pblcDataLttd = null;
                if (item.get("CRD_INFO_Y") != null && !String.valueOf(item.get("CRD_INFO_Y")).isEmpty()) {
                    pblcDataLttd = new BigDecimal(String.valueOf(item.get("CRD_INFO_Y")));
                }
                // 경도
                BigDecimal pblcDataLgtd = null;
                if (item.get("CRD_INFO_X") != null && !String.valueOf(item.get("CRD_INFO_X")).isEmpty()) {
                    pblcDataLgtd = new BigDecimal(String.valueOf(item.get("CRD_INFO_X")));
                }

                log.info(">>> bplcNm : {}, addr : {}", pblcDataPlacNm, pblcDataAddr);
                log.info(">>> item: {} \n", item);

                BCH00000101IN param = new BCH00000101IN();

                // 관리번호
                param.setPblcDataMngNo(mngNo);
                // 카테고리명(업태명)
                param.setCtgrNm(ctgrNm);
                // 장소명
                param.setPblcDataPlacNm(pblcDataPlacNm);
                // 주소
                param.setPblcDataAddr(pblcDataAddr);
                // 전화번호
                param.setPblcDataTelNo(pblcDataTelNo);
                // 위도
                param.setPblcDataLttd(pblcDataLttd);
                // 경도
                param.setPblcDataLgtd(pblcDataLgtd);

                // 수집 대상 테이블에 공공데이터 음식점 정보 적재
                daoBCH000001.insertScrpTrgt(param);
                totalInserted++;
            }

            log.info(">>> {} 페이지 수집 완료 (누적 {}건)", pageNo, totalInserted);

            if (items.size() < 100) break; // 마지막 페이지 체크
            pageNo++;
            Thread.sleep(500); // 서버 매너 타임
        }

        return RepeatStatus.FINISHED;
    }
}


