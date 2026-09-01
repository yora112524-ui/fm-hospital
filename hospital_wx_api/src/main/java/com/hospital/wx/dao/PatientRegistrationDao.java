package com.hospital.wx.dao;
import com.hospital.wx.pojo.PatientRegistration;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 患者挂号Dao业务
 * */
@Mapper
public interface PatientRegistrationDao {

    long getTodayRegistrationCount(Map param);
    Integer hasRegisteredToday(Map param);

    int insert(PatientRegistration patientRegistration);

//    int cancelUnpaidAppointments(String outTradeNo);

    HashMap selectWorkPlanIdAndScheduleId(String outTradeNo);

    long selectRegistrationCount(Map param);
    ArrayList<HashMap> selectRegistrationByPage(Map param);

    HashMap selectRegistrationInfo(Map param);

    HashMap searchRepayInfo(Map param);
    int updateRepayInfo(Map param);

    int closeUnpaidOrder(String outTradeNo);

    HashMap getDoctorScheduleIdByOutTradeNo(String outTradeNo);


    //更新付款状态
    int updatePaymentStatus(Map param);


}