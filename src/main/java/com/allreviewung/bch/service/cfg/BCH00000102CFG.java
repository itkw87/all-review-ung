package com.allreviewung.bch.service.cfg;

import com.allreviewung.bch.service.BCH00000102TSK;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BCH00000102CFG {

    private final BCH00000102TSK tskBCH00000102;

    @Bean
    public Job jobBCH00000102(JobRepository jobRepository, Step stepBCH00000102) {
        return new JobBuilder("BCH00000102CFG", jobRepository)
                .start(stepBCH00000102)
                .build();
    }

    @Bean
    public Step stepBCH00000102(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("BCH00000102_STEP1", jobRepository)
                .tasklet(tskBCH00000102, platformTransactionManager)
                .build();
    }
}