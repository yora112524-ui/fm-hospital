package com.hospital.wx.controller;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.felord.payment.wechat.v3.model.ResponseSignVerifyParams;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.controller.form.SelectRoomIdForm;
import com.hospital.wx.controller.form.UpdateCanRegisterForm;
import com.hospital.wx.controller.form.mobile.*;
import com.hospital.wx.exception.WxErrorCode;
import com.hospital.wx.pojo.VideoDiagnose;
import com.hospital.wx.service.VideoDiagnosisService;
import com.hospital.wx.utils.Constants;
import com.hospital.wx.utils.TrtcUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视频问诊controller开发
 * */
@RestController
@RequestMapping("/video/diagnosis")
@Tag(name = "VideoDiagnosisController", description = "视频问诊接口")
@Slf4j
public class VideoDiagnosisController {

    @Resource
    private VideoDiagnosisService videoDiagnosisService;

    @Value("${tencent.trtc.appId}")
    private int appId;

    @Autowired
    private TrtcUtil trtcUtil;


    @GetMapping("/generateUserSig")
    @SaCheckLogin
    @Operation(summary = "生成用户签名")
    public CommonResult generateUserSig() {
        try {
            // 通过 Sa-Token 框架获取当前的 userId，若未登录则抛出异常
            int userId = StpUtil.getLoginIdAsInt();

            // 生成用户签名
            String userSig = trtcUtil.generateUserSignature(String.valueOf(userId));

            // 返回成功结果，包含用户签名、用户 ID 和应用 ID
            return CommonResult.ok()
                    .put("userSig", userSig)
                    .put("userId", userId)
                    .put("appId", appId);
        } catch (Exception e) {
            // 记录异常日志
            log.error("生成用户签名失败", e);
            return CommonResult.failed(WxErrorCode.GENERATE_USER_SIG_FAILED.getMessage());
        }
    }

    /**
     * 医生上线
     */
    @GetMapping("/online")
    @SaCheckLogin
    public CommonResult online() {
        try {
            // 通过 Sa-Token 框架获取当前的 userId，若未登录则抛出异常
            int userId = StpUtil.getLoginIdAsInt();
            // 记录上线请求日志
            log.info("医生ID {} 正在上线", userId);
            // 执行上线逻辑
            videoDiagnosisService.online(userId);
            // 返回成功结果
            return CommonResult.ok(Constants.DOCTOR_ONLINE_SUCCESS);
        } catch (Exception e) {
            // 记录异常日志
            log.error("医生上线失败", e);
            return CommonResult.failed(WxErrorCode.DOCTOR_ONLINE_FAILED.getMessage());
        }
    }


    /**
     * 医生下线
     * */
    @GetMapping("/offline")
    @SaCheckLogin
    public CommonResult offline() {
        int userId = StpUtil.getLoginIdAsInt();
        boolean bool = videoDiagnosisService.offline(userId);
        return CommonResult.ok().put("result", bool);
    }

    /**
     * 【开发/关闭】挂号
     * */
    @PostMapping("/updateOpenFlag")
    @SaCheckLogin
    public CommonResult updateOpenFlag(@RequestBody @Valid UpdateCanRegisterForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        videoDiagnosisService.updateOpenFlag(userId, form.getOpen());
        return CommonResult.ok();
    }


    /**
     * 创建线上视频问诊订单
     * */
    @PostMapping("createVideoDiagnoseOrder")
    @SaCheckLogin
    public CommonResult createVideoDiagnoseOrder(@RequestBody @Valid CreateVideoDiagnoseOrderForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        //将formBean转化为pojo实体
        VideoDiagnose entity = BeanUtil.toBean(form,VideoDiagnose.class);
        HashMap result = videoDiagnosisService.createVideoDiagnoseOrder(userId, entity);
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, result);
    }

    /*
     * 查询在线医生列表
     * */
    @PostMapping("selectOnlineDoctorList")
    @SaCheckLogin
    public CommonResult selectOnlineDoctorList(@RequestBody @Valid SelectOnlineDoctorListForm form) {
        List<Map<String, Object>> list = videoDiagnosisService.selectOnlineDoctorList(form.getSubName(), form.getJob());
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, list);
    }

    @GetMapping("/refreshInfo")
    @SaCheckLogin
    @SaCheckPermission(value = {"VIDEO_DIAGNOSE:DIAGNOSE"}, mode = SaMode.OR)
    public CommonResult refreshInfo() {
        int userId = StpUtil.getLoginIdAsInt();
        HashMap map = videoDiagnosisService.refreshInfo(userId);
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, map);
    }

    @GetMapping("/selectVideoDiagnoseInfo")
    @SaCheckLogin
    @SaCheckPermission(value = {"VIDEO_DIAGNOSE:DIAGNOSE"}, mode = SaMode.OR)
    public CommonResult selectVideoDiagnoseInfo() {
        int userId = StpUtil.getLoginIdAsInt();
        HashMap map = videoDiagnosisService.selectVideoDiagnoseInfo(userId);
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, map);
    }

    @SneakyThrows
    @PostMapping("/transactionCallback")
    public Map transactionCallback(
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-signatrue") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            HttpServletRequest request) {

        String body = request.getReader().lines().collect(Collectors.joining());

        ResponseSignVerifyParams params = new ResponseSignVerifyParams();
        params.setWechatpaySerial(serial);
        params.setWechatpaySignature(signature);
        params.setWechatpayTimestamp(timestamp);
        params.setWechatpayNonce(nonce);
        params.setBody(body);

        // 对请求头进行检验数字签名 以确保是微信服务器发送的消息
//        return wechatApiProvider.callback("patient-wx-api").transactionCallback(params, data -> {
//            String outTradeNo = data.getOutTradeNo();
//            String transactionId = data.getTransactionId();
//            // 更新挂号记录的付款状态和付款单ID
//            videoDiagnosisService.updatePayment(new HashMap<String, Object>() {{
//                put("outTradeNo", outTradeNo);
//                put("transactionId", transactionId);
//                put("paymentStatus", 2);
//            }});
//        });
        return null;
    }

    @PostMapping("/searchPaymentResult")
    @SaCheckLogin
    public CommonResult searchPaymentResult(@RequestBody @Valid SelectPaymentResultForm form) {
        boolean bool = videoDiagnosisService.selectPaymentResult(form.getOutTradeNo());
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, bool);
    }

    @PostMapping("/searchImageByVideoDiagnoseId")
    @SaCheckLogin
    public CommonResult searchImageByVideoDiagnoseId(@RequestBody @Valid SelectImageByVideoDiagnoseIdForm form) {
          ArrayList<HashMap> list = videoDiagnosisService.selectImageByVideoDiagnoseId(form.getVideoDiagnoseId());
          return CommonResult.ok().put(CommonResult.RETURN_RESULT, list);
    }

    @PostMapping("/uploadImage")
    @SaCheckLogin
    public CommonResult uploadImage(@Param("file") MultipartFile file, @Param("orderId") Integer videoDiagnoseId) {
        String filename = videoDiagnosisService.uploadImage(file,videoDiagnoseId);
        return CommonResult.ok().put("filename", filename);
    }

    @PostMapping("/deleteImage")
    @SaCheckLogin
    public CommonResult deleteImage(@RequestBody @Valid DeleteVideoDiagnoseImageForm form) {
        Map param = BeanUtil.beanToMap(form);
        videoDiagnosisService.deleteImage(param);
        return CommonResult.ok();
    }

    @GetMapping("/selectUserSig")
    @SaCheckLogin
    public CommonResult searchUserSig() {
        int userId = StpUtil.getLoginIdAsInt();
        String userSig = trtcUtil.generateUserSignature(userId + "");
        return CommonResult.ok().put("userSig", userSig).put("userId", userId).put("appId", appId);
    }
    @PostMapping("/selectRoomId")
    @SaCheckLogin
    public CommonResult searchRoomId(@RequestBody @Valid SelectRoomIdForm form) {
        String roomId = videoDiagnosisService.selectRoomId(form.getDoctorId());
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, roomId);
    }
}
