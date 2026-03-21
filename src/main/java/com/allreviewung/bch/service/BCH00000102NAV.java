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

    private final BCH000001DAO daoBCH000001;
    private static final List<String> EXCLUDE_CATEGORIES = Arrays.asList("카페", "디저트", "커피");

    public void collect(WebDriver webDriver) {
        WebDriver currentDriver = webDriver;
        int restartInterval = 50; // 50건마다 재시작
        String keyWord = "";
        Duration durationSeconds = Duration.ofSeconds(2);
        try {
            // 네이버 지도로 이동
            currentDriver.get("https://map.naver.com/");

            // 검색창이 화면에 나타날 때까지 최대 대기시간 지정(Duration.ofSeconds)
//            WebDriverWait wait = new WebDriverWait(currentDriver, Duration.ofSeconds(5));
            WebDriverWait wait = new WebDriverWait(currentDriver, durationSeconds);
//            WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));

            List<BCH00000101DTO> scrpTrgtList = daoBCH000001.selectScrpTrgtList();

            if (scrpTrgtList != null && !scrpTrgtList.isEmpty()) {
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
                    double progress = (double) (i + 1) / scrpTrgtList.size() * 100;
                    log.info(">>> 네이버맵 크롤링 진행률: {}% 진행건수: {}/{}건", String.format("%.2f", progress), i + 1, scrpTrgtList.size());

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

                    WebElement searchInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input.input_search")));

                    // 키워드 초기화
                    searchInput.click(); // 일단 클릭해서 커서를 갖다 대고
                    searchInput.sendKeys(Keys.CONTROL + "a"); // 전체 선택 (Ctrl+A)
                    searchInput.sendKeys(Keys.BACK_SPACE);    // 지우기 (Backspace)

                    searchInput.sendKeys(keyWord);
                    searchInput.sendKeys(Keys.ENTER);
                    Thread.sleep(1000); // 검색 결과 로딩 대기

                    boolean hasNextPage = true;
                    boolean isNextScrpTrgt = false;

                    // ------------------------------------------------------------------
                    // 페이징 리스트 순회
                    // ------------------------------------------------------------------
                    while (hasNextPage) {
                        currentDriver.switchTo().defaultContent();

                        boolean isDirectEntry = false;
                        try {
                            // presenceOfElementLocated로 일단 DOM에 생길 때까지 대기
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));
                            isDirectEntry = true;
                        } catch (Exception e) {
                            log.info(">>> [NAV] Duration.ofSeconds 대기 후 상세 프레임 미발견. 리스트 모드로 전환합니다.");
                            isDirectEntry = false;
                        }

                        if (isDirectEntry) {
                            // ==================================================================
                            // [단건 케이스] 앤드와플처럼 상세가 바로 뜬 경우
                            // ==================================================================
                            currentDriver.switchTo().frame("entryIframe");
                            log.info(">>> [NAV] 단건 검색 결과 바로 진입");

                            // [단건 최종 수집] 메서드 안에서 defaultContent()로 빠져나가는 로직이 있으므로 그대로 호출
                            isNextScrpTrgt = scrapeAndSaveDetail(currentDriver, wait, scrpTrgt, keyWord, tmpPblcDataAddr);

//                            // 단건 수집이 끝났으니, 다음 가게 검색을 위해 메인으로 이동
//                            currentDriver.switchTo().defaultContent();
                            break; // 단건 처리 끝났으니 while 탈출

                        } else {
                            // ==================================================================
                            // [다건 케이스] 검색 결과가 여러 개라 리스트가 뜬 경우
                            // ==================================================================
                            currentDriver.switchTo().frame("searchIframe");

                            try {
                                // 리스트 항목이 화면에 뜰 때까지 대기 (최대 Duration.ofSeconds)
                                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.UEzoS")));
                            } catch (Exception e) {
                                log.warn(">>> [NAV] 결과가 아예 없습니다. 패스합니다: {}", keyWord);
                                hasNextPage = false;
                                break; // 여기서 while문 탈출
                            }

//                            currentDriver.switchTo().frame("searchIframe");
//
//                            List<WebElement> checkList = currentDriver.findElements(By.cssSelector("li.UEzoS"));
//
//                            if (checkList.isEmpty()) {
//                                log.warn(">>> [NAV] 결과가 아예 없습니다. 패스합니다: {}", keyWord);
//                                hasNextPage = false;
//                                break;
//                            }
//
//                            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.UEzoS")));

                            // ------------------------------------------------------------------
                            // 스크랩 대상을 전부 브라우저에 뿌리기 위해 스크롤 내리기 작업 수행
                            // ------------------------------------------------------------------
                            log.info(">>> 검색 결과 전체 로딩을 위해 스크롤을 내립니다...");
                            WebElement scrollContainer = currentDriver.findElement(By.cssSelector("#_pcmap_list_scroll_container"));
                            long lastHeight = (long) ((JavascriptExecutor) currentDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);

                            while (true) {
                                ((JavascriptExecutor) currentDriver).executeScript("arguments[0].scrollTo(0, arguments[0].scrollHeight)", scrollContainer);
//                                Thread.sleep(1500); // 네이버 서버가 데이터를 줄 시간을 줌
                                Thread.sleep(800); // 네이버 서버가 데이터를 줄 시간을 줌
                                long newHeight = (long) ((JavascriptExecutor) currentDriver).executeScript("return arguments[0].scrollHeight", scrollContainer);
                                if (newHeight == lastHeight) break;
                                lastHeight = newHeight;
                            }

                            List<WebElement> tmpPlacList = currentDriver.findElements(By.cssSelector("li.UEzoS"));
                            List<WebElement> placList = new ArrayList<>();

                            log.info(">>> [스크롤 완료] 현재페이지 검색결과: " + tmpPlacList.size() + "건");

                            // ------------------------------------------------------------------
                            // 제외 업종을 제외한 나머지로 검색결과 리스트 재구성
                            // ------------------------------------------------------------------
                            for (WebElement tmpPlac : tmpPlacList) {
                                String title = tmpPlac.findElement(By.cssSelector(".TYaxT")).getText();
                                String category = "";

                                try {
                                    category = tmpPlac.findElement(By.cssSelector(".KCMnt")).getText();
                                } catch (Exception e) {
                                    log.info(">>> [NAV] 리스트에서 업종 안 보임: {}", title);
                                }

                                // 람다식 사용 위한 상수
                                final String finalCategory = category;
                                if (!finalCategory.isEmpty() && EXCLUDE_CATEGORIES.stream().anyMatch(word -> finalCategory.contains(word))) {
                                    log.info(">>> [제외업종] {} 제외", title);
                                    continue;
                                }
                                placList.add(tmpPlac);
                            }

                            if (placList.isEmpty()) {
                                log.warn(">>> [NAV] 유효한 검색 결과가 없습니다. (전부 제외업종이거나 결과 없음) 검색 키워드: {}, 수집대상ID: {}, 장소명: {}", keyWord, scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm());
                                hasNextPage = false;
                                continue;
                            }

                            BCH00000201IN updateParam = new BCH00000201IN();
                            try {
                                updateParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                                updateParam.setProgStatCd("03");
                                // 진행 상태코드 변경 02: 네이버 완료 -> 03: 카카오 진행중
                                int result = daoBCH000001.updateScrpTrgtStat(updateParam);

                                if (result == 0) {
                                    throw new RuntimeException("DB에 업데이트 대상이 없습니다");
                                }
                                log.info(">>> [NAV] 진행상태 업데이트 완료. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd());
                            } catch (Exception e) {
                                log.error(">>> [NAV] 진행상태 업데이트 실패. 수집대상ID: {}, 장소명: {}, 예정 진행상태 코드: {}, | 에러: {}\n 다음가게로 이동...", scrpTrgt.getScrpTrgtId(), scrpTrgt.getPblcDataPlacNm(), updateParam.getProgStatCd(), e.getMessage());
                                continue;
                            }
//
//                            if (placList.size() == 1) {
//                                log.info(">>> [NAV] 단건 검색 결과 바로 진입");
//                                isNextScrpTrgt = scrapeAndSaveDetail(currentDriver, wait, scrpTrgt, keyWord, tmpPblcDataAddr);
//                                hasNextPage = false;
//                                if (isNextScrpTrgt) break;
//                            }

                            // ------------------------------------------------------------------
                            // 검색결과 리스트 순회하면서 상세정보 수집 작업 수행
                            // ------------------------------------------------------------------
                            int listSize = placList.size();
                            for (int j = 0; j < listSize; j++) {
                                try {
                                    // 매번 메인 -> 검색프레임으로 이동
                                    currentDriver.switchTo().defaultContent();
                                    wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("searchIframe")));

                                    // 리스트를 현재 기준으로 다시 추출
                                    List<WebElement> freshList = currentDriver.findElements(By.cssSelector("li.UEzoS"));
                                    if (j >= freshList.size()) break;

                                    // 전체(plac)를 추출
                                    WebElement plac = freshList.get(j);

                                    // 상단에 가려지지 않게 화면 중앙으로 스크롤
                                    ((JavascriptExecutor) currentDriver).executeScript("arguments[0].scrollIntoView({block: 'center'});", plac);
                                    Thread.sleep(500); // 네이버가 리스트를 다시 그릴 시간 부여

                                    // 다시 한번 j번째 요소를 찾아서 클릭합니다. (그 사이 변경 대비)
                                    WebElement finalPlac = currentDriver.findElements(By.cssSelector("li.UEzoS")).get(j);
                                    try {
                                        ((JavascriptExecutor) currentDriver).executeScript("arguments[0].click();", finalPlac);
                                    } catch (Exception e) {
                                        finalPlac.click();
                                    }

                                    // 메인으로 나가서 상세 프레임이 생성여부 확인
                                    currentDriver.switchTo().defaultContent();
                                    try {
//                                        new WebDriverWait(currentDriver, Duration.ofSeconds(3))
//                                                .until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));

                                        new WebDriverWait(currentDriver, Duration.ofMillis(1500))
                                                .until(ExpectedConditions.presenceOfElementLocated(By.id("entryIframe")));

                                        // 상세프레임 존재시 수집 메서드 호출
                                        isNextScrpTrgt = scrapeAndSaveDetail(currentDriver, wait, scrpTrgt, keyWord, tmpPblcDataAddr);
                                    } catch (Exception e) {
                                        log.warn(">>> [클릭 실패] {}번째 가게 상세창이 열리지 않음. 다음 놈 시도. 키워드: {}", j, keyWord);
                                        continue;
                                    }

                                    if (isNextScrpTrgt) break;
                                } catch (Exception e) {
                                    log.error(">>> 루프 내부 오류 발생  키워드: {}, 에러메세지: {}", keyWord, e.getMessage());
                                    e.printStackTrace();
                                }
                            } // for (WebElement plac : placList)

                            // ------------------------------------------------------------------
                            // 페이징 처리
                            // ------------------------------------------------------------------
                            try {
                                currentDriver.switchTo().defaultContent();
                                currentDriver.switchTo().frame("searchIframe");

                                // '이전/다음' 버튼 역할을 하는 .eUTV2 클래스들을 모두 찾음
                                List<WebElement> pageBtns = currentDriver.findElements(By.cssSelector(".eUTV2"));

                                // 리스트의 마지막 버튼이 '다음페이지' 버튼임
                                if (!pageBtns.isEmpty()) {
                                    WebElement nextBtn = pageBtns.get(pageBtns.size() - 1);
                                    String isDisabled = nextBtn.getAttribute("aria-disabled");

                                    // 비활성화 상태가 아니면(false) 클릭해서 다음 페이지로!
                                    if ("false".equals(isDisabled)) {
                                        log.info(">>> 다음 페이지 버튼을 발견했습니다. 클릭합니다.");
                                        nextBtn.click();

                                        // 페이지가 완전히 로딩될 때까지 2~3초 넉넉히 대기
//                                        Thread.sleep(2500);
                                        Thread.sleep(1000);
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
                                log.info(">>> [탈출] 매칭되는 가게를 찾았으므로 다음 페이지를 확인하지 않고 다음 키워드로 넘어갑니다.");
                            }
                        }
                    } // while (hasNextPage)
                } // for (int i = 0; i < scrpTrgtList.size(); i++)
            } // if (scrpTrgtList != null && !scrpTrgtList.isEmpty())
        } catch (Exception e) {
            log.error(">>> [NAV 리뷰 수집중 오류 발생] 키워드: {} | 에러내용: {}", keyWord, e.getMessage());
        }
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
                // 상세페이지 이동때까지 잠깐 대기
//                Thread.sleep(1500);

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
                        // 제외 업종이면 상태 업데이트도 안 하고 그냥 false(혹은 다음으로 가라는 신호) 반환
                        return false;
                    }
                }
                // ------------------------------------------------------------------
                // [상태 업데이트] 검문소 통과했으니 이제 '03(진행중)'으로 변경
                // ------------------------------------------------------------------
                BCH00000201IN startParam = new BCH00000201IN();
                startParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                startParam.setProgStatCd("03");
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
                        log.error(">>> [" + scrpTrgt.getPblcDataPlacNm() + "] 주소를 찾을 수 없어 건너뜁니다.");
                        isNextScrpTrgt = true;
                    }
                } catch (Exception e) {
                    log.info(">>> [" + placNm + "] 주소 로딩 시간이 너무 깁니다. 패스!");
                }

                // 공공데이터 주소가 포함되어 있는지 확인 (공백 제거 후 비교)
                String cleanAddr = addr.replace(" ", "");
                log.info(">>> 검색 키워드: {}, 장소명: {}, 카카오 주소: [{}], 공공데이터 주소: [{}]", keyWord, scrpTrgt.getPblcDataPlacNm(), addr, tmpPblcDataAddr);
                if (!cleanAddr.contains(tmpPblcDataAddr)) {
                    log.warn(">>> [패스] 주소 불일치 scrpTrgtId: {}", scrpTrgt.getScrpTrgtId());
                    isNextScrpTrgt = true;
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

                if (insertResult == 0) {
                    throw new RuntimeException("INSERT 결과가 0건입니다.");
                }

                BCH00000201IN updateParam = new BCH00000201IN();
                try {
                    updateParam.setScrpTrgtId(scrpTrgt.getScrpTrgtId());
                    updateParam.setProgStatCd("04");
                    // 진행 상태코드 변경 03: 카카오 진행중 -> 04: 전체 완료
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
                // 중복도 수집된것으로 간주
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

        // [성능 꿀팁] 이미지 로딩을 차단하면 네이버 맵이 2배 더 빨라집니다.
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2);
        options.setExperimentalOption("prefs", prefs);

        // 기타 필요한 옵션들
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        return new ChromeDriver(options);
    }
}
