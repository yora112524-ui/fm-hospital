package com.hospital.hms.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.controller.form.*;
import com.hospital.hms.pojo.Dept;
import com.hospital.hms.service.DeptService;
import com.hospital.hms.utils.CommonResult;
import com.hospital.common.utils.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/dept")
@Tag(name = "DeptController", description = "部门管理接口")
public class DeptController {
    @Autowired
    private DeptService deptService;
    @GetMapping("/selectAllDept")
    @Operation(summary = "查询所有部门")
    public CommonResult selectAllDept() {
        ArrayList<HashMap> list = deptService.selectAll();
        return CommonResult.ok().put("list", list);
    }

    @PostMapping("/selectById")
    @Operation(summary = "根据ID查询部门")
    @SaCheckPermission(value = {"ROOT", "DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectById(@Valid @RequestBody SelectDeptByIdForm form) {
        HashMap map = deptService.selectById(form.getId());
        return CommonResult.ok(map);
    }

    @PostMapping("/selectDeptByCondition")
    @Operation(summary = "查询部门分页数据")
    @SaCheckPermission(value = {"ROOT", "DEPT:SELECT"}, mode = SaMode.OR)
    public CommonResult selectDeptByCondition(@Valid @RequestBody SelectDeptByConditionForm form) {
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start", start);
        PageUtils pageUtils = deptService.selectDeptByCondition(param);
        return CommonResult.ok().put("page", pageUtils);
    }

    @PostMapping("/insert")
    @Operation(summary = "添加部门")
    @SaCheckPermission(value = {"ROOT", "DEPT:INSERT"}, mode = SaMode.OR)
    public CommonResult insert(@Valid @RequestBody InsertDeptForm form) {
        Dept zzyDept = JSONUtil.parse(form).toBean(Dept.class);
        int rows = deptService.insert(zzyDept);
        return CommonResult.ok().put("rows", rows);
    }

    @PostMapping("/update")
    @Operation(summary = "更新部门")
    @SaCheckPermission(value = {"ROOT", "DEPT:UPDATE"}, mode = SaMode.OR)
    public CommonResult update(@Valid @RequestBody UpdateDeptForm form) {
        Dept dept = new Dept();
        dept.setId(form.getId());
        dept.setDeptName(form.getDeptName());
        dept.setTel(form.getTel());
        dept.setEmail(form.getEmail());
        dept.setDesc(form.getDesc());
        int rows = deptService.update(dept);
        return CommonResult.ok().put("rows", rows);
    }

    @PostMapping("/deleteDeptByIds")
    @Operation(summary = "删除部门记录")
    @SaCheckPermission(value = {"ROOT", "DEPT:DELETE"}, mode = SaMode.OR)
    public CommonResult deleteDeptByIds(@Valid @RequestBody DeleteDeptByIdsForm form) {
        int rows = deptService.deleteDeptByIds(form.getIds());
        return CommonResult.ok().put("rows", rows);
    }
}