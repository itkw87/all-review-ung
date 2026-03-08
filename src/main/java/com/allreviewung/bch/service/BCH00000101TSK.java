package com.allreviewung.bch.service;

import com.allreviewung.bch.dao.BCH000001DAO;
import com.allreviewung.bch.service.svo.BCH00000101IN;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor // Autowired 대신 생성자 주입이 2년 차의 정석!
public class BCH00000101TSK implements Tasklet {

  private final BCH000001DAO daoBCH000001;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    for (int i = 0; i < 5; i++) {
      BCH00000101IN inParam = new BCH00000101IN();

      inParam.setExtlRevwId(System.currentTimeMillis());
      inParam.setSorcDvcd("NAV");
      inParam.setCntn("진짜 맛있어요! " + i);
      inParam.setRtng(4.5);
      inParam.setPlacId(12345);
      inParam.setRegId("BCH00000101CFG");
      inParam.setRegDtm(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
      inParam.setChngId("BCH00000101CFG");
      inParam.setChngDtm(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

      // DB 입력
      daoBCH000001.insertExtlRevw(inParam);
    }

    // 작업 완료 신호
    return RepeatStatus.FINISHED;
  }
}