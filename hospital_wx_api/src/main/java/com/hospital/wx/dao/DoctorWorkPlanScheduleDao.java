package com.hospital.wx.dao;
import com.hospital.wx.pojo.DoctorWorkPlanSchedule;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorWorkPlanScheduleDao {


    ArrayList<HashMap> selectDoctorWorkPlanSchedule(Map param);

    
    void insert(DoctorWorkPlanSchedule entity);

    ArrayList<HashMap> selectScheduleByDeptSub(Map param);

    ArrayList<HashMap> selectScheduleByWorkPlanId(int workPlanId);

    long selectSumNumByIds(ArrayList<Integer> ids);

    void deleteByIds(ArrayList<Integer> ids);

    void deleteByWorkPlanId(int workPlanId);



    int updateNumById(Map param);

    int releaseRegistrationLock(String outTradeNo);
}