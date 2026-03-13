package com.allreviewung.bch.service.cfg;

import com.allreviewung.bch.service.BCH00000101TSK;
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
public class BCH00000101CFG {

    private final BCH00000101TSK tskBCH00000101;

    @Bean
    public Job jobBCH000001(JobRepository jobRepository, Step stepBCH000001) {
        return new JobBuilder("BCH00000101CFG", jobRepository)
                .start(stepBCH000001)
                .build();
    }

    @Bean
    public Step stepBCH000001(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder("BCH000001_STEP1", jobRepository)
                .tasklet(tskBCH00000101, platformTransactionManager)
                .build();
    }
}