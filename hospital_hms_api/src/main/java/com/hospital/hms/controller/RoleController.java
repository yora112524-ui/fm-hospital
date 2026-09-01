package com.hospital.hms.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.controller.form.*;
import com.hospital.hms.pojo.Role;
import com.hospital.hms.service.RoleService;
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
@RequestMapping("/role")
@CrossOrigin
@Tag(name = "RoleController", description = "角色管理接口")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @GetMapping("/selectAllRole")
    @Operation(summary = "查询所有角色")
    public CommonResult selectAllRole() {
        ArrayList<HashMap> list = roleService.selectAllRole();
        return CommonResult.ok().put("list", list);
    }

    @PostMapping("/selectById")
    @Operation(summary = "根据ID查询角色")
    @SaCheckPermission(value = {"ROOT", "ROLE:SELECT"}, mode = SaMode.OR)
    public CommonResult selectById(@Valid @RequestBody SelectRoleByIdForm form) {
        HashMap map = roleService.selectById(form.getId());
        return CommonResult.ok(map);
    }

    @PostMapping("/selectRoleByCondition")
    @Operation(summary = "查询角色分页数据")
    @SaCheckPermission(value = {"ROOT", "ROLE:SELECT"}, mode = SaMode.OR)
    public CommonResult selectRoleByCondition(@Valid @RequestBody SelectRoleByConditionForm form) {
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start", start);
        PageUtils pageUtils = roleService.selectRoleByCondition(param);
        return CommonResult.ok().put("page", pageUtils);
    }

    @PostMapping("/insert")
    @Operation(summary = "添加角色")
    @SaCheckPermission(value = {"ROOT", "ROLE:INSERT"}, mode = SaMode.OR)
    public CommonResult insert(@Valid @RequestBody InsertRoleForm form) {
        Role role = new Role();
        role.setRoleName(form.getRoleName());
        role.setPermissions(JSONUtil.parseArray(form.getPermissions()).toString());
        role.setDesc(form.getDesc());
        int rows = roleService.insert(role);
        return CommonResult.ok().put("rows", rows);
    }

    @PostMapping("/update")
    @Operation(summary = "更新角色")
    @SaCheckPermission(value = {"ROOT", "ROLE:UPDATE"}, mode = SaMode.OR)
    public CommonResult update(@Valid @RequestBody UpdateRoleForm form) {
        Role role = new Role();
        role.setId(form.getId());
        role.setRoleName(form.getRoleName());
        role.setPermissions(JSONUtil.parseArray(form.getPermissions()).toString());
        role.setDesc(form.getDesc());
        int rows = roleService.update(role);
        //如果用户修改成功，并且用户修改了该角色的关联权限
        if (rows == 1 && form.getChanged()) {
            //把该角色关联的用户踢下线(让用户强制退出登录)
            ArrayList<Integer> list = roleService.selectUserByRoleId(form.getId());
            list.forEach(userId -> {
                StpUtil.logoutByLoginId(userId);
            });
        }
        return CommonResult.ok().put("rows",1);
    }

    @PostMapping("/deleteRoleByIds")
    @Operation(summary = "删除角色")
    @SaCheckPermission(value = {"ROOT", "ROLE:DELETE"}, mode = SaMode.OR)
    public CommonResult deleteRoleByIds(@Valid @RequestBody DeleteRoleByIdsForm form) {
        int rows = roleService.deleteRoleByIds(form.getIds());
        return CommonResult.ok().put("rows", rows);
    }

}
