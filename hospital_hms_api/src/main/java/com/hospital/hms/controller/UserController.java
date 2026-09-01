package com.hospital.hms.controller;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.hospital.common.utils.PageUtils;
import com.hospital.hms.controller.form.*;
import com.hospital.hms.pojo.User;
import com.hospital.hms.utils.CommonResult;
import com.hospital.hms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/user")
@Tag(name = "UserController", description = "用户管理接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * @Valid :开启校验
     * @RequestBody：请求参数绑定到LoginForm
     *
     * */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public CommonResult userLogin(@Valid @RequestBody LoginForm form) {
        HashMap hashMap = JSONUtil.parse(form).toBean(HashMap.class);
        Integer userId = userService.userLogin(hashMap);
        //构建返回结果
        CommonResult result = CommonResult.ok();
        //登录成功或者失败的处理
        if (userId != null) {
            //登录成功，设置token，并返回给客户端
            //基于Sa-Token生成登录凭证并且会保存到redis
            StpUtil.login(userId);
            String token = StpUtil.getTokenValue();
            String tokenName = StpUtil.getTokenName();

            //查询该用户的权限信息集合
            Set<String> permissions = userService.selectUserPrivileges(userId);

            // 将权限和 token 信息加入到返回结果中
            result.put("result", "登录成功");
            result.put("permissions", permissions);
            result.put("token", token);
            result.put("tokenName", tokenName);
            // 使用日志记录重要的操作
            log.info("User logged in: userId={}, token={}, tokenName={}", userId, token, tokenName);
        } else {
            result.put(CommonResult.RETURN_RESULT, "登录失败");
        }
        return result;
    }

    /**
     * 前提条件：退出登录接口的访问前提条件是,必须要登录后才可以访问。
     * SaCheckLogin该注解由sa-token框架提供
     * @SaCheckLogin：检查当前用户是否登录过，如果没有登录的情况下是无法访问该接口。
     * */
    @GetMapping("/logout")
    @SaCheckLogin   //检查用户是否登录
    @Operation(summary = "退出登录")
    public CommonResult logout() {
        //删除reids中的Token信息
        StpUtil.logout();
        return CommonResult.ok();
    }


    @PostMapping("/updatePassword")
    @SaCheckLogin
    @Operation(summary = "修改用户密码")
    public CommonResult updateUserPassword(@Valid @RequestBody UpdateUserPasswordForm form){
        //获取当前用户登录的id，StpUtil由sa-token框架提供
        int userId = StpUtil.getLoginIdAsInt();
        //将form转换为map，传递给service
        Map param = BeanUtil.beanToMap(form);
        //保存当前用户的id到map
        param.put("userId", userId);
        //调用service查询
        int rows = userService.updateUserPassword(param);
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, rows);
    }


    /*
    * 登录成功后加载用户信息
    * */
    @GetMapping("/loadUserDetails")
    @Operation(summary = "登录成功加载用户信息")
    @SaCheckLogin
    public CommonResult loadUserDetails() {
        return CommonResult.ok(userService.selectUserDetails(StpUtil.getLoginIdAsInt()));
    }

    @PostMapping("/selectUserByCondition")
    @Operation(summary = "按条件查询用户带分页")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public CommonResult selectUserByCondition(@RequestBody SelectUserConditionByPageForm form) {
        int page = form.getPage();
        int length = form.getLength();
        int start = (page-1) * length;
        HashMap params = JSONUtil.parse(form).toBean(HashMap.class);
        params.put("start",start);
        PageUtils pageUtils = userService.selectUserByCondition(params);
        return CommonResult.ok().put("page",pageUtils);
    }


    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT", "USER:INSERT"}, mode = SaMode.OR)
    @Operation(summary = "添加用户")
    public CommonResult insert(@Valid @RequestBody InsertUserForm form) {
        User user = JSONUtil.parse(form).toBean(User.class);
        user.setStatus((byte)1);
        user.setRole(JSONUtil.parseArray(form.getRole()).toString());
        user.setCreateTime(new Date());
        int rows = userService.insert(user);
        return CommonResult.ok().put("rows",rows);
    }

    @PostMapping("/update")
    @SaCheckPermission(value = {"ROOT", "USER:UPDATE"}, mode = SaMode.OR)
    @Operation(summary = "修改用户")
    public CommonResult update(@Valid @RequestBody UpdateUserForm form){
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        //将JSONArray转换为字符串
        param.replace("role",JSONUtil.parseArray(form.getRole()).toString());
        int rows = userService.update(param);
        if(rows == 1) {
            //修改资料后，把用户踢下线
            System.out.println(form.getUserId());
            StpUtil.logoutByLoginId(form.getUserId());
        }
        return CommonResult.ok().put("status",rows);
    }

    @PostMapping("/deleteUserByIds")
    @SaCheckPermission(value = {"ROOT", "USER:DELETE"}, mode = SaMode.OR)
    @Operation(summary = "删除用户")
    public CommonResult deleteUserByIds(@Valid @RequestBody DeleteUserByIdsForm form) {
        //获取到当前用户的ID
        Integer userId = StpUtil.getLoginIdAsInt();
        if(ArrayUtil.contains(form.getIds(),userId)){
            return CommonResult.error("无法删除自己的帐户");
        }

        int rows = userService.deleteUserByIds(form.getIds());
        if(rows > 0) {
            for (Integer id:form.getIds()) {
                //将删除的用户剔下线
                StpUtil.logoutByLoginId(id);
            }
        }
        return CommonResult.ok().put("rows",rows);
    }

    @PostMapping("/selectById")
    @Operation(summary = "根据ID查找用户")
    @SaCheckPermission(value = {"ROOT", "USER:SELECT"}, mode = SaMode.OR)
    public CommonResult selectById(@Valid @RequestBody SelectUserByIdForm form) {
        HashMap map = userService.selectById(form.getUserId());
        return CommonResult.ok(map);
    }


    @GetMapping("/selectAllUser")
    @Operation(summary = "查询所有用户")
    @SaCheckLogin
    public CommonResult searchAllUser() {
        ArrayList<HashMap> list = userService.selectAllUser();
        return CommonResult.ok().put("list", list);
    }

//    @PostMapping("/searchNameAndDept")
//    @Operation(summary = "查找员工姓名和部门")
//    @SaCheckLogin
//    public CommonResult searchNameAndDept(@Valid @RequestBody SearchNameAndDeptForm form){
//        HashMap map=userService.searchNameAndDept(form.getId());
//        return CommonResult.ok(map);
//    }
}