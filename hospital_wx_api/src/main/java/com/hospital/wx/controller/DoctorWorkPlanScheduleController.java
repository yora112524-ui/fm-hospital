package com.hospital.wx.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.controller.form.SelectScheduleByWorkPlanIdForm;
import com.hospital.wx.controller.form.UpdateDoctorScheduleForm;
import com.hospital.wx.controller.form.selectScheduleByDeptSubForm;
import com.hospital.wx.service.DoctorWorkPlanScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/doctor/work_plan/schedule")
@Tag(name = "DoctorWorkPlanScheduleController", description = "出诊管理接口")
public class DoctorWorkPlanScheduleController {

    @Autowired
    private DoctorWorkPlanScheduleService doctorWorkPlanScheduleService;

    @Operation(summary = "通过诊室ID查询医生出诊计划")
    @PostMapping("/selectScheduleByDeptSub")
    @SaCheckPermission(value = {"ROOT", "SCHEDULE:SELECT"}, mode = SaMode.OR)
    public CommonResult selectScheduleByDeptSubIdAndDate(@RequestBody @Valid selectScheduleByDeptSubForm form) {
        Map param = BeanUtil.beanToMap(form);
        ArrayList list = doctorWorkPlanScheduleService.selectScheduleByDeptSub(param);
        return CommonResult.ok().put("result", list);
    }

    @Operation(summary = "通过WorkPlanId查询医生出诊计划")
    @PostMapping("/selectScheduleByWorkPlanId")
    @SaCheckPermission(value = {"ROOT", "SCHEDULE:SELECT"}, mode = SaMode.OR)
    public CommonResult selectScheduleByWorkPlanId(@RequestBody @Valid SelectScheduleByWorkPlanIdForm form) {
        HashMap map = doctorWorkPlanScheduleService.selectScheduleByWorkPlanId(form.getWorkPlanId());
        return CommonResult.ok().put("result", map);
    }

    @Operation(summary = "更新医生出诊计划")
    @PostMapping("/updateSchedule")
    @SaCheckPermission(value = {"ROOT", "SCHEDULE:UPDATE"}, mode = SaMode.OR)
    public CommonResult updateSchedule(@RequestBody @Valid UpdateDoctorScheduleForm form) {
        Map param = BeanUtil.beanToMap(form);
        doctorWorkPlanScheduleService.updateSchedule(param);
        return CommonResult.ok();
    }
}
