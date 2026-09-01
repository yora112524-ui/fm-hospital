package com.hospital.wx.service;

import com.hospital.wx.pojo.MedicalDept;
import com.hospital.wx.utils.PageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MedicalDeptService {
    HashMap selectDeptAndSub();

    ArrayList<HashMap> selectAll();

    PageUtils selectConditionByPage(Map param);

    void insert(MedicalDept medicalDept);

    HashMap selectById(int id);

    void update(MedicalDept medicalDept);

    void deleteByIds(Integer[] ids);

    ArrayList<HashMap> selectMedicalDeptList(Map param);

}
