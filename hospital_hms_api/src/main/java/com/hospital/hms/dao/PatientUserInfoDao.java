package com.hospital.hms.dao;
import com.hospital.hms.pojo.PatientUserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Map;

@Mapper
public interface PatientUserInfoDao {
    String selectTelById(int userId);

    int insert(PatientUserInfo patientUserInfo);
    HashMap selectPatientUserInfoByUserId(int userId);

    int update(PatientUserInfo entity);

    Integer userExists(int userId);

    Boolean existsFaceModel(int userId);

    void updateExistFaceModel(Map param);

    Integer selectIdByUserId(int userId);
}