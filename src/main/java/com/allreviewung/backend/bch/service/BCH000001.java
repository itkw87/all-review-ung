package com.allreviewung.backend.bch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BCH000001 {

  private final JobLauncher jobLauncher;
  private final Job jobBCH000001;

  @GetMapping("/test/bch")
  public String runBatch() {
    try {

      JobParameters params = new JobParametersBuilder()
              .addLong("time", System.currentTimeMillis())
              .toJobParameters();

      jobLauncher.run(jobBCH000001, params);

      return "배치 실행 성공";

    } catch (Exception e) {
      e.printStackTrace();
      return "배치 실패: " + e.getMessage();
    }
  }
}