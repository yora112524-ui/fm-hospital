package com.hospital.wx.dao;
import com.hospital.wx.pojo.DoctorWorkPlan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorWorkPlanDao {
    ArrayList<HashMap> selectWorkPlanByTime(Map param);

    Integer selectIdByDoctorAndSubId(Map param);

    void insert(DoctorWorkPlan doctorWorkPlan);

    void updateMaximum(Map param);

    Integer selectNumById(int id);
    void deleteById(int id);

    ArrayList<String> selectDoctorScheduleBySubIdAndDate(Map param);
    ArrayList<HashMap> getDoctorShiftDetailsBySubIdAndDate(Map param);

    int updateNumById(Map param);

    int releaseRegistrationLock(String outTradeNo);



}