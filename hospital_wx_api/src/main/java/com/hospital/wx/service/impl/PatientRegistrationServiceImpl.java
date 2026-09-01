package com.hospital.wx.service.impl;
import cn.felord.payment.wechat.v3.model.ResponseSignVerifyParams;
import cn.felord.payment.wechat.v3.model.TransactionConsumeData;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospital.common.utils.PageUtils;
import com.hospital.wx.dao.*;
import com.hospital.wx.exception.WxErrorCode;
import com.hospital.wx.pojo.PatientRegistration;
import com.hospital.wx.service.PatientFaceService;
import com.hospital.wx.service.PatientRegistrationService;
import com.hospital.wx.service.PaymentService;
import com.hospital.wx.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PatientRegistrationServiceImpl implements PatientRegistrationService {

    @Autowired
    private PatientRegistrationDao patientRegistrationDao;

    private String WORK_PLAN_SCHEDULE_KEY = Constants.WORK_PLAN_SCHEDULE_KEY;
    private String  REGISTRATION_PAYMENT_ = Constants.REGISTRATION_PAYMENT;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    PatientUserDao patientUserDao;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    PatientUserInfoDao patientUserInfoDao;

    @Autowired
    PatientFaceService patientFaceService;

    @Autowired
    DoctorWorkPlanDao doctorWorkPlanDao;

    @Autowired
    DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    //微信支付后的回调URL
    private String notifyUrl = "/patient/registration/handleWechatPaymentCallback";



    /**
     * 患者挂号业务逻辑开发
     * */
    @Override
    @Transactional
    public HashMap registerMedicalAppointment(Map param) {
        int workPlanId = MapUtil.getInt(param, "workPlanId");
        int scheduleId = MapUtil.getInt(param, "scheduleId");

        // 检查挂号时段是否存在缓存中
        String key = WORK_PLAN_SCHEDULE_KEY + scheduleId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return null;
        }

        //占用 Redis 中的挂号名额
        if (!checkAndReserveScheduleSlot(key)) {
            return null;
        }

        try {
            // 从参数中获取 userId
            int userId = MapUtil.getInt(param, "userId");
            // 通过 userId 查询用户信息
            HashMap<String, Object> userMap = patientUserDao.selectOpenIdByUserId(userId);
            //获取用户的 openId（微信支付标识）
            String openId = MapUtil.getStr(userMap, "openId");
            // 获取用户的就诊卡 ID
            int patientCardId = MapUtil.getInt(userMap, "patientCardId");

            // 获取支付金额（字符串格式）
            String amountStr = MapUtil.getStr(param, "amount");
            // 将金额转换为分（元 × 100） 如果挂号金额是29.5 转换完后就是：2950
            int total = NumberUtil.mul(amountStr, "100").intValue();
            // 生成挂号单流水号
            String outTradeNo = IdUtil.simpleUUID().toUpperCase();

            /**
             * 创建支付订单
             * outTradeNo：挂号单流水号
             * openId：用户的 openId（微信支付标识）
             * total：金额
             * */
            ObjectNode objectNode = createPaymentOrder(outTradeNo, openId, total);

            /**
             *      在微信支付流程中，prepay_id（预支付交易会话标识）是微信支付服务器生成的一个唯一标识，用于后续支付请求。它的作用如下：
             *      标识一次预支付交易：
             *      这是在商户服务器向微信支付后台请求预支付订单（/v3/pay/transactions/app 或 /v3/pay/transactions/jsapi 等接口）后，微信支付返回的一个 prepay_id，表示该订单已经成功创建。
             *           用于支付请求：
             *           在客户端（App、小程序、H5等）发起支付时，需要带上 prepay_id，微信支付 SDK 才能拉起支付界面，让用户完成付款
             *
             * */
            String prepayId = objectNode.get("prepay_id").textValue();

            // 保存挂号记录
            saveRegistrationRecord(param, patientCardId, outTradeNo, prepayId);

            // 更新医生出诊计划与时段的实际挂号人数
            updateAppointmentCount(workPlanId, scheduleId);

            // 缓存付款信息
            cachePaymentInfo(outTradeNo, workPlanId, scheduleId);

            //返回信息
            return buildPaymentResponse(outTradeNo, objectNode);
        } catch (Exception e) {
            // 记录错误日志，包含异常信息和关键参数
            log.error("微信支付挂号异常， 错误信息: {}", e.getMessage(), e);
            // 回滚医生排班的预约名额
            rollbackScheduleSlot(key);
            throw e;
        }
    }

    /*
    * 构建返回值
    * */
    private HashMap<String, String> buildPaymentResponse(String outTradeNo, ObjectNode objectNode) {
        return new HashMap<>(Map.of(
                "outTradeNo", outTradeNo,
                "prepayId", objectNode.get("prepay_id").asText(),
                "timeStamp", objectNode.get("timeStamp").asText(),
                "nonceStr", objectNode.get("nonceStr").asText(),
                "package", objectNode.get("package").asText(),
                "signType", objectNode.get("signType").asText(),
                "paySign", objectNode.get("paySign").asText()
        ));
    }

    /**
     * 占用 Redis 中的挂号名额， 基于Redis 乐观锁的操作。
     */
    private boolean checkAndReserveScheduleSlot(String key) {
        // 使用 Redis 事务执行操作
        Object execute = redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                // 监听指定的键，以检测是否被其他事务修改
                operations.watch(key);
                // 获取 Redis 哈希结构中的所有键值对
                Map<Object, Object> entry = operations.opsForHash().entries(key);
                // 获取最大可预约数量（默认值为 0）
                int maximum = MapUtil.getInt(entry, "maximum", 0);
                // 获取当前已预约数量（默认值为 0）
                int num = MapUtil.getInt(entry, "num", 0);
                // 如果已预约数量达到或超过最大限制，则取消监视并返回 null
                if (num >= maximum) {
                    operations.unwatch();
                    return null;
                }
                // 开启 Redis 事务
                operations.multi();
                // 在哈希表中将 "num" 的值加 1，表示新增一个预约
                operations.opsForHash().increment(key, "num", 1L);
                // 提交事务，如果 `watch` 监视的键未被修改，则执行操作，否则事务回滚
                return operations.exec();
            }
        });
        // 如果事务执行成功，则返回 true，否则返回 false
        return execute != null;
    }


    /**
     * 创建支付订单
     * outTradeNo：挂号单
     * openId：用户的标识
     * total：挂号费
     * notifyUrl：微信支付成功后的回调URL，支付通知的结果会返回给这个notifyUrl
     */
    private ObjectNode createPaymentOrder(String outTradeNo, String openId, int total) {
        //学习过程中测试代码调用mockCreatePaymentOrder，实际开发调用createPaymentOrder
        return paymentService.mockCreatePaymentOrder(outTradeNo, openId, total, "挂号费", notifyUrl, null);
    }

    /**
     * 保存挂号记录
     */
    private void saveRegistrationRecord(Map<String, Object> param, int patientCardId, String outTradeNo, String prepayId) {
        PatientRegistration entity = new PatientRegistration();
        // 设置排班计划 ID（医院的工作计划 ID）
        entity.setWorkPlanId(MapUtil.getInt(param, "workPlanId"));
        // 设置医生的排班 ID（医生当天的排班记录）
        entity.setDoctorScheduleId(MapUtil.getInt(param, "scheduleId"));
        // 绑定患者就诊卡 ID（唯一标识患者）
        entity.setPatientCardId(patientCardId);
        // 设置医生 ID
        entity.setDoctorId(MapUtil.getInt(param, "doctorId"));
        // 设置诊室 ID
        entity.setDeptSubId(MapUtil.getInt(param, "deptSubId"));
        // 设置挂号日期（格式如 YYYY-MM-DD）
        entity.setDate(MapUtil.getStr(param, "date"));
        // 设置预约时间段（slot 表示时间段编号）
        entity.setSlot(MapUtil.getInt(param, "slot"));
        // 设置挂号费用（转换为 BigDecimal 类型）
        entity.setAmount(new BigDecimal(MapUtil.getStr(param, "amount")));
        //挂号单流水号（UUID）
        entity.setOutTradeNo(outTradeNo);
        //微信付款单预支付ID，prepay_id 用于后续支付确认
        entity.setPrepayId(prepayId);
        // 将挂号信息插入数据库
        patientRegistrationDao.insert(entity);
    }

    /**
     * 更新出诊计划与时段的实际挂号人数
     */
    private void updateAppointmentCount(int workPlanId, int scheduleId) {
        doctorWorkPlanDao.updateNumById(Map.of("id", workPlanId, "n", 1));
        doctorWorkPlanScheduleDao.updateNumById(Map.of("id", scheduleId, "n", 1));
    }

    /**
     * 缓存付款信息
     */
    private void cachePaymentInfo(String outTradeNo, int workPlanId, int scheduleId) {
        String redisKey = REGISTRATION_PAYMENT_ + outTradeNo;
        redisTemplate.opsForHash().putAll(redisKey, Map.of(
                "workPlanId", workPlanId,
                "scheduleId", scheduleId,
                "outTradeNo", outTradeNo
        ));
        //实际开发，设置6分钟的有效期
//        redisTemplate.expireAt(redisKey, new DateTime().offset(DateField.MINUTE, 6));
        //测试代码,设置1分钟的有效期
        redisTemplate.expireAt(redisKey, new DateTime().offset(DateField.MINUTE, 1));

    }

    /**
     * 事务失败时回滚 Redis 挂号名额
     */
    private void rollbackScheduleSlot(String key) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.opsForHash().increment(key, "num", -1);
        }
    }


    @Override
    public String checkRegisterCondition(Map param) {
        // 获取当前系统时间
        param.put("today", DateUtil.today());

        // 条件1: 检查当天用户是否已经挂号3次以上
        if (isRegistrationLimitReached(param)) {
            return WxErrorCode.DAILY_REGISTRATION_LIMIT_REACHED.getMessage().toString();
        }

        // 条件2: 检查当天是否已经挂过该门诊的号
        if (hasRegisteredForRoom(param)) {
            return WxErrorCode.ROOM_ALREADY_REGISTERED.getMessage().toString();
        }

        // 条件3: 检查是否存在人脸面部数据
        if (!hasFaceModel(param)) {
            return WxErrorCode.FACE_MODEL_NOT_FOUND.getMessage().toString();
        }

        // 条件4: 检查今日是否存在挂号用户的面部识别记录
//        if (!checkFaceAuthRecordForToday(param)) {
//            return WxErrorCode.NO_FACE_VERIFICATION_RECORD.getMessage().toString();
//        }

        // 所有条件满足
        return WxErrorCode.REGISTRATION_ELIGIBLE.getMessage().toString();
    }

    // 条件1: 检查当天用户是否已经挂号3次以上
    private boolean isRegistrationLimitReached(Map<String, Object> param) {
        long count = patientRegistrationDao.getTodayRegistrationCount(param);
        return count >= 3;
    }

    // 条件2: 检查当天是否已经挂过该门诊的号
    private boolean hasRegisteredForRoom(Map<String, Object> param) {
        Integer id = patientRegistrationDao.hasRegisteredToday(param);
        return id != null;
    }

    // 条件3: 检查是否存在人脸面部数据
    private boolean hasFaceModel(Map<String, Object> param) {
        int userId = MapUtil.getInt(param, "userId");
        Boolean bool = patientUserInfoDao.existsFaceModel(userId);
        return bool != null && bool;
    }

    // 条件4: 检查今日是否存在挂号用户的面部识别记录
    private boolean checkFaceAuthRecordForToday(Map<String, Object> param) {
        return patientFaceService.checkFaceAuthRecordForToday(param);
    }

    @Override
    public boolean searchPaymentResult(String outTradeNo) {
        return true;
//        String transactionId = paymentService.searchPaymentResult(outTradeNo);
//        if (transactionId != null) {
//            //更新挂号单为已付款，并且记录transactionId
//            updatePayment(new HashMap() {{
//                put("outTradeNo", outTradeNo);
//                put("transactionId", transactionId);
//                put("paymentStatus", 2);
//            }});
//            return true;
//        } else {
//            return false;
//        }
    }

    @Override
    public PageUtils selectRegistrationByPage(Map param) {
        ArrayList list = null;
        long count = patientRegistrationDao.selectRegistrationCount(param);
        if (count > 0) {
            list = patientRegistrationDao.selectRegistrationByPage(param);
        } else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, count, page, length);
        return pageUtils;
    }

    @Override
    public HashMap selectRegistrationInfo(Map param) {
        return patientRegistrationDao.selectRegistrationInfo(param);
    }

    @Transactional
    @Override
    public HashMap repayRegistration(Map param) {
        //先查询openId和付款金额
        HashMap map = patientRegistrationDao.searchRepayInfo(param);
        String openId = MapUtil.getStr(map, "openId");
        String temp = MapUtil.getStr(map, "amount");
        int amount = NumberUtil.mul(temp, "100").intValue();
        //生成新的流水号
        String outTradeNo = IdUtil.simpleUUID();
        //创建新的支付订单
        ObjectNode objectNode = paymentService.mockCreatePaymentOrder(outTradeNo, openId, amount,"挂号费",notifyUrl,null);
        String prepayId = objectNode.get("prepay_id").textValue(); //预支付订单ID
        param.put("outTradeNo", outTradeNo);
        param.put("prepayId", prepayId);
        //保存prepayId和新的outTradeNo
        patientRegistrationDao.updateRepayInfo(param);
        //返回小程序弹出付款弹窗必备的参数
        HashMap result = new HashMap() {{
            put("outTradeNo", outTradeNo);
            put("prepayId", prepayId);
            put("timeStamp", objectNode.get("timeStamp").asText());
            put("nonceStr", objectNode.get("nonceStr").asText());
            put("package", objectNode.get("package").asText());
            put("signType", objectNode.get("signType").asText());
            put("paySign", objectNode.get("paySign").asText());
        }};
        return result;
    }


    @Override
    @Transactional
    public void updatePaymentStatus(Map param) {
        String outTradeNo = MapUtil.getStr(param, "outTradeNo");
        //挂号费支付成功，删除Redis中对应的挂号缓存
        redisTemplate.delete(Constants.REGISTRATION_PAYMENT + outTradeNo);
        //更新挂号单付款状态为已付款（payment_status = 2）
        patientRegistrationDao.updatePaymentStatus(param);
    }
}
