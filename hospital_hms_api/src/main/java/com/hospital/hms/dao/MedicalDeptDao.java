package com.hospital.hms.dao;
import com.hospital.hms.pojo.MedicalDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface MedicalDeptDao {


    /*
    * 分页查询接口
    * */
    List<HashMap<String, Object>> selectConditionByPage(Map<String, Object> param);

    /*
    * 查询出满足条件的总记录数
    * */
    long selectConditionByPageCount(Map param);

    ArrayList<HashMap> selectDeptAndSub();

    ArrayList<HashMap> selectAllDeptNameAndId();


    /**
     * 添加科室
     * */
    void insert(MedicalDept entity);

    HashMap selectById(int id);

    void update(MedicalDept medicalDept);

    long selectSubCountByIds(Integer[] ids);

    void deleteByIds(Integer[] ids);

    ArrayList<HashMap> selectMedicalDeptList(Map param);
}