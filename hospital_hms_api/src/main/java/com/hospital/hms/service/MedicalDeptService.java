package com.hospital.hms.service;
import com.hospital.hms.pojo.MedicalDept;
import com.hospital.common.utils.PageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MedicalDeptService {
    /*
    * 分页查询接口
    * */
    PageUtils selectConditionByPage(Map param);

    HashMap selectDeptAndSub();

    ArrayList<HashMap> selectAllDeptNameAndId();



    void insert(MedicalDept medicalDept);

    HashMap selectById(int id);

    void update(MedicalDept medicalDept);

    void deleteByIds(Integer[] ids);

    public ArrayList<HashMap> selectMedicalDeptList(Map param);

}
