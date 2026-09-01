package com.hospital.hms.dao;
import com.hospital.hms.pojo.FaceAuth;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface PatientFaceDao {

     public Integer hasFaceAuthInDay(Map param);

     public int insert(FaceAuth entity);


}