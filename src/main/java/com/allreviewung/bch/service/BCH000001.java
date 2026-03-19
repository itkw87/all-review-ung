package com.allreviewung.bch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BCH000001 {

    private final JobLauncher jobLauncher;
    private final Job jobBCH00000101;
    private final Job jobBCH00000102;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @GetMapping("/test/public-data")
    public String runPublicDataBatch() {
        try {
            log.info(">>> 행정안전부 식품 일반음식점 수집을 시작합니다.");

            // 새로 만든 공공데이터 수집 Job (BCH00000201) 실행
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(jobBCH00000101, params); // 공공데이터 전용 Job
            return "공공데이터 수집 시작 성공";
        } catch (Exception e) {
            log.error("수집 실패", e);
            return "실패: " + e.getMessage();
        }
    }

    @GetMapping("/test/bch")
    public String runBatch() {
        if (isRunning.get()) {
            log.warn(">>> [알림] 이미 배치가 실행 중입니다. 잠시 후 다시 시도하세요.");
            return "이미 배치가 실행 중입니다.";
        }

        // 실행상태 false면 true로 변경(true반환)
        if (isRunning.compareAndSet(false, true)) {
            try {
                log.info(">>> 배치 실행을 시작합니다.");
                JobParameters params = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(jobBCH00000102, params);
                return "배치 실행 성공";

            }
            catch (Exception e) {
                e.printStackTrace();
                return "배치 실패: " + e.getMessage();
            }
            finally {
                isRunning.set(false);
                log.info("배치가 종료 되었습니다.");
            }
        }
        return "배치 실행 요청 실패";
    }
}