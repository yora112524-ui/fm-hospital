package com.hospital.hms.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.hospital.hms.service.PermissionService;
import com.hospital.hms.utils.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/permission")
@Tag(name="PermissionController",description = "权限管理接口")
@CrossOrigin
public class PermissionController {
    @Autowired
    private PermissionService permissionService;

    @GetMapping("/selectAllPermission")
    @Operation(summary = "查询所有权限")
//    @SaCheckPermission(value = {"ROOT","ROLE:INSERT","ROLE:UPDATE"},mode = SaMode.OR)
    public CommonResult selectAllPermission(){
        ArrayList<HashMap> list=permissionService.selectAllPermission();
        return CommonResult.ok().put("list",list);
    }
}
