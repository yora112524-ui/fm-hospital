package com.hospital.hms.dao;
import com.hospital.hms.pojo.VideoDiagnose;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Map;

@Mapper
public interface VideoDiagnosisDao {

    void insert(VideoDiagnose entity);
    HashMap selectByOutTradeNo(String outTradeNo);

    //根据挂号单ID，查询付款结果
    HashMap selectPaymentStatus(int id);

    //关闭挂号单
    void closePayment(Map param);

    //根据挂号单ID查询患者信息
    HashMap selectVideoDiagnoseInfo(int diagnoseId);

    void updatePayment(Map param);

    void updateStatus(HashMap param);



}




