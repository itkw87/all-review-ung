package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BCH00000101TSK implements Tasklet {

    @Value("${public-data.api-key}")
    private String serviceKey;

    private final BCH000001DAO daoBCH000001;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        int pageNo = 1;
        int totalInserted = 0;

        while (true) {
            // 1. URI 조립
            URI uri = UriComponentsBuilder.fromHttpUrl("https://apis.data.go.kr/1741000/general_restaurants/info")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", 100)
                    .queryParam("returnType", "json")
                    .queryParam("cond[OPN_ATMY_GRP_CD::EQ]", "3240000")
                    .queryParam("cond[ROAD_NM_ADDR::LIKE]", "강일동")
                    .build(true).toUri();

            // 2. 호출 및 파싱 (간단하게 Map으로 받기)
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            // API 응답 구조에 따라 items 꺼내기 (JSON 구조 확인 필요)
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            if (items == null || items.isEmpty()) {
                log.info(">>> 수집할 데이터가 더 이상 없습니다.");
                break;
            }

            // 3. DB 저장
            for (Map<String, Object> item : items) {
                String bplcNm = (String) item.get("BPLC_NM");
                String addr = (String) item.get("ROAD_NM_ADDR");

                // 검색 키워드: "강일동 가게명" (지도가 잘 알아먹게 조합)
                String searchKwd = "강일동 " + bplcNm;

                // DAO 호출 (TB_ARVU_SCRP_TRGT_L에 Insert)
//                daoBCH000001.insertScrpTrgt(searchKwd);
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
