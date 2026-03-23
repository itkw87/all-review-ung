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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BCH00000102NAV {

    private int scrpTrgtCount;
    private int insertCount;
    private int skipCount;

    private final BCH000001DAO daoBCH000001;

    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트", "커피");

    public WebDriver collect(WebDriver webDriver) {
        WebDriver currentDriver = webDriver;

        int totalCount = 0;
        int restartInterval = 50; // 50건마다 재시작

        String keyWord = "";

        Duration durationSeconds = Duration.ofSeconds(2);
        try {
            // 네이버 지도로 이동
            currentDriver.get("https://map.naver.com/");

            // 검색창이 화면에 나타날 때까지 최대 대기시간 지정(Duration.ofSeconds)
            WebDriverWait wait = new WebDriverWait(currentDriver, durationSeconds);

            List<BCH00000101DTO> scrpTrgtList = daoBCH000001.selectScrpTrgtList(Arrays.asList("00", "01", "05"));

            if (scrpTrgtList != null && !scrpTrgtList.isEmpty()) {
                scrpTrgtCount = scrpTrgtList.size();
                for (int i = 0; i < scrpTrgtList.size(); i++) {
                    // ------------------------------------------------------------------
                    // ★ 주기적 재시작 로직 (restartInterval 건 마다)
                    // ------------------------------------------------------------------
                    if (i > 0 && i % restartInterval == 0) {
                        log.info(">>> [성능 최적화] 브라우저가 지쳤습니다. 메모리 정리를 위해 재시작합니다. (현재: {}건)", i);
                        currentDriver.quit(); // 현재 브라우저 완전 종료

                        // 새로운 브라우저 실행
                        currentDriver = initNewDriver();
                        currentDriver.get("https://map.naver.com/");

                        // wait 객체도 새 드라이버에 맞춰 다시 생성
                        wait = new WebDriverWait(currentDriver, durationSeconds);
                        log.info(">>> [성능 최적화] 브라우저 재시작 완료.");
                    }

                    currentDriver.switchTo().defaultContent();
                    BCH00000101DTO scrpTrgt = scrpTrgtList.get(i);
                    log.info("================================================================================");
                    log.info("[ NAV ] 수집대상ID: {} | 장소명: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm());

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

                    double progress = (double) (i + 1) / scrpTrgtList.size() * 100;
                    log.info(">>> 네이버맵 크롤링 진행률: {}%, 진행건수: {}/{}건, 검색 키워드: {}", String.format("%.2f", progress), i + 1, scrpTrgtList.size(), keyWord);

                    // ==================================================================
                    // ★ [최종 심폐소생술 적용] 재검색 변수 세팅
                    // ==================================================================
                    String fallbackCategory = "식당";
                    String searchKeyword = keyWord;
                    boolean isNextScrpTrgt = false;

                    // 최대 2번 검색 (1차: 원본 키워드, 2차: 업종 추가 키워드)
                    for (int attempt = 1; attempt <= 2; attempt++) {
                        currentDriver.switchTo().defaultContent();
                        WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));

                        // 키워드 초기화 및 입력
                        searchInput.click();
                        searchInput.sendKeys(Keys.CONTROL + "a");
                        searchInput.sendKeys(Keys.BACK_SPACE);
                        searchInput.sendKeys(searchKeyword);
                        searchInput.sendKeys(Keys.ENTER);

                        boolean isDirectEntry = false;
                        boolean hasSearchIframe = false;
                        boolean isAddressPage = false;

                        // #entryIframe 이든 #searchIframe 이든 둘 중 하나라도 나올 때까지 대기
                        try {
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#entryIframe, #searchIframe")));

                            // ★ [긴급 처방 추가] 네이버가 프레임을 완벽히 그릴 수 있도록 딱 0.8초만 줍니다!
                            // 이 줄이 없으면 코드가 너무 빨라서 상세창을 못 보고 지나칩니다.
                            Thread.sleep(2000);

                        } catch (Exception e) {
                            log.info(">>> [NAV] 검색 결과 프레임 로딩 타임아웃 (키워드: {})", searchKeyword);
                        }

                        // 이제 화면에 뭐가 떴는지 대기 없이 즉시 확인
                        if (!currentDriver.findElements(By.id("entryIframe")).isEmpty()) {
                            currentDriver.switchTo().frame("entryIframe");
                            List<WebElement> addressCheck = currentDriver.findElements(By.cssSelector(".icon_address, [class*='StyledEntryAddress']"));
                            if (!addressCheck.isEmpty()) {
                                isAddressPage = true; // 주소로 인식하여 나오는 영역
                            } else {
                                isDirectEntry = true; // 상세정보 프레임
                            }
                            currentDriver.switchTo().defaultContent();
                        } else if (!currentDriver.findElements(By.id("searchIframe")).isEmpty()) {
                            hasSearchIframe = true; // 검색결과 프레임
                        }

                        // 3. 심폐소생술 (재검색 결정)
                        if ((!isDirectEntry && !hasSearchIframe) || isAddressPage) {
                            if (attempt == 1) {
                                log.warn(">>> [NAV] 주소 화면 또는 잘못 인식됨! 업종명 붙여서 재검색 시도: {}", keyWord + " " + fallbackCategory);
                                searchKeyword = keyWord + " " + fallbackCategory;
                                continue; // 루프 처음으로 돌아가서 바뀐 키워드로 다시 검색
                            } else {
                                log.warn(">>> [NAV] 재검색도 실패했습니다. 진짜 패스: {}", searchKeyword);
                                updateSkipStatus(scrpTrgt.getScrpTrgtId());
                                break; // 2번 다 실패하면 이 장소는 패스
                            }
                        }

                        // ==================================================================
                        // 4. 프레임이 정상적으로 뜬 경우 (기존 로직 진행)
                        // ==================================================================
                        boolean hasNextPage = true;

                        while (hasNextPage) {
                            currentDriver.switchTo().defaultContent();

                            if (isDirectEntry) {
                                // [단건 케이스]
                                currentDriver.switchTo().frame("entryIframe");
                                log.info(">>> [NAV] 단건 검색 결과 바로 진입");
                                isNextScrpTrgt = scrapeAndSaveDetail(currentDriver, wait, scrpTrgt, searchKeyword, tmpPblcDataAddr);
                                break; // 단건 처리 끝났으니 while 탈출

                            } else {
                                // [다건 케이스]
                                currentDriver.switchTo().frame("searchIframe");

                                try {
                                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.UEzoS")));
                                } catch (Exception e) {
                                    log.warn(">>> [NAV] 결과가 아예 없습니다. 패스합니다: {}", searchKeyword);
                                    updateSkipStatus(scrpTrgt.getScrpTrgtId());
                                    hasNextPage = false;
                                    break;
                                }

                                // 스크롤 내리기 작업 수행
                                log.info(">>> 검색 결과 전체 로딩을 위해 스크롤을 내립니다...");
                                WebElement scrollContainer = currentDriver.findElement(By.cssSelector("#_pcmap_list_scroll_container"));
                                long lastHeight = (long) ((JavascriptExecutor) currentDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);

                                while (true) {
                                    ((JavascriptExecutor) currentDriver).executeScript("arguments[0].scrollTo(0, arguments[0].scrollHeight)", scrollContainer);
                                    // 무조건 0.8초 쉬지 말고, 높이가 변할 때까지만 대기 (최대 1초)
                                    final long currentLastHeight = lastHeight;
                                    try {
                                        new WebDriverWait(currentDriver, Duration.ofMillis(1000)).until(d ->
                                                (long) ((JavascriptExecutor) d).executeScript("return arguments[0].scrollHeight", scrollContainer) > currentLastHeight
                                        );
                                    } catch (Exception e) { break; } // 더 이상 안 변하면 탈출

                                    long newHeight = (long) ((JavascriptExecutor) currentDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);
                                    if (newHeight == lastHeight) break;
                                    lastHeight = newHeight;
                                }

                                List<WebElement> tmpPlacList = currentDriver.findElements(By.cssSelector("li.UEzoS"));
                                List<WebElement> placList = new ArrayList<>();

                                log.info(">>> [스크롤 완료] 현재페이지 검색결과: " + tmpPlacList.size() + "건");

                                // 제외 업종 필터링
                                for (WebElement tmpPlac : tmpPlacList) {
                                    String title = tmpPlac.findElement(By.cssSelector(".TYaxT")).getText();
                                    String category = "";
                                    try {
                                        category = tmpPlac.findElement(By.cssSelector(".KCMnt")).getText();
                                    } catch (Exception e) {
                                        log.info(">>> [NAV] 리스트에서 업종 안 보임: {}", title);
                                    }

                                    final String finalCategory = category;
                                    if (!finalCategory.isEmpty() && EXCLUDE_CATEGORIES.stream().anyMatch(word -> finalCategory.contains(word))) {
                                        log.info(">>> [제외업종] {} 제외", title);
                                        continue;
                                    }
                                    placList.add(tmpPlac);
                                }

                                if (placList.isEmpty()) {
                                    log.warn(">>> [NAV] 유효한 검색 결과가 없습니다. 패스. 검색 키워드: {}, 수집대상ID: {}", searchKeyword, scrpTrgt.getScrpTrgtId());
                                    updateSkipStatus(scrpTrgt.getScrpTrgtId());
                                    hasNextPage = false;
                                    continue;
                                }

                                BCH00000201IN updateParam = new BCH00000201IN();
                                try {
                                    updateParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                                    updateParam.setProgStatCd("01");
                                    int result = daoBCH000001.updateScrpTrgtStat(updateParam);
                                    if (result == 0) throw new RuntimeException("DB에 업데이트 대상이 없습니다");
                                    log.info(">>> [NAV] 진행상태 업데이트 완료. 수집대상ID: {}, 예정 진행상태 코드: {}", scrpTrgt.getScrpTrgtId(), updateParam.getProgStatCd());
                                } catch (Exception e) {
                                    log.error(">>> [NAV] 진행상태 업데이트 실패. 다음가게로 이동... 에러: {}", e.getMessage());
                                    continue;
                                }

                                // 리스트 순회 및 수집
                                int listSize = placList.size();
                                for (int j = 0; j < listSize; j++) {
                                    try {
                                        currentDriver.switchTo().defaultContent();
                                        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("searchIframe")));

                                        List<WebElement> freshList = currentDriver.findElements(By.cssSelector("li.UEzoS"));
                                        if (j >= freshList.size()) break;

                                        WebElement plac = freshList.get(j);

                                        ((JavascriptExecutor) currentDriver).executeScript("arguments[0].scrollIntoView({block: 'center'});", plac);

                                        // 바로 클릭 가능할 때까지 wait로 변경
                                        WebElement finalPlac = wait.until(ExpectedConditions.elementToBeClickable(
                                                currentDriver.findElements(By.cssSelector("li.UEzoS")).get(j)
                                        ));

                                        try {
                                            ((JavascriptExecutor) currentDriver).executeScript("arguments[0].click();", finalPlac);
                                        } catch (Exception e) {
                                            finalPlac.click();
                                        }


                                        try {
                                            // 상세정보 프레임 대기시간을 1초만 갖고 안뜨면 바로 catch로 던지기
                                            new WebDriverWait(currentDriver, Duration.ofSeconds(1))
                                                    .until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));

                                            isNextScrpTrgt = scrapeAndSaveDetail(currentDriver, wait, scrpTrgt, searchKeyword, tmpPblcDataAddr);
                                        } catch (Exception e) {
                                            updateSkipStatus(scrpTrgt.getScrpTrgtId());
                                            log.warn(">>> [NAV] 상세 프레임 미발생(1초 초과) 스킵: {}", searchKeyword);
                                            isNextScrpTrgt = true;
                                            break;
                                        }

                                        if (isNextScrpTrgt) break;
                                    } catch (Exception e) {
                                        log.error(">>> 루프 내부 오류 발생  키워드: {}, 에러메세지: {}", searchKeyword, e.getMessage());
                                    }
                                }

                                // 페이징 처리
                                try {
                                    currentDriver.switchTo().defaultContent();
                                    currentDriver.switchTo().frame("searchIframe");

                                    List<WebElement> pageBtns = currentDriver.findElements(By.cssSelector(".eUTV2"));
                                    if (!pageBtns.isEmpty()) {
                                        WebElement nextBtn = pageBtns.get(pageBtns.size() - 1);
                                        String isDisabled = nextBtn.getAttribute("aria-disabled");

                                        if ("false".equals(isDisabled)) {
                                            log.info(">>> 다음 페이지 버튼을 발견했습니다. 클릭합니다.");
                                            nextBtn.click();

                                        } else {
                                            log.info(">>> [알림] 마지막 페이지입니다. 수집을 종료합니다.");
                                            hasNextPage = false;
                                        }
                                    } else {
                                        log.info(">>> [알림] 페이지 버튼을 찾을 수 없어 종료합니다.");
                                        hasNextPage = false;
                                    }
                                } catch (Exception e) {
                                    log.error(">>> 페이지 이동 중 오류 발생: " + e.getMessage());
                                    hasNextPage = false;
                                }

                                if (isNextScrpTrgt) {
                                    log.info(">>> [탈출] 매칭되는 가게를 찾았으므로 다음 페이지를 확인하지 않고 넘어갑니다.");
                                }
                            }
                        } // while (hasNextPage)
                        break; // 정상적으로 검색 및 수집을 마쳤으면 attempt 루프(재검색 루프)도 탈출!
                    } // for (int attempt = 1; attempt <= 2; attempt++)
                    log.info("================================================================================\n");
                } // for (int i = 0; i < scrpTrgtList.size(); i++)
            } // if (scrpTrgtList != null && !scrpTrgtList.isEmpty())
        } catch (Exception e) {
            log.error(">>> [NAV 리뷰 수집중 오류 발생] 키워드: {} | 에러내용: {}", keyWord, e.getMessage());
        } finally {
            log.info("==========================================");
            log.info(">>> [수집 최종 리포트]");
            log.info(">>> 1. 전체 대상: {}건", scrpTrgtCount);
            log.info(">>> 2. Insert 성공: {}건", insertCount);
            log.info(">>> 3. 스킵(수집불가): {}건", skipCount);
            log.info(">>> 4. 기타(중복 등): {}건", (scrpTrgtCount - insertCount - skipCount));
            log.info("==========================================");

            // 다음 실행을 위해 카운트 리셋
            scrpTrgtCount = 0;
            insertCount = 0;
            skipCount = 0;
        }
        return currentDriver;

    }

    /**
     * 상세 페이지에서 정보를 추출하고 DB에 저장하는 공통 메서드
     */
    private boolean scrapeAndSaveDetail(WebDriver currentDriver, WebDriverWait wait, BCH00000101DTO scrpTrgt, String keyWord, String tmpPblcDataAddr) {

        boolean isNextScrpTrgt = false;

        try {
            String placNm = "";       // 가게명
            String extlPlacId = "";   // 외부장소ID
            String addr = "";
            String telNo = "";
            try {
                // 리스트 프레임(searchIframe)에서 빠져나와 상세 프레임(entryIframe) 이동 (검색결과가 단건이여도 상세 프레임은 생김)
                currentDriver.switchTo().defaultContent();
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));
                currentDriver.switchTo().frame("entryIframe");

                // ------------------------------------------------------------------
                // [검문소] 상세페이지 업종 체크 (여기서 카페면 바로 아웃)
                // ------------------------------------------------------------------
                List<WebElement> catEls = currentDriver.findElements(By.cssSelector(".lnJFt"));
                if (!catEls.isEmpty()) {
                    final String detailCategory = catEls.get(0).getText();
                    if (EXCLUDE_CATEGORIES.stream().anyMatch(word -> detailCategory.contains(word))) {
                        log.warn(">>> [제외업종 탈락] '{}' 업종이라 수집 중단. (ID: {})", detailCategory, scrpTrgt.getScrpTrgtId());
                        // 다음 검색 결과 목록으로 패스
                        return false;
                    }
                }
                // ------------------------------------------------------------------
                // [상태 업데이트] 검문소 통과했으니 이제 '03(진행중)'으로 변경
                // ------------------------------------------------------------------
                BCH00000201IN startParam = new BCH00000201IN();
                startParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                startParam.setProgStatCd("01");
                daoBCH000001.updateScrpTrgtStat(startParam);
                log.info(">>> [NAV] 검문소 통과! 진행상태 03 업데이트 완료.");

                // 주소(.pz7wy) 글자가 실제로 '눈에 보일 때까지' 대기
                try {
                    List<WebElement> addrEls = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(".pz7wy")));
                    if (!addrEls.isEmpty()) {
                        addr = addrEls.get(0).getText().trim();
                    }
                    // 주소가 없으면 저장하지 않고 스킵
                    if (addr.isEmpty()) {
                        updateSkipStatus(scrpTrgt.getScrpTrgtId());
                        log.error(">>> [" + scrpTrgt.getPblcDataPlacNm() + "] 주소를 찾을 수 없어 건너뜁니다.");
                        // 다음 수집 대상 목록으로 패스
                        isNextScrpTrgt = true;
                    }
                } catch (Exception e) {
                    log.info(">>> [" + placNm + "] 주소 로딩 시간이 너무 깁니다. 패스!");
                }

//                // 공공데이터 주소가 포함되어 있는지 확인 (공백 제거 후 비교)
//                String cleanAddr = addr.replace(" ", "");
//                log.info(">>> 검색 키워드: {}, 장소명: {}, 카카오 주소: [{}], 공공데이터 주소: [{}]", keyWord, scrpTrgt.getPblcDataPlacNm(), addr, tmpPblcDataAddr);
//                if (!cleanAddr.contains(tmpPblcDataAddr)) {
//                    log.warn(">>> [패스] 주소 불일치 scrpTrgtId: {}", scrpTrgt.getScrpTrgtId());
//                    isNextScrpTrgt = true;
//                }
//                log.info(">>> [성공] 주소 일치 상세정보 수집 시작");

                // 공공데이터 주소가 포함되어 있는지 확인 (공백 제거 후 비교)
                String cleanAddr = addr.replace(" ", "");
                log.info(">>> 검색 키워드: {}, 장소명: {}, 카카오 주소: [{}], 공공데이터 주소: [{}]", keyWord, scrpTrgt.getPblcDataPlacNm(), addr, tmpPblcDataAddr);

                if (!cleanAddr.contains(tmpPblcDataAddr)) {
                    log.warn(">>> [패스] 주소 불일치 scrpTrgtId: {}", scrpTrgt.getScrpTrgtId());
                    // 다음 검색 결과 목록으로 패스
                    return false;
                }
                log.info(">>> [성공] 주소 일치 상세정보 수집 시작");

                // ------------------------------------------------------------------
                // 상세 정보(외부ID, 장소명, 전화번호 등) 추출 및 DB INSERT
                // ------------------------------------------------------------------
                String currentUrl = currentDriver.getCurrentUrl();
                if (currentUrl.contains("/place/")) {
                    String[] parts = currentUrl.split("/place/");
                    if (parts.length > 1) {
                        // 현재 브라우저의 URL에서 외부ID(네이버맵 ID) 추출
                        extlPlacId = parts[1].split("/|\\?")[0];
                    }
                }
                // ID가 없으면 저장하지 않고 스킵
                if (extlPlacId == null || extlPlacId.isEmpty()) {
                    updateSkipStatus(scrpTrgt.getScrpTrgtId());
                    log.error(">>> [" + scrpTrgt.getPblcDataPlacNm() + "] 외부ID를  찾을 수 없어 건너뜁니다.");
                    isNextScrpTrgt = true;
                }

                List<WebElement> placNmEls = currentDriver.findElements(By.cssSelector(".GHAhO"));
                if (!placNmEls.isEmpty()) {
                    placNm = placNmEls.get(0).getText().trim();
                }

                List<WebElement> telEls = currentDriver.findElements(By.cssSelector(".xlx7Q"));
                if (!telEls.isEmpty()) {
                    telNo = telEls.get(0).getText().trim();
                }

            } catch (Exception e) {
                log.error(">>> 상세 정보 추출 작업 실패: " + keyWord + " | " + e.getMessage());
                isNextScrpTrgt = false;
                return isNextScrpTrgt;
            }
            // 데이터 세팅 및 DB 저장
            BCH00000202IN insertParam = new BCH00000202IN();
            insertParam.setExtlPlacId(extlPlacId);
            insertParam.setPlacNm(placNm);
            insertParam.setAddr(addr);
            insertParam.setTelNo(telNo);
            insertParam.setSorcDvcd("NAV");
            insertParam.setCtgrDvcd("RST");

            try {
                int insertResult = daoBCH000001.insertPlac(insertParam);

                if (insertResult > 0) {
                    insertCount += insertResult;
                    log.info(">>> [NAV] 장소 테이블에 INSERT완료. 수집대상ID: {}, 장소명: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm());
                } else {
                    throw new RuntimeException(String.format("INSERT 결과가 %d건입니다.", insertResult));
                }

                BCH00000201IN updateParam = new BCH00000201IN();
                try {
                    updateParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                    updateParam.setProgStatCd("02");
                    // 진행 상태코드 변경 01: 네이버 진행중 -> 02: 네이버 완료
                    int result = daoBCH000001.updateScrpTrgtStat(updateParam);

                    if (result == 0) {
                        throw new RuntimeException("DB에 업데이트 대상이 없습니다");
                    }
                    log.info(">>> [NAV] 진행상태 업데이트 완료. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd());
                } catch (Exception e) {
                    log.error(">>> [NAV] 진행상태 업데이트 실패. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}, | 에러: {}\n 다음가게로 이동...", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd(), e.getMessage());
                }

                isNextScrpTrgt = true;
                log.info(">>> [성공] 수집대상ID: {}, 관리번호: {}, 장소명: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataMngNo(), scrpTrgt.getPblcDataPlacNm());
            } catch (DuplicateKeyException e) {
                // 중복도 수집된 것으로 간주
                log.info(">>> [중복] 이미 수집된 가게입니다: " + scrpTrgt.getPblcDataPlacNm());
            } catch (Exception e) {
                log.error(">>> [NAV] DB 작업 중 예외 발생. ID: {} | 에러: {}", scrpTrgt.getScrpTrgtId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error(">>> [NAV] 상세정보 수집 중 예외 발생. 다음 가게로 건너뜁니다. ID: {} | 에러: {}", scrpTrgt.getScrpTrgtId(), e.getMessage());
            isNextScrpTrgt = true;
        }

        return isNextScrpTrgt;
    }

    /**
     * 브라우저를 새로 띄우는 공통 메서드 (이미지 차단 등 성능 옵션 포함)
     */
    private WebDriver initNewDriver() {
        ChromeOptions options = new ChromeOptions();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);      // 이미지 차단
        prefs.put("profile.managed_default_content_settings.stylesheets", 2); // CSS(Style) 차단
        prefs.put("profile.managed_default_content_settings.fonts", 2);       // 폰트 차단
        options.setExperimentalOption("prefs", prefs);

        // 기타 필요한 옵션들
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return new ChromeDriver(options);
    }

    /**
     * 검색 결과가 없거나 제외업종일 때 상태를 업데이트 (무한 재시도 방지)
     */
    private void updateSkipStatus(String scrpTrgtId) {
        try {
            BCH00000201IN updateParam = new BCH00000201IN();
            updateParam.setScrpTrgtId(scrpTrgtId);

            updateParam.setProgStatCd("01");

            int result = daoBCH000001.updateScrpTrgtStat(updateParam);

            skipCount += result;

            log.info(">>> [NAV] 수집 불가 대상 스킵 처리 완료. ID: {}, 변경상태: {}", scrpTrgtId, updateParam.getProgStatCd());
        } catch (Exception e) {
            log.error(">>> [NAV] 스킵 상태 업데이트 실패. ID: {} | 에러: {}", scrpTrgtId, e.getMessage());
        }
    }
}
