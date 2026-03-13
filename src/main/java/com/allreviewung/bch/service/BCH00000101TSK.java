package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import com.allreviewung.bch.service.svo.BCH00000102IN;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BCH00000101TSK implements Tasklet {

    private final BCH000001DAO daoBCH000001;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        // 1. 전담 정비사 호출 (드라이버 자동 세팅)
        WebDriverManager.chromedriver().setup();

        // 2. 크롬 옵션 설정 (보안 및 네트워크 설정)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");

        // 3. 운전사(Driver) 대동해서 크롬 브라우저 실행!
        WebDriver driver = new ChromeDriver(options);

        try {

            System.out.println(">>> 셀레니움 크롤링 시작합니다!");

            BCH00000101DTO scrpTrgtDto = daoBCH000001.selectNextScrpTrgt();

            if (scrpTrgtDto == null) {
                System.out.println(">>> 더 이상 수집할 대상이 없습니다.: ");
                return RepeatStatus.FINISHED;
            }

            BCH00000101IN updateParam = new BCH00000101IN();
            updateParam.setScrpTrgtId(scrpTrgtDto.getScrpTrgtId());
            // 진행상태 변경: 대기(00) -> 진행(01)
            updateParam.setProgStatCd("01");
            daoBCH000001.updateScrpTrgtStat(updateParam);

            // 네이버 지도로 이동
            driver.get("https://map.naver.com/");

            // 검색창이 화면에 나타날 때까지 최대 10초간 대기(빈화면 긁음 방지)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));

            searchInput.sendKeys(scrpTrgtDto.getSrchKwd());
            searchInput.sendKeys(Keys.ENTER);

            // [핵심] 검색 결과 Iframe으로 전환
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchIframe")));
            driver.switchTo().frame("searchIframe");

            // 장소 리스트 긁기
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.UEzoS")));
            List<WebElement> placList = driver.findElements(By.cssSelector("li.UEzoS"));

            System.out.println(">>> [" + scrpTrgtDto.getSrchKwd() + "] 검색 결과: " + placList.size() + "건");

            int idx = 0;
            for (WebElement plac : placList) {
                try {
                    BCH00000102IN revwParam = new BCH00000102IN();

                    // 가게 이름 (무조건 있다고 믿고 바로 findElement)
                    String placeNm = plac.findElement(By.cssSelector(".TYaxT")).getText();

                    // ID 추출
                    String realNaverId = "";
                    try {
                        WebElement linkElement = plac.findElement(By.cssSelector("a.YTJkH"));
                        String href = linkElement.getAttribute("href");

                        if (href != null && href.contains("/place/")) {
                            // 정상적인 상세페이지 주소인 경우 숫자 ID 추출
                            realNaverId = href.split("/place/")[1].split("\\?")[0];
                        } else {
                            // 만약 링크가 리스트 주소라면, 다른 속성(id 등)을 찾아야 합니다.
                            // 네이버는 가끔 data-id 같은 속성에 ID를 넣어두기도 합니다.
                            realNaverId = linkElement.getAttribute("data-id");
                        }
                    } catch (Exception e) {
                        System.err.println(">>> ID 추출 실패: " + e.getMessage());
                    }
                    // 값 세팅
                    revwParam.setExtlPlacId(realNaverId);
                    revwParam.setPlacNm(placeNm);
                    revwParam.setAddr("");
                    revwParam.setSorcDvcd("NAV");
                    revwParam.setCtgrDvcd("RESTAURANT");
                    revwParam.setLttd(0.0);
                    revwParam.setLgtd(0.0);

                    // 4. DB 저장
                    daoBCH000001.insertExtlRevw(revwParam);

                    System.out.println(">>> [성공] ID: " + realNaverId + " | 가게명: " + placeNm);

                } catch (Exception e) {
                    System.err.println(">>> 데이터 세팅 중 오류: " + e.getMessage());
                }
            }

            // 진행상태 변경: 진행(01) -> 완료(02)
            updateParam.setProgStatCd("02");
            daoBCH000001.updateScrpTrgtStat(updateParam);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                // 크롤링 후 빈화면 닫기(크롬창 다중 실행 방지)
                driver.quit();
            }
        }

        // 작업 완료 신호
        return RepeatStatus.FINISHED;
    }
}