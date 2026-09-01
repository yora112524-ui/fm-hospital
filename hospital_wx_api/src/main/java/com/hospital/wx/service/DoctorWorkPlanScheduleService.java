package com.hospital.wx.service;
import com.hospital.wx.pojo.DoctorWorkPlanSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorWorkPlanScheduleService {

    ArrayList<HashMap> selectDoctorWorkPlanSchedule(Map param);
    public void insert(ArrayList<DoctorWorkPlanSchedule> list);

    public ArrayList selectScheduleByDeptSub(Map param);

    public HashMap selectScheduleByWorkPlanId(int workPlanId);

    public void updateSchedule(Map param);

    public void deleteByWorkPlanId(int workPlanId);




}
