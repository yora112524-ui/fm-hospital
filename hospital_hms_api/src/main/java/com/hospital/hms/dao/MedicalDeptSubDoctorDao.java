package com.hospital.hms.dao;
import com.hospital.hms.pojo.MedicalDeptSubDoctor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Mapper
public interface MedicalDeptSubDoctorDao {

    void insert(MedicalDeptSubDoctor medicalDeptSubDoctor);

    void updateDoctorSubDept(Map param);

    void deleteByDoctorId(Integer[] ids);

}