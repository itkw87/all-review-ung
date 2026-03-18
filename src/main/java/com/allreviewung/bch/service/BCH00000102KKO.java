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
public class BCH00000102KKO {

    private final BCH000001DAO daoBCH000001;
    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트카페", "커피전문점", "간식", "제과,베이커리");

    public void collect(WebDriver webDriver, BCH00000101DTO scrpTrgtDto) {

        try {
            // 카카오 지도로 이동
            webDriver.get("https://map.kakao.com/");

            // 검색창이 화면에 나타날 때까지 최대 10초간 대기(빈화면 긁음 방지)
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id='search.keyword.query']")));
            searchInput.sendKeys(scrpTrgtDto.getSrchKwd());
            searchInput.sendKeys(Keys.ENTER);

            int currentPage = 1;
            // [루프 시작] 페이지가 없을 때까지 반복
            boolean hasNext = true;
            while (hasNext) {
//                // 루프 시작시 브라우저 메인 화면으로 이동
//                webDriver.switchTo().defaultContent();
//                // 브라우저 메인 화면에서 searchIframe 생성시 까지 대기
//                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("searchIframe")));
//                // searchIframe으로 이동
//                webDriver.switchTo().frame("searchIframe");

                // 첫 번째 장소 로딩될 때까지 잠시 대기
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".PlaceItem")));

                // 스크랩 대상을 전부 브라우저에 뿌리기 위해 스크롤 내리기 작업 수행
                log.info(">>> 검색 결과 전체 로딩을 위해 스크롤을 내립니다...");

                WebElement scrollContainer = webDriver.findElement(By.cssSelector("[id='info.body']"));
                long lastHeight = (long) ((JavascriptExecutor) webDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);

                while (true) {
                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].scrollTo(0, arguments[0].scrollHeight)", scrollContainer);
                    Thread.sleep(1500); // 카카오 서버가 데이터를 줄 시간을 줌
                    long newHeight = (long) ((JavascriptExecutor) webDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);
                    if (newHeight == lastHeight) break;
                    lastHeight = newHeight;
                }

                // 스크롤 수행 작업 종료후 리스트 확정
                List<WebElement> placList = webDriver.findElements(By.cssSelector("li.PlaceItem"));
                log.info(">>> [KKO][스크롤 완료] 현재페이지 검색결과: " + placList.size() + "건");

                for (WebElement plac : placList) {

                    String mainHandle = "";
                    try {
                        BCH00000102IN insertParam = new BCH00000102IN();

                        String title = plac.findElement(By.cssSelector("[data-id='name']")).getText();
                        String category = plac.findElement(By.cssSelector("[data-id='subcategory']")).getText();

//                        if (EXCLUDE_CATEGORIES.stream().anyMatch(word -> category.contains(word))) {
//                            log.info(">>> [제외업종] {}은(는) {} 업종이라 제외합니다.", title, category);
//                            continue;
//                        }

                        String placNm = "";       // 가게명
                        String extlPlacId = "";   // 외부장소ID
                        String addr = "";
                        String telNo = "";
                        try {
                            // 메인창 보관
                            mainHandle = webDriver.getWindowHandle();

                            // 상세보기 클릭 (새 탭 열림)
                            WebElement moreviewEl = plac.findElement(By.cssSelector("[data-id='moreview']"));
                            ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", moreviewEl);
                            Thread.sleep(2000);

                            for (String handle : webDriver.getWindowHandles()) {
                                if (!handle.equals(mainHandle)) {
                                    webDriver.switchTo().window(handle);    // 새 탭으로 이동!
                                    break;
                                }
                            }

                            // 현재 브라우저의 URL을 가져옴 (생략 가능하나 ID 추출 위해 유지)
                            String currentUrl = webDriver.getCurrentUrl();
                            if (currentUrl.contains("place.map.kakao.com/")) {
                                // 1. 마지막 '/'의 위치를 찾아서 그 이후 문자열만 추출
                                extlPlacId = currentUrl.substring(currentUrl.lastIndexOf("/") + 1);
                            }

                            // ------------------------------------------------------------------
                            // 상세 정보(장소명, 주소, 전화번호) 긁기 + 데이터 청소
                            // ------------------------------------------------------------------
                            List<WebElement> placNmEls = webDriver.findElements(By.cssSelector(".tit_place"));
                            if (!placNmEls.isEmpty()) {
                                placNm = placNmEls.get(0).getText().trim();
                            }

                            List<WebElement> addrEls = webDriver.findElements(By.cssSelector(".unit_default:has(.ico_address) .txt_detail"));
                            if (!addrEls.isEmpty()) {
                                addr = addrEls.get(0).getText().trim();
                            }

                            List<WebElement> telEls = webDriver.findElements(By.cssSelector(".unit_default:has(.ico_call2) .txt_detail"));
                            if (!telEls.isEmpty()) {
                                String tmpTelNo = telEls.get(0).getText().trim();

                                if (tmpTelNo.matches("^[0-9\\-]+$")) {
                                    telNo = tmpTelNo;
                                }
                                else {
                                    telNo = "";
                                    log.warn(">>> [KKO] {} 전화번호에 숫자 or '-' 이외의 문자가 섞여 있어 빈값 처리", tmpTelNo);
                                }
                            }

                        } catch (Exception e) {
                            log.error(">>> 상세 정보 수집 실패: " + placNm + " | " + e.getMessage());
                        }

                        // ID가 없으면 저장하지 않고 스킵
                        if (extlPlacId == null || extlPlacId.isEmpty()) {
                            log.error(">>> [" + placNm + "] 외부ID를  찾을 수 없어 건너뜁니다.");
                            continue;
                        }

//                        if (addr == null || addr.isEmpty()) {
//                            log.error(">>> [" + placNm + "] 카테고리를  찾을 수 없어 건너뜁니다.");
//                            continue;
//                        }

                        if (addr == null || addr.isEmpty()) {
                            log.error(">>> [" + placNm + "] 주소를  찾을 수 없어 건너뜁니다.");
                            continue;
                        }
                        // 데이터 세팅 및 DB 저장
                        insertParam.setExtlPlacId(extlPlacId);
                        insertParam.setPlacNm(placNm);
                        insertParam.setAddr(addr);
                        insertParam.setTelNo(telNo);
                        insertParam.setSorcDvcd("KKO");
                        insertParam.setCtgrDvcd("RST");

                        try {
                            daoBCH000001.insertPlac(insertParam);
                            log.info(">>> [성공] ID: " + extlPlacId + " | 가게명: " + placNm);
                        } catch (DuplicateKeyException e) {
                            log.info(">>> [중복] 이미 수집된 가게입니다: " + placNm);
                        }

                    } catch (Exception e) {
                        log.error(">>> 루프 내부 오류: " + e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        if (webDriver.getWindowHandles().size() > 1) {
                            // 현재 새 탭 닫고 메인창으로 복귀
                            webDriver.close();
                            webDriver.switchTo().window(mainHandle);
                        }
                    }
                }

                try {
                    // ------------------------------------------------------------------
                    // 페이징 처리
                    // ------------------------------------------------------------------

                    // 첫 페이지일 경우
                    if (currentPage == 1) {
                        WebElement seeMoreEl = webDriver.findElement(By.cssSelector("[id='info.search.place.more']"));

                        log.info(">>> [KKO] 1페이지 완료 -> 장소 더보기 클릭");
                        ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", seeMoreEl);
                        currentPage++;
                        Thread.sleep(2000);
                    }
                    // 각 그룹의 마지막 페이지일 경우
                    else if (currentPage % 5 == 0) {
                        WebElement nextBtn = webDriver.findElement(By.cssSelector("[id='info.search.page.next']"));

                        // 진짜 마지막 그룹에서 disabled되는지 체크 필요
                        if (nextBtn.getAttribute("class").contains("disabled")) {
                            log.info(">>> [KKO] 마지막 페이지입니다. 수집 종료.");
                            hasNext = false;
                        }
                        else {
                            log.info(">>> [KKO] {} 페이지 수집 완료 -> 다음 그룹으로 이동", currentPage);
                            ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", nextBtn);
                            currentPage++;
                        }
                    }
                    else {
                        int nextTargetNo = (currentPage % 5)  + 1;
                        String nextId = "info.search.page.no" + nextTargetNo;

                        WebElement nextNumBtn = webDriver.findElement(By.cssSelector("[id='" + nextId + "']"));
                        log.info(">>> [KKO] {} 페이지 수집 완료 -> 다음 페이지로 이동", currentPage);

                        ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", nextNumBtn);
                        currentPage++;
                    }
                    // 페이지 전환 후 리스트 로딩 대기
                    Thread.sleep(3000);
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
