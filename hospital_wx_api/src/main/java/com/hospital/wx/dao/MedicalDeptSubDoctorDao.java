package com.hospital.wx.dao;
import com.hospital.wx.pojo.MedicalDeptSubDoctor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Mapper
public interface MedicalDeptSubDoctorDao {

    @Transactional
    public void insert(MedicalDeptSubDoctor medicalDeptSubDoctor);

    public void updateDoctorSubDept(Map param);

    public void deleteByDoctorId(Integer[] ids);

}