package com.hospital.hms.service;
import java.util.HashMap;

public interface PatientUserService {
    public HashMap loginOrRegister(String code, String nickname, String photo, String sex);

    public HashMap selectUserInfoById(int userId);
}
