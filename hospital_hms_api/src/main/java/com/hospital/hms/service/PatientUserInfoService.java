package com.hospital.hms.service;
import com.hospital.hms.pojo.PatientUserInfo;

import java.util.HashMap;

public interface PatientUserInfoService {
    public void insert(PatientUserInfo patientUserInfo);

    public HashMap selectPatientUserInfoByUserId(int userId);

    public void update(PatientUserInfo entity);

    public Boolean userExists(int userId);
}
