package com.hospital.wx.service;

import cn.hutool.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorWorkPlanService {

    ArrayList<String> selectDoctorScheduleBySubIdAndDate(Map param);

    ArrayList<HashMap> getDoctorShiftDetailsBySubIdAndDate(Map param);
}
