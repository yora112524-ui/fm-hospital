package com.hospital.wx.controller;
import cn.felord.payment.wechat.v3.model.ResponseSignVerifyParams;
import com.hospital.common.utils.PageUtils;
import com.hospital.wx.controller.form.mobile.*;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.hospital.wx.common.CommonResult;
import com.hospital.wx.exception.WxErrorCode;
import com.hospital.wx.service.PatientRegistrationService;
import com.hospital.wx.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者挂号
 * */
@RestController
@RequestMapping("/patient/registration")
@Tag(name = "PatientRegistrationController", description = "患者挂号接口")
@Slf4j
public class PatientRegistrationController {

    @Autowired
    private PatientRegistrationService patientRegistrationService;

    @Resource
    private PaymentService paymentService;


    /**
     * 查询挂号结果
     * */
    @PostMapping("/selectPaymentResult")
    @SaCheckLogin
    public CommonResult searchPaymentResult(@RequestBody @Valid SelectPaymentResultForm form) {
        boolean bool = patientRegistrationService.searchPaymentResult(form.getOutTradeNo());
        return CommonResult.ok().put(CommonResult.RETURN_RESULT, bool);
    }


    /**
     * 患者挂号接口
     * */
    @PostMapping("/registerMedicalAppointment")
    @SaCheckLogin
    @Operation(summary = "患者挂号")
    public CommonResult registerMedicalAppointment(@RequestBody @Valid PatientRegistrationAppointmentForm form) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            Map param = BeanUtil.beanToMap(form);
            param.put("userId", userId);
            //执行挂号的流程
            HashMap result = patientRegistrationService.registerMedicalAppointment(param);
            return CommonResult.ok(result != null ? result : new HashMap<>());
        } catch (Exception e) {
            log.error("预约挂号时出错，form: {}", form, e);
            return CommonResult.failed(WxErrorCode.REGISTER_MEDICAL_APPOINTMENT_FAILED);
        }
    }

    @PostMapping("/checkRegisterCondition")
    @SaCheckLogin
    @Operation(summary = "检查挂号条件是否满足")
    public CommonResult checkRegisterCondition(@RequestBody @Valid CheckRegisterConditionForm form) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            form.setUserId(userId);
            Map param = BeanUtil.beanToMap(form);
            String result = patientRegistrationService.checkRegisterCondition(param);
            return CommonResult.ok().put(CommonResult.RETURN_RESULT, result);
        } catch (Exception e) {
            // 记录异常日志，便于排查问题
            log.error("检查挂号条件时出错，form: {}", form, e);
            // 返回失败信息
            return CommonResult.failed(WxErrorCode.CHECK_REGISTER_CONDITION_FAILED);
        }
    }

    @PostMapping("/selectRegistrationByPage")
    @SaCheckLogin
    public CommonResult selectRegistrationByPage(@RequestBody @Valid SelectRegistrationByPageForm form) {
        try {
            // 获取当前用户ID
            int userId = StpUtil.getLoginIdAsInt();
            form.setUserId(userId);
            Map param = BeanUtil.beanToMap(form);
            param.put("start", (form.getPage() - 1) * form.getLength());
            PageUtils pageUtils = patientRegistrationService.selectRegistrationByPage(param);
            return CommonResult.ok().put("result", pageUtils);
        } catch (Exception e) {
            // 记录异常日志
            log.error("查询挂号信息时出错，form: {}", form, e);
            // 返回通用失败信息
            return CommonResult.failed(WxErrorCode.REGISTRATION_QUERY_FAILED);
        }
    }

    @PostMapping("/selectRegistrationInfo")
    @SaCheckLogin
    public CommonResult selectRegistrationInfo(@RequestBody @Valid SelectRegistrationInfoForm form) {
        try {
            int userId = StpUtil.getLoginIdAsInt();
            form.setUserId(userId);
            Map param = BeanUtil.beanToMap(form);
            HashMap map = patientRegistrationService.selectRegistrationInfo(param);
            return CommonResult.ok(map);
        } catch (Exception e) {
            // 记录异常日志
            log.error("查询挂号详情时出错，form: {}", form, e);
            return CommonResult.failed(WxErrorCode.REGISTRATION_DETAIL_QUERY_FAILED);
        }
    }

    @PostMapping("/repayRegistration")
    @SaCheckLogin
    public CommonResult repayRegistration(@RequestBody @Valid RepayRegistrationForm form) {
        int userId = StpUtil.getLoginIdAsInt();
        form.setUserId(userId);
        Map param = BeanUtil.beanToMap(form);
        HashMap result = patientRegistrationService.repayRegistration(param);
        return CommonResult.ok(result);
    }

    /**
     * 接收付款结果通知
     * @SneakyThrows 是 Lombok 提供的一个注解，它的作用是 自动抛出受检异常（Checked Exception），避免显式使用 try-catch 或 throws 关键字。

     Java 规定受检异常（Checked Exception） 必须要 try-catch 或 throws 处理。例如：
     public Map handleWechatPaymentCallback(HttpServletRequest request) throws IOException {
        String body = request.getReader().lines().collect(Collectors.joining());
        return processRequest(body);
     }

    如果加上 Lombok 的 @SneakyThrows，就不需要 throws IOException 了：
     @SneakyThrows
     public Map handleWechatPaymentCallback(HttpServletRequest request) {
         String body = request.getReader().lines().collect(Collectors.joining());
         return processRequest(body);
     }

     Wechatpay-Serial（微信支付平台证书序列号）
        作用：标识微信支付的证书序列号，用于匹配商户保存的微信支付平台证书，确定签名是否合法。
        示例值：515F5B5A265AA49A8FEFF12E0A3E
        详细解释：
            微信支付使用 平台证书 进行签名，这个 serial 就是当前使用的证书序列号。
            商户需要 提前下载 并存储微信支付的证书，收到通知时，通过 serial 来匹配本地证书，以验证签名是否有效。

     Wechatpay-Signature（微信支付的签名）
        作用：微信支付对请求体（body）+ 请求头（timestamp、nonce） 进行签名，商户通过 验签 以确保数据没有被篡改。
        示例值：eD5Yh...==（Base64编码的签名字符串）
        详细解释：
         签名计算方式：
            signature = Sign( timestamp + "\n" + nonce + "\n" + body + "\n" )
            其中：
                timestamp 是通知请求的时间戳
                nonce 是随机字符串
                body 是请求的 JSON 体
                商户服务器需要使用 微信支付的公钥 来验签，确保数据是 微信官方 发送的，防止中间人攻击。

     Wechatpay-Timestamp（时间戳）
        作用：表示微信支付发送通知的时间，用于防止重放攻击。
        示例值：1711388022
        详细解释：
            这个值是 Unix 时间戳（秒级），表示请求的发送时间。
            验签时，商户服务器应该检查当前时间与 timestamp 之间的时间差是否过大（通常不超过 5 分钟），防止恶意重放旧请求。

     Wechatpay-Nonce（随机字符串）
        作用：防止重放攻击，配合 timestamp 共同保证请求的唯一性。
        示例值：3b1f0137b87a4c2f9b8c527c162b7c8e

        详细解释：
            这个 nonce 是微信支付生成的随机字符串，与 timestamp 结合，确保每个请求的唯一性。
            商户可以缓存 最近 5 分钟内收到的 nonce 值，如果发现重复的 nonce，就拒绝请求，避免攻击者重放历史请求。

     返回值示例：
     {
         "code": 200,   表示处理成功
         "message": "Payment processed successfully",   提示信息
         "outTradeNo": "1234567890",    商户订单号
         "transactionId": "4200001234202301011234567890"    微信支付的交易流水号
     }

     * */

    /**
     *
     //微信支付后的回调URL
     private String notifyUrl = "/patient/registration/handleWechatPaymentCallback";
     * */
    @SneakyThrows
    @PostMapping("/handleWechatPaymentCallback")
    public Map handleWechatPaymentCallback(
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            HttpServletRequest request) {

            String body = request.getReader().lines().collect(Collectors.joining());
            //创建封装对象，添加相关参数，验证请求真伪需要用到
            ResponseSignVerifyParams params = new ResponseSignVerifyParams();
            params.setWechatpaySerial(serial);
            params.setWechatpaySignature(signature);
            params.setWechatpayTimestamp(timestamp);
            params.setWechatpayNonce(nonce);
            params.setBody(body);
            //验证通知消息的真伪
            return paymentService.handleWechatPaymentCallback(params);
    }

}