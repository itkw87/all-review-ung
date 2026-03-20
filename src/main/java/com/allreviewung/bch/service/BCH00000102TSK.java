package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.service.svo.BCH00000201IN;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try {
            log.info(">>> 전체 수집을 시작합니다.");

            // WebDriverManager 사용하여 크롬 드라이버 자동 세팅
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("profile.managed_default_content_settings.images", 2);      // 이미지 차단
            prefs.put("profile.managed_default_content_settings.stylesheets", 2); // CSS(Style) 차단
            prefs.put("profile.managed_default_content_settings.fonts", 2);       // 폰트 차단

            options.setExperimentalOption("prefs", prefs);
            // 아예 창을 안 띄우기
            // options.addArguments("--headless");

            driver = new ChromeDriver(options);

            // 네이버맵 리뷰 수집
//                navBCH00000101.collect(driver);

            // 카카오맵 리뷰 수집
            kkoBCH00000101.collect(driver);
            log.info(">>> 전체수집 완료!");
        } catch (Exception e) {
            log.error(">>> 전체수집 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                // 크롤링 끝나면 브라우저 닫기
                driver.quit();
            }
        }
        // 작업 완료 신호
        return RepeatStatus.FINISHED;
    }
}