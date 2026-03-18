package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BCH00000102TSK implements Tasklet {

    private final BCH00000102NAV navBCH00000101;

    private final BCH00000102KKO kkoBCH00000101;

    private final BCH000001DAO daoBCH000001;
    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트", "커피");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        WebDriver driver = null;
        while (true) {
            BCH00000101DTO scrpTrgtDto = daoBCH000001.selectNextScrpTrgt();

            if (scrpTrgtDto == null) {
                log.info(">>> [모두 완료] 더 이상 수집할 대상이 없습니다.");
                break;
            }

            try {
                log.info(">>> [{}] 전체 통합 수집을 시작합니다.", scrpTrgtDto.getSrchKwd());

                // 상태 업데이트: 대기(00) -> 진행(01)
                this.updateStatus(scrpTrgtDto.getScrpTrgtId(), "01");

                // 드라이버 세팅 (키워드마다 새로 켜서 메모리 누수 방지)

                // WebDriverManager 사용하여 크롬 드라이버 자동 세팅
                WebDriverManager.chromedriver().setup();
                // 크롬 옵션 설정 (보안 및 네트워크 설정)
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(options);

                // 네이버맵 리뷰 수집
//                navBCH00000101.collect(driver, scrpTrgtDto);

                // 카카오맵 리뷰 수집
                kkoBCH00000101.collect(driver, scrpTrgtDto);

                // 진행상태 변경: 진행(01) -> 완료(02)
                this.updateStatus(scrpTrgtDto.getScrpTrgtId(), "02");
                log.info(">>> [" + scrpTrgtDto.getSrchKwd() + "] 수집 완료!");
            } catch (Exception e) {
                log.error(">>> [" + scrpTrgtDto.getSrchKwd() + "] 처리 중 에러 발생: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (driver != null) {
                    // 한 키워드에 대한 크롤링 끝나면 브라우저 닫기
                    driver.quit();
                }
            }
        }
        // 작업 완료 신호
        return RepeatStatus.FINISHED;
    }

    /**
     * 상태 업데이트 공통 메서드
     */
    private void updateStatus(String id, String statCd) {
        BCH00000101IN param = new BCH00000101IN();
        param.setScrpTrgtId(id);
        param.setProgStatCd(statCd);
        daoBCH000001.updateScrpTrgtStat(param);
    }
}