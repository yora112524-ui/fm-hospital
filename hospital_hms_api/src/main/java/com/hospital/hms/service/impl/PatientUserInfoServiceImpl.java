package com.hospital.hms.service.impl;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.dao.PatientUserInfoDao;
import com.hospital.hms.pojo.PatientUserInfo;
import com.hospital.hms.service.PatientUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
public class PatientUserInfoServiceImpl implements PatientUserInfoService {

    @Autowired
    private PatientUserInfoDao patientUserInfoDao;

    @Override
    @Transactional
    public void insert(PatientUserInfo patientUserInfo) {
        patientUserInfoDao.insert(patientUserInfo);
    }

    @Override
    public HashMap selectPatientUserInfoByUserId(int userId) {
        //查询患者信息
        HashMap map = patientUserInfoDao.selectPatientUserInfoByUserId(userId);
        if (map != null) {
            //将medicalHistory转换为json数组
            String medicalHistory = MapUtil.getStr(map, "medicalHistory");
            JSONArray array = JSONUtil.parseArray(medicalHistory);
            map.replace("medicalHistory", array);
        }
        return map;
    }

    @Override
    @Transactional
    public void update(PatientUserInfo patientUserInfo) {
        patientUserInfoDao.update(patientUserInfo);
    }

    @Override
    public Boolean userExists(int userId) {
        Integer id = patientUserInfoDao.userExists(userId);
        return id != null;
    }
}
