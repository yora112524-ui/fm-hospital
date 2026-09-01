package com.hospital.hms.service;

import cn.hutool.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DoctorWorkPlanService {
    JSONArray selectWorkPlanByTime(Map param, List dateList);

    String insert(Map param);

    ArrayList<String> selectCanRegisterInDateRange(Map param);

    ArrayList<HashMap> selectDeptSubDoctorPlanInDay(Map param);

    String checkRegisterCondition(Map param);
}
