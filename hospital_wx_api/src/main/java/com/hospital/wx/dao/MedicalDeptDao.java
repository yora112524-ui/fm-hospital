package com.hospital.wx.dao;
import com.hospital.wx.pojo.MedicalDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface MedicalDeptDao {
    ArrayList<HashMap> selectDeptAndSub();

    ArrayList<HashMap> selectAll();

    ArrayList<HashMap> selectConditionByPage(Map param);

    long selectConditionByPageCount(Map param);

    void insert(MedicalDept entity);

    HashMap selectById(int id);

    void update(MedicalDept medicalDept);

    long selectSubCountByIds(Integer[] ids);

    void deleteByIds(Integer[] ids);

    ArrayList<HashMap> selectMedicalDeptList(Map param);

}