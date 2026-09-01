package com.hospital.hms.dao;

import com.hospital.hms.pojo.DoctorConsultationReport;
import org.apache.ibatis.annotations.Mapper;
import java.util.HashMap;
import java.util.Map;


@Mapper
public interface DoctorConsultationReportDao {

    void insert(DoctorConsultationReport entity);
    HashMap selectConsultationReportByRegistrationId(Map param);
}
