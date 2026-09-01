package com.hospital.wx.dao;

import com.hospital.wx.pojo.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorDao {

    HashMap selectDoctorInfoById(int id);


    public ArrayList<HashMap> selectConditionByPage(Map param);

    public long selectConditionByPageCount(Map param);

    public HashMap selectDoctorDetailById(int id);

    @Transactional
    public void insert(Doctor entity);
    public Integer selectIdByUUID(String uuid);
    public void updatePicture(Map map);

    public HashMap selectById(int id);

    public void update(Map map);

    public void deleteByIds(Integer[] ids);

    public ArrayList<HashMap> selectDoctorBySubId(int subId);



    /**
     * 查询需要缓存的医生信息
     * */
    HashMap getDoctorDetailsForCache(int doctorId);

}
