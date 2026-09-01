package com.hospital.hms.dao;
import com.hospital.hms.pojo.DoctorWorkPlanSchedule;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorWorkPlanScheduleDao {
    
    void insert(DoctorWorkPlanSchedule entity);

    ArrayList<HashMap> selectDoctorScheduleByDeptSubIdAndDate(Map param);

    ArrayList<HashMap> selectDoctorScheduleByWorkPlanId(int workPlanId);

    long selectSumNumByIds(ArrayList<Integer> ids);

    void deleteByIds(ArrayList<Integer> ids);

    void deleteByWorkPlanId(int workPlanId);

    ArrayList<HashMap> selectDoctorWorkPlanSchedule(Map param);

    int updateNumById(Map param);

    int releaseNumByOutTradeNo(String outTradeNo);
}