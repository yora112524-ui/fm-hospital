package com.hospital.wx.service;
import cn.felord.payment.wechat.v3.model.ResponseSignVerifyParams;
import com.hospital.common.utils.PageUtils;
import java.util.HashMap;
import java.util.Map;

public interface PatientRegistrationService {

    HashMap registerMedicalAppointment(Map param);

    String checkRegisterCondition(Map param);

    boolean searchPaymentResult(String outTradeNo);

    PageUtils selectRegistrationByPage(Map param);

    HashMap selectRegistrationInfo(Map param);

    HashMap repayRegistration(Map param);

    //更新付款状态
    void updatePaymentStatus(Map param);

}
