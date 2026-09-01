package com.hospital.hms.dao;
import com.hospital.hms.pojo.PatientRegistration;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 患者挂号Dao业务处理
 * */
@Mapper
public interface PatientRegistrationDao {

    public long selectRegistrationCountInToday(Map param);
    public Integer existsRegisterRecordInDay(Map param);

    public int insert(PatientRegistration patientRegistration);

    public int cancelUnpaidAppointments(String outTradeNo);

    public HashMap selectWorkPlanIdAndScheduleId(String outTradeNo);


}