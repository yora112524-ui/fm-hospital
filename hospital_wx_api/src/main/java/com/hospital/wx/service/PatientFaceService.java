package com.hospital.wx.service;

import java.util.Map;

public interface PatientFaceService {
    boolean checkFaceAuthRecordForToday(Map param);

    void createFaceModel(Map param);

    boolean verifyFaceModel(int userId, String photo);


}
