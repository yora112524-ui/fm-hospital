package com.hospital.hms.dao;

import com.hospital.hms.pojo.MedicalDeptSub;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface MedicalDeptSubDao {

    public ArrayList<HashMap> selectConditionByPage(Map param);
    public long selectConditionByPageCount(Map param);

    public void insert(MedicalDeptSub medicalDeptSub);

    public HashMap selectById(Integer id);

    public void update(MedicalDeptSub entity);

    public long selectIncludeDoctorCount(Integer[] ids);

    public void deleteByIds(Integer[] ids);

    public ArrayList<HashMap> selectMedicalDeptSubList(int deptId);

}