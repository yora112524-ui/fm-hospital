package com.hospital.hms.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.hms.controller.form.SelectDoctorConsultationReportIdForm;
import com.hospital.hms.service.DoctorConsultationReportService;
import com.hospital.hms.utils.CommonResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/consultation/report"})
@Tag(
        name = "DoctorConsultationReportController",
        description = "挂号单查询接口"
)
public class DoctorConsultationReportController {

    @Resource
    private DoctorConsultationReportService doctorConsultationReportService;

    public DoctorConsultationReportController() {
    }

    @PostMapping({"/selectConsultationReportByRegistrationId"})
    @SaCheckLogin
    public CommonResult searchPrescriptionByRegistrationId(@RequestBody @Valid SelectDoctorConsultationReportIdForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        HashMap map = doctorConsultationReportService.selectConsultationReportByRegistrationId(param);
        return CommonResult.ok().put("result", map);
    }
}
