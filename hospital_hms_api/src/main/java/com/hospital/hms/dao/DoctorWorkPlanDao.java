package com.hospital.hms.dao;
import com.hospital.hms.pojo.DoctorWorkPlan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorWorkPlanDao {


    Integer checkDoctorWorkPlanForToday(Map param);

    void insert(DoctorWorkPlan doctorWorkPlan);

    ArrayList<HashMap> selectWorkPlanByTime(Map param);

    void updateMaximum(Map param);

    Integer selectActualRegistrationCount(int id);

    void deleteById(int id);

    ArrayList<String> selectCanRegisterInDateRange(Map param);

    ArrayList<HashMap> selectDeptSubDoctorPlanInDay(Map param);

    int updateNumById(Map param);

    int releaseNumByOutTradeNo(String outTradeNo);
}