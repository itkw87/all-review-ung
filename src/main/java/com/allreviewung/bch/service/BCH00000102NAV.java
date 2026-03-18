package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000102IN;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BCH00000102NAV {

    private final BCH000001DAO daoBCH000001;
    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트", "커피");

    public void collect(WebDriver webDriver, BCH00000101DTO scrpTrgtDto) {

        try {
            // 네이버 지도로 이동
            webDriver.get("https://map.naver.com/");

            // 검색창이 화면에 나타날 때까지 최대 10초간 대기(빈화면 긁음 방지)
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));
            searchInput.sendKeys(scrpTrgtDto.getSrchKwd());
            searchInput.sendKeys(Keys.ENTER);

            // [루프 시작] 페이지가 없을 때까지 반복
            boolean hasNext = true;
            while (hasNext) {
                // 루프 시작시 브라우저 메인 화면으로 이동
                webDriver.switchTo().defaultContent();
                // 브라우저 메인 화면에서 searchIframe 생성시 까지 대기
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchIframe")));
                // searchIframe으로 이동
                webDriver.switchTo().frame("searchIframe");

                // 첫 번째 장소 로딩될 때까지 잠시 대기
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.UEzoS")));

                // 스크랩 대상을 전부 브라우저에 뿌리기 위해 스크롤 내리기 작업 수행
                log.info(">>> 검색 결과 전체 로딩을 위해 스크롤을 내립니다...");
                WebElement scrollContainer = webDriver.findElement(By.cssSelector("#_pcmap_list_scroll_container"));
                long lastHeight = (long) ((JavascriptExecutor) webDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);

                while (true) {
                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollTo(0, arguments[0].scrollHeight)", scrollContainer);
                    Thread.sleep(1500); // 네이버 서버가 데이터를 줄 시간을 줌
                    long newHeight = (long) ((JavascriptExecutor) webDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);
                    if (newHeight == lastHeight) break;
                    lastHeight = newHeight;
                }

                // 스크롤 수행 작업 종료후 리스트 확정
                List<WebElement> placList = webDriver.findElements(By.cssSelector("li.UEzoS"));
                log.info(">>> [스크롤 완료] 현재페이지 검색결과: " + placList.size() + "건");

                for (WebElement plac : placList) {
                    try {
                        BCH00000102IN insertParam = new BCH00000102IN();

                        WebElement titleEl = plac.findElement(By.cssSelector(".TYaxT"));
                        String title = titleEl.getText();
                        String category = plac.findElement(By.cssSelector(".KCMnt")).getText();

                        if (EXCLUDE_CATEGORIES.stream().anyMatch(word -> category.contains(word))) {
                            log.info(">>> [제외업종] {}은(는) {} 업종이라 제외합니다.", title, category);
                            continue;
                        }

                        String placNm = "";       // 가게명
                        String extlPlacId = "";   // 외부장소ID
                        String addr = "";
                        String telNo = "";
                        try {
                            try {
                                // 클릭 전 해당 요소가 화면에 보이도록 스크롤 (안전장치)
                                ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollIntoView(true);", titleEl);
                                Thread.sleep(500); // 스크롤 후 안정화 대기

                                // 자바스크립트로 강제 클릭!
                                ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", titleEl);
                            } catch (Exception e) {
                                // 만약 이것도 안 되면 일반 클릭으로 재시도
                                titleEl.click();
                            }

                            // 클릭 후 상세 페이지 주소가 반영될 때까지 아주 잠깐 대기 (1초)
                            Thread.sleep(1000);

                            // [핵심] 프레임 이동: 리스트 프레임(searchIframe)에서 빠져나와 상세 프레임으로!
                            webDriver.switchTo().defaultContent();
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));
                            webDriver.switchTo().frame("entryIframe");

                            // ------------------------------------------------------------------
                            // 주소(.pz7wy) 글자가 실제로 '눈에 보일 때까지' 기다리기 (최대 5초)
                            // ------------------------------------------------------------------
                            try {
                                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".pz7wy")));
                            } catch (Exception e) {
                                log.info(">>> [" + placNm + "] 주소 로딩 시간이 너무 깁니다. 패스!");
                            }

                            // 현재 브라우저의 URL을 가져옴 (생략 가능하나 ID 추출 위해 유지)
                            String currentUrl = webDriver.getCurrentUrl();
                            if (currentUrl.contains("/place/")) {
                                String[] parts = currentUrl.split("/place/");
                                if (parts.length > 1) {
                                    extlPlacId = parts[1].split("/|\\?")[0];
                                }
                            }
                            // ------------------------------------------------------------------
                            // 상세 정보(장소명, 주소, 전화번호) 긁기 + 데이터 청소
                            // ------------------------------------------------------------------
                            List<WebElement> placNmEls = webDriver.findElements(By.cssSelector(".GHAhO"));
                            if (!placNmEls.isEmpty()) {
                                placNm = placNmEls.get(0).getText().trim();
                            }

                            List<WebElement> addrEls = webDriver.findElements(By.cssSelector(".pz7wy"));
                            if (!addrEls.isEmpty()) {
                                addr = addrEls.get(0).getText().trim();
                            }

                            List<WebElement> telEls = webDriver.findElements(By.cssSelector(".xlx7Q"));
                            if (!telEls.isEmpty()) {
                                telNo = telEls.get(0).getText().trim();
                            }

                            // [필수] 다음 가게를 찾기 위해 다시 리스트 프레임(searchIframe)으로 복귀
                            webDriver.switchTo().defaultContent();
                            webDriver.switchTo().frame("searchIframe");
                        } catch (Exception e) {
                            log.error(">>> 상세 정보 수집 실패: " + placNm + " | " + e.getMessage());
                            // 에러가 나도 다음 루프를 위해 리스트 프레임으로 복귀 시도
                            webDriver.switchTo().defaultContent();
                            webDriver.switchTo().frame("searchIframe");
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
                    webDriver.switchTo().defaultContent();
                    webDriver.switchTo().frame("searchIframe");

                    // '이전/다음' 버튼 역할을 하는 .eUTV2 클래스들을 모두 찾음
                    List<WebElement> pageBtns = webDriver.findElements(By.cssSelector(".eUTV2"));

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
        } catch (Exception e) {
            log.error(">>> [NAV 리뷰 수집중 오류 발생] 키워드: {} | 에러내용: {}", scrpTrgtDto.getSrchKwd(), e.getMessage());
        }


    }
}
