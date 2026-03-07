package com.allreviewung.backend.bch.dao;

import com.allreviewung.backend.bch.service.svo.BCH00000101IN;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BCH000001DAO {

  int insertExtlRevw(BCH00000101IN inParam);

}
