package com.hospital.wx.service.impl;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hospital.wx.dao.DoctorWorkPlanDao;
import com.hospital.wx.dao.PatientRegistrationDao;
import com.hospital.wx.dao.PatientUserInfoDao;
import com.hospital.wx.exception.GlobalException;
import com.hospital.wx.exception.WxErrorCode;
import com.hospital.wx.pojo.DoctorWorkPlan;
import com.hospital.wx.pojo.DoctorWorkPlanSchedule;
import com.hospital.wx.service.DoctorWorkPlanScheduleService;
import com.hospital.wx.service.DoctorWorkPlanService;
import com.hospital.wx.service.PatientFaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class DoctorWorkPlanServiceImpl implements DoctorWorkPlanService {

    @Autowired
    private DoctorWorkPlanDao doctorWorkPlanDao;

    @Autowired
    private DoctorWorkPlanScheduleService doctorWorkPlanScheduleService;



    @Autowired
    PatientUserInfoDao patientUserInfoDao;

    @Autowired
    PatientFaceService patientFaceService;

    /*
    * 根据诊室ID+日期查询医生出诊计划信息
    * */
    @Override
    public ArrayList<String> selectDoctorScheduleBySubIdAndDate(Map param) {
        /**
         * 获取医生排班列表(结果是根据数据库的实际日期结果返回的)
         * list =>
         * ["2025-02-21","2025-02-22","2025-02-23"]
         * */
        ArrayList<String> list = doctorWorkPlanDao.selectDoctorScheduleBySubIdAndDate(param);
//        String jsonStr = JSONUtil.toJsonStr(list);
//        System.out.println(jsonStr);
        //解析出startDate和endDate
        DateTime startDate = DateUtil.parse(MapUtil.getStr(param, "startDate"));
        DateTime endDate = DateUtil.parse(MapUtil.getStr(param, "endDate"));
        /**
         *
         * 基于startDate-endDate生成日期范围
         * 例如：  2024-05-07 - 2024-05-13
         * */
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        //创建结果集
        ArrayList result = new ArrayList();
        /**
         * 遍历range生成最终结果数据。
         * */
        while (range.hasNext()) {
            String date = range.next().toDateStr();
            if (list.contains(date)) {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "出诊");
                }});
            } else {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "无号");
                }});
            }
        }
        /*
         * [
         *   {"date":"2025-02-21","status":"出诊"},
         *   {"date":"2025-02-22","status":"出诊"},
         *   {"date":"2025-02-23","status":"出诊"},
         *   {"date":"2025-02-24","status":"无号"},
         *   {"date":"2025-02-25","status":"无号"},
         *   {"date":"2025-02-26","status":"无号"},
         *   {"date":"2025-02-27","status":"无号"}]
         *]
         * */
        return result;
    }

    @Override
    public ArrayList<HashMap> getDoctorShiftDetailsBySubIdAndDate(Map param) {
        return doctorWorkPlanDao.getDoctorShiftDetailsBySubIdAndDate(param);
    }


//
//    public String checkRegisterCondition(Map param) {
//
//        /**
//         * 条件1：检查当天用户是否已经挂号3次以上
//         * */
//        //获取当前系统时间
//        param.put("today", DateUtil.today());
//        long count = patientRegistrationDao.getTodayRegistrationCount(param);
//        if (count >= 3) {
//            return WxErrorCode.DAILY_REGISTRATION_LIMIT_REACHED.toString();
//        }
//
//        //条件2：检查当天是否已经挂过该门诊的号
//        Integer id = patientRegistrationDao.hasRegisteredToday(param);
//        if (id != null) {
//            return WxErrorCode.ROOM_ALREADY_REGISTERED.toString();
//        }
//
//        /**
//         * 条件3： 检查是否存在人脸面部数据（是否采集过人脸数据）
//         * 如果没有满足条件3前端会跳转到人脸信息采集页面让患者进行人脸录入
//         * */
//        int userId = MapUtil.getInt(param, "userId");
//        Boolean bool = patientUserInfoDao.existsFaceModel(userId);
//        if (bool == null || !bool) {
//            return WxErrorCode.FACE_MODEL_NOT_FOUND.toString();
//        }
//
//        /**
//         * 条件4：检查今日是否存在挂号用户的面部识别记录
//         * 每次挂号的时候当天需要进行人脸验证才可以.
//         * 有的是要求之前做过人脸验证就行了
//         * 如果当天用户做过人脸识别在会patient_face_auth里面保存一条记录
//         * 如果没有满足条件4: 前端会跳转到人脸验证的页面让患者进行人脸验证.
//         * */
//        bool = patientFaceService.hasFaceAuthInDay(param);
//        if (!bool) {
//            return WxErrorCode.NO_FACE_VERIFICATION_RECORD.toString();
//        }
//        return WxErrorCode.REGISTRATION_ELIGIBLE.toString();
//    }
}