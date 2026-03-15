package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import com.allreviewung.bch.service.svo.BCH00000102IN;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BCH00000101TSK implements Tasklet {

    private final BCH000001DAO daoBCH000001;

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
                log.info(">>> [" + scrpTrgtDto.getSrchKwd() + "] 수집을 시작합니다.");

                BCH00000101IN updateParam = new BCH00000101IN();
                updateParam.setScrpTrgtId(scrpTrgtDto.getScrpTrgtId());
                // 상태 업데이트: 대기(00) -> 진행(01)
                updateParam.setProgStatCd("01");
                daoBCH000001.updateScrpTrgtStat(updateParam);

                // 드라이버 세팅 (키워드마다 새로 켜서 메모리 누수 방지)

                // WebDriverManager 사용하여 크롬 드라이버 자동 세팅
                WebDriverManager.chromedriver().setup();
                // 크롬 옵션 설정 (보안 및 네트워크 설정)
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--remote-allow-origins=*");
                driver = new ChromeDriver(options);

                // 네이버 지도로 이동
                driver.get("https://map.naver.com/");

                // 검색창이 화면에 나타날 때까지 최대 10초간 대기(빈화면 긁음 방지)
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));
                searchInput.sendKeys(scrpTrgtDto.getSrchKwd());
                searchInput.sendKeys(Keys.ENTER);

                // [루프 시작] 페이지가 없을 때까지 반복
                boolean hasNext = true;
                while (hasNext) {
                    // 루프 시작시 브라우저 메인 화면으로 이동
                    driver.switchTo().defaultContent();
                    // 브라우저 메인 화면에서 searchIframe 생성시 까지 대기
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchIframe")));
                    // searchIframe으로 이동
                    driver.switchTo().frame("searchIframe");

                    // 첫 번째 장소 로딩될 때까지 잠시 대기
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.UEzoS")));

                    // 스크랩 대상을 전부 브라우저에 뿌리기 위해 스크롤 내리기 작업 수행
                    log.info(">>> 검색 결과 전체 로딩을 위해 스크롤을 내립니다...");
                    WebElement scrollContainer = driver.findElement(By.cssSelector("#_pcmap_list_scroll_container"));
                    long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", scrollContainer);

                    while (true) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTo(0, arguments[0].scrollHeight)", scrollContainer);
                        Thread.sleep(1500); // 네이버 서버가 데이터를 줄 시간을 줌
                        long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", scrollContainer);
                        if (newHeight == lastHeight) break;
                        lastHeight = newHeight;
                    }

                    // 스크롤 수행 작업 종료후 리스트 확정
                    List<WebElement> placList = driver.findElements(By.cssSelector("li.UEzoS"));
                    log.info(">>> [스크롤 완료] 최종 검색 결과: " + placList.size() + "건");

                    for (WebElement plac : placList) {
                        try {
                            BCH00000102IN insertParam = new BCH00000102IN();

//                            // 가게 이름 먼저 추출
                            WebElement titleEl = plac.findElement(By.cssSelector(".TYaxT"));
//                            String placNm = titleEl.getText();

                            String placNm = "";       // 가게명
                            String extlPlacId = "";   // 외부장소ID
                            String addr = "";
                            String telNo = "";
                            try {
                                try {
                                    // 클릭 전 해당 요소가 화면에 보이도록 스크롤 (안전장치)
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", titleEl);
                                    Thread.sleep(500); // 스크롤 후 안정화 대기

                                    // 자바스크립트로 강제 클릭!
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", titleEl);
                                } catch (Exception e) {
                                    // 만약 이것도 안 되면 일반 클릭으로 재시도
                                    titleEl.click();
                                }

                                // 클릭 후 상세 페이지 주소가 반영될 때까지 아주 잠깐 대기 (1초)
                                Thread.sleep(1000);

                                // [핵심] 프레임 이동: 리스트 프레임(searchIframe)에서 빠져나와 상세 프레임으로!
                                driver.switchTo().defaultContent();
                                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));
                                driver.switchTo().frame("entryIframe");

                                // ------------------------------------------------------------------
                                // 주소(.pz7wy) 글자가 실제로 '눈에 보일 때까지' 기다리기 (최대 5초)
                                // ------------------------------------------------------------------
                                try {
                                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".pz7wy")));
                                } catch (Exception e) {
                                    log.info(">>> [" + placNm + "] 주소 로딩 시간이 너무 깁니다. 패스!");
                                }

                                // 현재 브라우저의 URL을 가져옴 (생략 가능하나 ID 추출 위해 유지)
                                String currentUrl = driver.getCurrentUrl();
                                if (currentUrl.contains("/place/")) {
                                    String[] parts = currentUrl.split("/place/");
                                    if (parts.length > 1) {
                                        extlPlacId = parts[1].split("/|\\?")[0];
                                    }
                                }
                                // ------------------------------------------------------------------
                                // 상세 정보(장소명, 주소, 전화번호) 긁기 + 데이터 청소
                                // ------------------------------------------------------------------
                                List<WebElement> placNmEls = driver.findElements(By.cssSelector(".GHAhO"));
                                if (!placNmEls.isEmpty()) {
                                    placNm = placNmEls.get(0).getText().trim();
                                }

                                List<WebElement> addrEls = driver.findElements(By.cssSelector(".pz7wy"));
                                if (!addrEls.isEmpty()) {
                                    addr = addrEls.get(0).getText().trim();
                                }

                                List<WebElement> telEls = driver.findElements(By.cssSelector(".xlx7Q"));
                                if (!telEls.isEmpty()) {
                                    telNo = telEls.get(0).getText().trim();
                                }

                                // [필수] 다음 가게를 찾기 위해 다시 리스트 프레임(searchIframe)으로 복귀
                                driver.switchTo().defaultContent();
                                driver.switchTo().frame("searchIframe");
                            } catch (Exception e) {
                                log.error(">>> 상세 정보 수집 실패: " + placNm + " | " + e.getMessage());
                                // 에러가 나도 다음 루프를 위해 리스트 프레임으로 복귀 시도
                                driver.switchTo().defaultContent();
                                driver.switchTo().frame("searchIframe");
                            }

                            // ID가 없으면 저장하지 않고 스킵
                            if (extlPlacId == null || extlPlacId.isEmpty()) {
                                log.error(">>> [" + placNm + "] ID를  찾을 수 없어 건너뜁니다.");
                                continue;
                            }

                            if (addr == null || addr.isEmpty()) {
                                log.error(">>> [" + placNm + "] 주소를  찾을 수 없어 건너뜁니다.");
                                continue;
                            }
                            // 데이터 세팅 및 DB 저장
                            insertParam.setExtlPlacId(extlPlacId);
                            insertParam.setPlacNm(placNm);
                            insertParam.setAddr(addr);
                            insertParam.setTelNo(telNo);
                            insertParam.setSorcDvcd("NAV");
                            insertParam.setCtgrDvcd("RST");

                            try {
                                daoBCH000001.insertPlac(insertParam);
                                log.info(">>> [성공] ID: " + extlPlacId + " | 가게명: " + placNm);
                            } catch (DuplicateKeyException e) {
                                log.info(">>> [중복] 이미 수집된 가게입니다: " + placNm);
                            }

                        } catch (Exception e) {
                            log.error(">>> 루프 내부 오류 발생: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    try {
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame("searchIframe");

                        // '이전/다음' 버튼 역할을 하는 .eUTV2 클래스들을 모두 찾음
                        List<WebElement> pageBtns = driver.findElements(By.cssSelector(".eUTV2"));

                        // 리스트의 마지막 버튼이 '다음페이지' 버튼임
                        if (!pageBtns.isEmpty()) {
                            WebElement nextBtn = pageBtns.get(pageBtns.size() - 1);
                            String isDisabled = nextBtn.getAttribute("aria-disabled");

                            // 비활성화 상태가 아니면(false) 클릭해서 다음 페이지로!
                            if ("false".equals(isDisabled)) {
                                log.info(">>> 다음 페이지 버튼을 발견했습니다. 클릭합니다.");
                                nextBtn.click();

                                // 페이지가 완전히 로딩될 때까지 2~3초 넉넉히 대기
                                Thread.sleep(2500);
                            } else {
                                log.info(">>> [알림] 마지막 페이지입니다. 수집을 종료합니다.");
                                hasNext = false;
                            }
                        } else {
                            log.info(">>> [알림] 페이지 버튼을 찾을 수 없어 종료합니다.");
                            hasNext = false;
                        }

                    } catch (Exception e) {
                        log.error(">>> 페이지 이동 중 오류 발생: " + e.getMessage());
                        hasNext = false;
                    }
                }

                // 진행상태 변경: 진행(01) -> 완료(02)
                updateParam.setProgStatCd("02");
                daoBCH000001.updateScrpTrgtStat(updateParam);
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
}