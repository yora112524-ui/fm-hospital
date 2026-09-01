package com.hospital.wx.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.controller.form.*;
import com.hospital.wx.pojo.MedicalDeptSub;
import com.hospital.wx.service.MedicalDeptSubService;
import com.hospital.wx.utils.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/medical/dept/sub")
@Tag(name = "MedicalDeptSubController", description = "诊室管理接口")
public class MedicalDeptSubController {
    @Autowired
    MedicalDeptSubService medicalDeptSubService;

    @Operation(summary = "分页显示诊室信息接口")
    @PostMapping("/selectConditionByPage")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectConditionByPage(@RequestBody @Valid SelectMedicalDeptSubByPageForm form) {
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = medicalDeptSubService.selectConditionByPage(param);
        return CommonResult.ok().put("result", pageUtils);
    }

    @Operation(summary = "添加诊室信息接口")
    @PostMapping("/insert")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:INSERT"}, mode = SaMode.OR)
    public CommonResult insert(@RequestBody @Valid InsertMedicalDeptSubForm form) {
        MedicalDeptSub medicalDeptSub = BeanUtil.toBean(form, MedicalDeptSub.class);
        medicalDeptSubService.insert(medicalDeptSub);
        return CommonResult.ok();
    }

    @Operation(summary = "通过ID查询诊室信息接口")
    @PostMapping("/selectById")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectById(@RequestBody @Valid SelectMedicalDeptSubByIdForm form) {
        HashMap map = medicalDeptSubService.selectById(form.getId());
        return CommonResult.ok(map);
    }

    @Operation(summary = "修改诊室信息接口")
    @PostMapping("/update")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:UPDATE"}, mode = SaMode.OR)
    public CommonResult update(@RequestBody @Valid UpdateMedicalDeptSubForm form) {
        MedicalDeptSub medicalDeptSub = BeanUtil.toBean(form, MedicalDeptSub.class);
        medicalDeptSubService.update(medicalDeptSub);
        return CommonResult.ok();
    }

    @Operation(summary = "删除诊室信息接口")
    @PostMapping("/deleteByIds")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:DELETE"}, mode = SaMode.OR)
    public CommonResult deleteByIds(@RequestBody @Valid DeleteMedicalDeptSubByIdsForm form) {
        medicalDeptSubService.deleteByIds(form.getIds());
        return CommonResult.ok();
    }

    @PostMapping("/selectMedicalDeptSubList")
    @SaCheckLogin
    @Operation(summary = "根据科室ID查询诊室信息")
    public CommonResult selectMedicalDeptSubList(@RequestBody @Valid SelectMedicalDeptSubListForm form) {
        ArrayList<HashMap> list = medicalDeptSubService.selectMedicalDeptSubList(form.getDeptId());
         return CommonResult.ok().put(CommonResult.RETURN_RESULT, list);
    }



//    @Operation(summary = "获取科室门诊信息")
//    @GetMapping("/selectDeptAndSub")
//    @SaCheckLogin
//    public CommonResult searchDeptAndSub() {
//        HashMap map = medicalDeptService.selectDeptAndSub();
//        return CommonResult.ok().put("result", map);
//    }
//
//    @Operation(summary = "获取所有科室名称和ID")
//    @GetMapping("/selectAll")
//    @SaCheckLogin
//    public CommonResult selectAll() {
//        ArrayList<HashMap> result = medicalDeptService.selectAll();
//        return CommonResult.ok().put("result", result);
//    }
//
//

}
