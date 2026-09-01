package com.hospital.wx.service;
import com.hospital.wx.pojo.MedicalDeptSub;
import com.hospital.wx.utils.PageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MedicalDeptSubService {

    public PageUtils selectConditionByPage(Map param);

    public void insert(MedicalDeptSub medicalDeptSub);

    public HashMap selectById(int id);

    public void update(MedicalDeptSub medicalDeptSub);

    public void deleteByIds(Integer[] ids);

    ArrayList<HashMap> selectMedicalDeptSubList(int deptId);

}
