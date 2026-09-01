package com.hospital.hms.service;
import com.hospital.hms.pojo.DoctorWorkPlanSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorWorkPlanScheduleService {
    void insert(ArrayList<DoctorWorkPlanSchedule> list);

    ArrayList selectDoctorScheduleByDeptSubIdAndDate(Map param);

    HashMap selectDoctorScheduleByWorkPlanId(int workPlanId);

    void updateSchedule(Map param);

    void deleteByWorkPlanId(int workPlanId);

    void deleteWorkPlan(int workPlanId);

    ArrayList<HashMap> selectDoctorWorkPlanSchedule(Map param);
}
