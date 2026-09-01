package com.hospital.hms.service;

import java.util.Map;

public interface PatientFaceService {
    public boolean hasFaceAuthInDay(Map param);

    public void createFaceModel(Map param);

    public boolean verifyFaceModel(int userId, String photo);


}
