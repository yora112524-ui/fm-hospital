package com.hospital.wx.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.controller.form.*;
import com.hospital.wx.pojo.MedicalDept;
import com.hospital.wx.service.MedicalDeptService;
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
@RequestMapping("/medical/dept")
@Tag(name = "MedicalDeptController", description = "科室管理接口")
public class MedicalDeptController {
    @Autowired
    MedicalDeptService medicalDeptService;

    @Operation(summary = "获取科室门诊信息")
    @GetMapping("/selectDeptAndSub")
    @SaCheckLogin
    public CommonResult searchDeptAndSub() {
        HashMap map = medicalDeptService.selectDeptAndSub();
        return CommonResult.ok().put("result", map);
    }

    @Operation(summary = "获取所有科室名称和ID")
    @GetMapping("/selectAll")
    @SaCheckLogin
    public CommonResult selectAll() {
        ArrayList<HashMap> result = medicalDeptService.selectAll();
        return CommonResult.ok().put("result", result);
    }

    @Operation(summary = "分页显示科室信息接口")
    @PostMapping("/selectConditionByPage")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectConditionByPage(@RequestBody @Valid SelectMedicalDeptByPageForm form) {
        Map param = BeanUtil.beanToMap(form);
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        param.put("start", start);
        PageUtils pageUtils = medicalDeptService.selectConditionByPage(param);
        return CommonResult.ok().put("result", pageUtils);
    }

    @Operation(summary = "添加科室信息接口")
    @PostMapping("/insert")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:INSERT"}, mode = SaMode.OR)
    public CommonResult insert(@RequestBody @Valid InsertMedicalDeptForm form) {
        MedicalDept entity = BeanUtil.toBean(form, MedicalDept.class);
        medicalDeptService.insert(entity);
        return CommonResult.ok();
    }

    @Operation(summary = "通过ID查询科室信息接口")
    @PostMapping("/selectById")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectById(@RequestBody @Valid SelectMedicalDeptByIdForm form) {
        HashMap map = medicalDeptService.selectById(form.getId());
        return CommonResult.ok(map);
    }

    @Operation(summary = "修改科室信息接口")
    @PostMapping("/update")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:UPDATE"}, mode = SaMode.OR)
    public CommonResult update(@RequestBody @Valid UpdateMedicalDeptForm form) {
        MedicalDept entity = BeanUtil.toBean(form, MedicalDept.class);
        medicalDeptService.update(entity);
        return CommonResult.ok();
    }

    @Operation(summary = "删除科室信息接口")
    @PostMapping("/deleteByIds")
    @SaCheckLogin
    @SaCheckPermission(value = {"ROOT", "MEDICAL_DEPT:DELETE"}, mode = SaMode.OR)
    public CommonResult deleteByIds(@RequestBody @Valid DeleteMedicalDeptByIdsForm form) {
        medicalDeptService.deleteByIds(form.getIds());
        return CommonResult.ok();
    }

    @Operation(summary = "查询所有科室列表")
    @PostMapping("/selectMedicalDeptList")
    public CommonResult selectMedicalDeptList(@RequestBody @Valid SelectMedicalDeptListForm form) {
        Map param = BeanUtil.beanToMap(form);
        ArrayList<HashMap> list = medicalDeptService.selectMedicalDeptList(param);
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, list);
    }
}
