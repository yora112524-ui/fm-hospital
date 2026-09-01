package com.hospital.hms.service;
import com.hospital.hms.pojo.MedicalDeptSub;
import com.hospital.hms.utils.PageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MedicalDeptSubService {

    public PageUtils selectConditionByPage(Map param);

    public void insert(MedicalDeptSub medicalDeptSub);

    public HashMap selectById(int id);

    public void update(MedicalDeptSub medicalDeptSub);

    public void deleteByIds(Integer[] ids);

    public ArrayList<HashMap> selectMedicalDeptSubList(int deptId);

}
