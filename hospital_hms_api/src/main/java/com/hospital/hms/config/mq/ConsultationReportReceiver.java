package com.hospital.hms.config.mq;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.dao.DoctorConsultationReportDao;
import com.hospital.hms.pojo.DoctorConsultationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Component
@RabbitListener(queues = {"consultationReport"})
@Slf4j
public class ConsultationReportReceiver {

    @Resource
    private DoctorConsultationReportDao doctorConsultationReportDao;


    @RabbitHandler
    @Transactional
    public void receive(String msg) {
        JSONObject json = JSONUtil.parseObj(msg);
        String uuid = json.getStr("uuid");
        Integer patientCardId = json.getInt("patientCardId");
        String diagnosis = json.getStr("diagnosis");
        Integer subDeptId = json.getInt("subDeptId");
        Integer doctorId = json.getInt("doctorId");
        Integer registrationId = json.getInt("registrationId");
        String rp = json.getStr("rp");

        DoctorConsultationReport entity = new DoctorConsultationReport();
        entity.setUuid(uuid);
        entity.setPatientCardId(patientCardId);
        entity.setDiagnosis(diagnosis);
        entity.setSubDeptId(subDeptId);
        entity.setDoctorId(doctorId);
        entity.setRegistrationId(registrationId);
        entity.setRp(rp);
        doctorConsultationReportDao.insert(entity);
        log.debug(msg);
        log.debug("成功保存医生处方");
    }
}
