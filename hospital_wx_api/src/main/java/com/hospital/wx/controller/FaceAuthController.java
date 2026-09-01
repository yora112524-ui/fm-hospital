package com.hospital.wx.controller;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.controller.form.mobile.CreateFaceModelForm;
import com.hospital.wx.controller.form.mobile.VerifyFaceModelForm;
import com.hospital.wx.service.PatientFaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.util.Map;
import com.hospital.wx.exception.WxErrorCode;
/**
 * 人脸识别
 * */
@RestController
@RequestMapping("/face/auth")
@Tag(name = "FaceAuthController", description = "人脸识别接口")
@Slf4j
public class FaceAuthController {

    @Autowired
    private PatientFaceService patientFaceService;

    /**
     * 患者人脸信息录入
     * */
    @PostMapping("/createFaceModel")
    @SaCheckLogin
    @Operation(summary = "患者人脸信息录入")
    public CommonResult createFaceModel(@RequestBody @Valid CreateFaceModelForm form) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            form.setUserId(userId);
            Map param = BeanUtil.beanToMap(form);
            patientFaceService.createFaceModel(param);
            return CommonResult.ok();
        } catch (Exception e) {
            // 记录异常日志，便于排查问题
            log.error("录入患者人脸信息时出错，form: {}", form, e);
            // 返回失败信息
            return CommonResult.failed(WxErrorCode.FACE_MODEL_CREATION_FAILED);
        }
    }

    /**
     * 患者人脸信息识别接口
     * */
    @PostMapping("/verifyFaceModel")
    @SaCheckLogin
    @Operation(summary = "患者人脸信息验证")
    public CommonResult verifyFaceModel(@RequestBody @Valid VerifyFaceModelForm form) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            boolean bool = patientFaceService.verifyFaceModel(userId, form.getPhoto());
            return CommonResult.ok().put(CommonResult.RETURN_RESULT, bool);
        } catch (Exception e) {
            // 记录异常日志，便于排查问题
            log.error("验证患者人脸信息时出错，form: {}", form, e);
            // 返回失败信息
            return CommonResult.failed(WxErrorCode.FACE_MODEL_VERIFICATION_FAILED);
        }
    }

}