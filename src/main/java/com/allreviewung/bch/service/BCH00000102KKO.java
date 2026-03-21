package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.dto.BCH00000101DTO;
import com.allreviewung.bch.service.svo.BCH00000201IN;
import com.allreviewung.bch.service.svo.BCH00000202IN;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BCH00000102KKO {

    private final BCH000001DAO daoBCH000001;
    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트카페", "커피전문점", "간식", "제과,베이커리");

    public void collect(WebDriver webDriver) {

        String keyWord = "";
        try {
            // 카카오 지도로 이동
            webDriver.get("https://map.kakao.com/");

            // 검색창이 화면에 나타날 때까지 최대 대기시간 지정(5초)
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[id='search.keyword.query']")));

            List<BCH00000101DTO> scrpTrgtList = daoBCH000001.selectScrpTrgtList();

            if (scrpTrgtList != null && !scrpTrgtList.isEmpty()) {
                for (int i = 0; i < scrpTrgtList.size(); i++) {
                    BCH00000101DTO scrpTrgt = scrpTrgtList.get(i);
                    double progress = (double)(i + 1) / scrpTrgtList.size() * 100;
                    log.info(">>> 카카오맵 크롤링 진행률: {}% 진행건수: {}/{}건", String.format("%.2f", progress), i + 1, scrpTrgtList.size());

                    String[] pblcDataAddrArr = scrpTrgt.getPblcDataAddr().split(" ");

                    // 비교용 주소 (서울강동구천호동 형태)
                    String tmpPblcDataAddr = "";
                    if (pblcDataAddrArr.length >= 3) {
                        tmpPblcDataAddr = pblcDataAddrArr[0] + pblcDataAddrArr[1] + pblcDataAddrArr[2];
                    } else {
                        tmpPblcDataAddr = String.join("", pblcDataAddrArr);
                    }
                    tmpPblcDataAddr = tmpPblcDataAddr.replace("서울특별시", "서울");
                    keyWord = pblcDataAddrArr[1] + " " + scrpTrgt.getPblcDataPlacNm(); // 구명 + 장소명

                    searchInput.sendKeys(keyWord);
                    searchInput.sendKeys(Keys.ENTER);
                    Thread.sleep(1000); // 검색 결과 로딩 대기

                    int currentPage = 1;
                    boolean hasNextPage = true;
                    boolean isMatched = false;
                    boolean isDuplicate = false;
                    while (hasNextPage) {
                        // 일단 리스트가 나타날 때까지 아주 잠깐만 기다려줌 (로딩 시간)
//                        Thread.sleep(1000);
                        try {
                            wait.until(d -> {
                                // 검색 결과 리스트가 1개라도 떴는지 확인
                                boolean hasResults = d.findElements(By.cssSelector("li.PlaceItem")).size() > 0;

                                // '결과 없음' 영역이 화면에 표시되는지 확인
                                WebElement noPlaceDiv = d.findElement(By.id("info.noPlace"));
                                boolean isNoResultVisible = noPlaceDiv.isDisplayed();

                                // 둘 중 하나라도 만족하면 대기 종료!
                                return hasResults || isNoResultVisible;
                            });
                        } catch (Exception e) {
                            log.warn(">>> [KKO] 최대 대기시간까지 결과/결과없음 둘 다 안 뜸: {}", keyWord);
                        }

                        //검색 결과 리스트가 있는지 '안전하게' 확인
                        List<WebElement> tmpPlacList = webDriver.findElements(By.cssSelector("li.PlaceItem"));
                        List<WebElement> placList = new ArrayList<>();

                        // 원본 검색 결과 개수 파악
                        int totalFound = tmpPlacList.size();

                        // 제외 업종 거르기
                        for (WebElement tmpPlac : tmpPlacList) {
                            String title = tmpPlac.findElement(By.cssSelector("[data-id='name']")).getText();
                            String category = tmpPlac.findElement(By.cssSelector("[data-id='subcategory']")).getText();

                            if (EXCLUDE_CATEGORIES.stream().anyMatch(word -> category.contains(word))) {
                                log.info(">>> [제외업종] {} (업종: {}) -> 리스트에서 제외", title, category);
                                continue;
                            }
                            placList.add(tmpPlac);
                        }

                        // 결과 분석 및 로그 분리
                        if (totalFound == 0) {
                            // 애초에 검색된 게 0건인 경우
                            log.warn(">>> [검색결과 없음] 키워드: '{}' 로 검색된 장소가 아예 없습니다. (ID: {})", keyWord, scrpTrgt.getScrpTrgtId());
                            hasNextPage = false;
                            continue;
                        }
                        if (placList.isEmpty()) {
                            // 검색은 됐는데(totalFound > 0), 필터링하니 남은 게 없는 경우
                            log.warn(">>> [필터링 탈락] 검색결과는 {}건 있었으나, 모두 제외 업종이라 수집할 게 없습니다. 키워드: '{}'", totalFound, keyWord);
                            hasNextPage = false;
                            continue;
                        }
                        
                        log.info(">>> [수집가능] 총 {}건 중 유효 데이터 {}건 확인 완료.", totalFound, placList.size());

                        BCH00000201IN updateParam = new BCH00000201IN();
                        try {
                            updateParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                            updateParam.setProgStatCd("03");
                            // 진행 상태코드 변경 02: 네이버 완료 -> 03: 카카오 진행중
                            int result = daoBCH000001.updateScrpTrgtStat(updateParam);

                            if (result == 0) {
                                throw new RuntimeException("DB에 업데이트 대상이 없습니다");
                            }
                            log.info(">>> [KKO] 진행상태 업데이트 완료. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd());
                        } catch (Exception e) {
                            log.error(">>> [KKO] 진행상태 업데이트 실패. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}, | 에러: {}\n 다음가게로 이동...", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd(), e.getMessage());
                            continue;
                        }

                        // ------------------------------------------------------------------
                        // keyWord에 따른 검색결과 리스트 반복문으로 돌면서 비교 후 INSERT처리
                        // ------------------------------------------------------------------
                        for (WebElement plac : placList) {
                            String mainHandle = "";
                            try {
                                String placNm = "";       // 장소명
                                String extlPlacId = "";   // 외부장소ID
                                String addr = "";
                                String telNo = "";
                                try {
                                    // [수정] 각 리스트 아이템(plac) 안에서 주소를 찾아야 함
                                    WebElement addrEl = plac.findElement(By.cssSelector("[data-id='address']"));
                                    String mainPageAddr = addrEl.getText();

                                    // 공공데이터 주소가 포함되어 있는지 확인 (공백 제거 후 비교)
                                    String cleanMainAddr = mainPageAddr.replace(" ", "");
                                    log.info(">>> 검색 키워드: {}, 장소명: {}, 카카오 주소: [{}], 공공데이터 주소: [{}]", keyWord, scrpTrgt.getPblcDataPlacNm(), mainPageAddr, tmpPblcDataAddr);
                                    if (!cleanMainAddr.contains(tmpPblcDataAddr)) {
                                        log.warn(">>> [패스] 주소 불일치 scrpTrgtId: {}", scrpTrgt.getScrpTrgtId());
                                        continue; // 다음 가게로
                                    }
                                    log.info(">>> [성공] 주소 일치 상세정보 수집 시작");

                                    // 메인창 보관
                                    mainHandle = webDriver.getWindowHandle();

                                    // 상세보기 클릭 (새 탭 열림)
                                    WebElement moreviewEl = plac.findElement(By.cssSelector("[data-id='moreview']"));
                                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", moreviewEl);
                                    Thread.sleep(1000);

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

                                    // ID가 없으면 저장하지 않고 스킵
                                    if (extlPlacId.isEmpty()) {
                                        log.error(">>> [" + scrpTrgt.getPblcDataPlacNm() + "] 외부ID를  찾을 수 없어 건너뜁니다.");
                                        continue;
                                    }

                                    // ------------------------------------------------------------------
                                    // 상세 정보(장소명, 전화번호) 긁기 + 데이터 청소
                                    // ------------------------------------------------------------------
                                    List<WebElement> placNmEls = webDriver.findElements(By.cssSelector(".tit_place"));
                                    if (!placNmEls.isEmpty()) {
                                        placNm = placNmEls.get(0).getText().trim();
                                    }


                                    List<WebElement> addrEls = webDriver.findElements(By.cssSelector(".unit_default:has(.ico_address) .txt_detail"));
                                    if (!addrEls.isEmpty()) {
                                        addr = addrEls.get(0).getText().trim();
                                    }

                                    // 주소가 없으면 저장하지 않고 스킵
                                    if (addr.isEmpty()) {
                                        log.error(">>> [" + scrpTrgt.getPblcDataPlacNm() + "] 주소를 찾을 수 없어 건너뜁니다.");
                                        continue;
                                    }

                                    List<WebElement> telEls = webDriver.findElements(By.cssSelector(".unit_default:has(.ico_call2) .txt_detail"));
                                    if (!telEls.isEmpty()) {
                                        String tmpTelNo = telEls.get(0).getText().trim();

                                        if (tmpTelNo.matches("^[0-9\\-]+$")) {
                                            telNo = tmpTelNo;
                                        } else {
                                            telNo = "";
                                            log.warn(">>> [KKO] {} 전화번호에 숫자 or '-' 이외의 문자가 섞여 있어 빈값 처리", tmpTelNo);
                                        }
                                    }

                                } catch (Exception e) {
                                    log.error(">>> 상세 정보 수집 실패: " + scrpTrgt.getPblcDataPlacNm() + " | " + e.getMessage());
                                }

                                // 데이터 세팅 및 DB 저장
                                BCH00000202IN insertParam = new BCH00000202IN();
                                insertParam.setExtlPlacId(extlPlacId);
                                insertParam.setPlacNm(placNm);
                                insertParam.setAddr(addr);
                                insertParam.setTelNo(telNo);
                                insertParam.setSorcDvcd("KKO");
                                insertParam.setCtgrDvcd("RST");
                                try {
                                    int insertResult = daoBCH000001.insertPlac(insertParam);

                                    if (insertResult == 0) {
                                        throw new RuntimeException("INSERT 결과가 0건입니다.");
                                    }
                                    try {
                                        updateParam.setProgStatCd("04");
                                        // 진행 상태코드 변경 03: 카카오 진행중 -> 04: 전체 완료
                                        int result = daoBCH000001.updateScrpTrgtStat(updateParam);

                                        if (result == 0) {
                                            throw new RuntimeException("DB에 업데이트 대상이 없습니다");
                                        }
                                        log.info(">>> [KKO] 진행상태 업데이트 완료. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd());
                                    } catch (Exception e) {
                                        log.error(">>> [KKO] 진행상태 업데이트 실패. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}, | 에러: {}\n 다음가게로 이동...", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd(), e.getMessage());
                                        continue;
                                    }
                                    isMatched = true;
                                    log.info(">>> [성공] 수집대상ID: {}, 관리번호: {}, 장소명: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataMngNo(), scrpTrgt.getPblcDataPlacNm());
                                } catch (DuplicateKeyException e) {
                                    isDuplicate = true;
                                    log.info(">>> [중복] 이미 수집된 가게입니다: " + scrpTrgt.getPblcDataPlacNm());
                                } catch (Exception e) {
                                    log.error(">>> [KKO] DB 작업 중 예외 발생. ID: {} | 에러: {}", scrpTrgt.getScrpTrgtId(), e.getMessage());
                                    continue; // 다음 가게로 이동
                                }
                                if (isMatched || isDuplicate) break;
                            } catch (Exception e) {
                                log.error(">>> 루프 내부 오류: " + e.getMessage());
                                e.printStackTrace();
                            } finally {
                                if (webDriver.getWindowHandles().size() > 1) {
                                    // 현재 새 탭 닫고 메인창으로 복귀
                                    webDriver.close();
                                    webDriver.switchTo().window(mainHandle);
                                }
                            }
                        }

                        // ------------------------------------------------------------------
                        // 페이징 처리
                        // ------------------------------------------------------------------
                        try {
                            // 첫 페이지일 경우
                            if (currentPage == 1) {
                                List<WebElement> seeMoreEls = webDriver.findElements(By.cssSelector("[id='info.search.place.more']"));

                                if (seeMoreEls.isEmpty() || !seeMoreEls.get(0).isDisplayed()) {
                                    log.info(">>> [KKO] 1페이지가 끝입니다. 다음 가게로 이동.");
                                    hasNextPage = false;
                                }
                                else {
                                    log.info(">>> [KKO] 1페이지 완료 -> 장소 더보기 클릭");
                                    // [수정] 리스트에 담아두지 말고, 클릭 가능한 상태가 될 때까지 기다렸다가 '즉시' 클릭
                                    wait.until(ExpectedConditions.elementToBeClickable(By.id("info.search.place.more"))).click();
                                    currentPage++;
                                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.PlaceItem")));
                                }
                            }

                            if (currentPage == 1) {
                                // 더보기 요소 새로 추출
                                List<WebElement> freshSeeMoreEls = webDriver.findElements(By.id("info.search.place.more"));

                                // 더보기 요소 비어있지 않고(존재하고), 화면에 보일 때만 실행
                                if (!freshSeeMoreEls.isEmpty() && freshSeeMoreEls.get(0).isDisplayed()) {
                                    log.info(">>> [KKO] 1페이지 완료 -> 장소 더보기 클릭");

                                    // 새로 뽑은 리스트의 0번째 요소를 즉시 클릭
                                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", freshSeeMoreEls.get(0));

                                    currentPage++;
                                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.PlaceItem")));
                                } else {
                                    log.info(">>> [KKO] 더보기 버튼이 없거나 안 보임 (1페이지 종료)");
                                    hasNextPage = false;
                                }
                            }

                            // 각 그룹의 마지막 페이지일 경우
                            else if (currentPage % 5 == 0) {
                                WebElement nextBtn = webDriver.findElement(By.cssSelector("[id='info.search.page.next']"));

                                // 진짜 마지막 그룹에서 disabled되는지 체크 필요
                                if (nextBtn.getAttribute("class").contains("disabled")) {
                                    log.info(">>> [KKO] 마지막 페이지입니다. 수집 종료.");
                                    hasNextPage = false;
                                } else {
                                    log.info(">>> [KKO] {} 페이지 수집 완료 -> 다음 그룹으로 이동", currentPage);
                                    ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", nextBtn);
                                    currentPage++;
                                    wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.PlaceItem")));
                                }
                            } else {
                                int nextTargetNo = (currentPage % 5) + 1;
                                String nextId = "info.search.page.no" + nextTargetNo;

                                WebElement nextNumBtn = webDriver.findElement(By.cssSelector("[id='" + nextId + "']"));
                                log.info(">>> [KKO] {} 페이지 수집 완료 -> 다음 페이지로 이동", currentPage);

                                ((JavascriptExecutor) webDriver).executeScript("arguments[0].click();", nextNumBtn);
                                currentPage++;
                                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("li.PlaceItem")));
                            }
                        } catch (Exception e) {
                            log.error(">>> 페이지 이동 중 오류 발생: " + e.getMessage());
                            hasNextPage = false;
                        }
                        if (isMatched) {
                            log.info(">>> [탈출] 매칭되는 가게를 찾았으므로 다음 페이지를 확인하지 않고 다음 키워드로 넘어갑니다.");
                            hasNextPage = false;
                            break;
                        }
                    }
                    // 검색창 비우기
                    searchInput.sendKeys(Keys.CONTROL + "a");
                    searchInput.sendKeys(Keys.BACK_SPACE);
                }
            }
        } catch (Exception e) {
            log.error(">>> [KKO 리뷰 수집중 오류 발생] 키워드: {} | 에러내용: {}", keyWord, e.getMessage());
        }
    }
}
