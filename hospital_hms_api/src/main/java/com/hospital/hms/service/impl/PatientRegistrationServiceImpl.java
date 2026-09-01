package com.hospital.hms.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospital.hms.dao.DoctorWorkPlanDao;
import com.hospital.hms.dao.DoctorWorkPlanScheduleDao;
import com.hospital.hms.dao.PatientRegistrationDao;
import com.hospital.hms.dao.PatientUserDao;
import com.hospital.hms.pojo.PatientRegistration;
import com.hospital.hms.service.PatientRegistrationService;
import com.hospital.hms.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PatientRegistrationServiceImpl implements PatientRegistrationService {

    private String DOCTOR_SCHEDULE_KEY = "doctor_schedule_";

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    PatientUserDao patientUserDao;

//    @Autowired
    private PaymentService paymentService;

    @Autowired
    PatientRegistrationDao patientRegistrationDao;

    @Autowired
    DoctorWorkPlanDao doctorWorkPlanDao;

    @Autowired
    DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    private String notifyUrl = "/registration/transactionCallback";


    /**
     * 挂号的业务逻辑
     * */
    @Override
    @Transactional
    public HashMap registerMedicalAppointment(Map param) {
        int workPlanId = MapUtil.getInt(param, "workPlanId");
        int scheduleId = MapUtil.getInt(param, "scheduleId");
        //检查Redis中是否存在日程缓存（过期的出诊计划和时段会自动删除），不存在缓存就不执行挂号
        String key = DOCTOR_SCHEDULE_KEY + scheduleId;
        if (!redisTemplate.hasKey(key)) {
            return null;
        }
        //Redis事务代码必须写到execute()回调函数中
        Object execute = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //关注缓存数据（拿到乐观锁的Version）
                operations.watch(key);

                //拿到缓存的数据
                Map entry = operations.opsForHash().entries(key);

                //拿到缓存该时段最大接诊人数和已挂号人数
                int maximum = Integer.parseInt(entry.get("maximum").toString());
                int num = Integer.parseInt(entry.get("num").toString());

                //如果已挂号人数小于最大人数就可以挂号
                if (num < maximum) {
                    //开启Redis事务
                    operations.multi();
                    //已挂号人数+1
                    operations.opsForHash().increment(key, "num", 1);
                    //提交事务
                    return operations.exec();
                }
                //到达挂号人数上限就不执行挂号
                else {
                    operations.unwatch();
                    return null;
                }
            }
        });
        //如果Redis事务提交失败就结束Service方法
        if (execute == null) {
            return null;
        }
        //如果Redis事务提交成功，就执行下面的代码
        try {
            //1 创建支付订单
            //2 保存挂号记录
            //3 更新数据库中的该时段挂号人数和该医生当日挂号实际人数
            //4 在Redis中缓存付款记录，并设置过期时间

            /**
             * 如下为代码实现逻辑
             * */
            int userId = MapUtil.getInt(param, "userId");
            //查询患者openId字符串，用于创建支付单
            HashMap map = patientUserDao.selectOpenIdByUserId(userId);
            String openId = MapUtil.getStr(map, "openId");
            int patientCardId = MapUtil.getInt(map, "patientCardId");
            int doctorId = MapUtil.getInt(param, "doctorId");
            int deptSubId = MapUtil.getInt(param, "deptSubId");
            String date = MapUtil.getStr(param, "date");
            //挂号时段
            int slot = MapUtil.getInt(param, "slot");
            //挂号金额，（货币单位是分）
            String temp = MapUtil.getStr(param, "amount");
            int total = NumberUtil.mul(temp, "100").intValue();
            //生成订单号
            String outTradeNo = IdUtil.simpleUUID().toUpperCase();
            //创建支付订单
            ObjectNode objectNode = paymentService.unifiedOrder(outTradeNo, openId,total,"挂号费",notifyUrl,null);
            //支付单的预支付ID
            String prepayId = objectNode.get("prepay_id").textValue();
            PatientRegistration entity = new PatientRegistration();
            entity.setWorkPlanId(workPlanId);
            entity.setDoctorScheduleId(scheduleId);
            entity.setPatientCardId(patientCardId);
            entity.setDoctorId(doctorId);
            entity.setDeptSubId(deptSubId);
            entity.setDate(date);
            entity.setSlot(slot);
            entity.setAmount(new BigDecimal(temp));
            entity.setOutTradeNo(outTradeNo);
            entity.setPrepayId(prepayId);
            //保存挂号记录
            patientRegistrationDao.insert(entity);
            //更新出诊计划实际挂号人数
            doctorWorkPlanDao.updateNumById(new HashMap() {{
                put("id", workPlanId);
                put("n", 1);
            }});
            //更新出诊时段实际挂号人数
            doctorWorkPlanScheduleDao.updateNumById(new HashMap() {{
                put("id", scheduleId);
                put("n", 1);
            }});

            /*
             * 在Redis中缓存付款记录，并设置过期时间。
             * (1) 如果15分钟内患者支付了挂号费，会有Java程序删除该缓存。
             * (2) 如果该缓存过期，说明15分钟内患者没有支付挂号费。
             * 如果患者没有付款，就关闭挂号单，并且恢复缓存和数据库中的已挂号人数。
             */
            redisTemplate.opsForHash().putAll("registration_payment_" + outTradeNo,new HashMap() {{
                put("workPlanId", workPlanId);
                put("scheduleId", scheduleId);
                put("outTradeNo", outTradeNo);
            }});
            DateTime now = new DateTime();
            now.offset(DateField.MINUTE, 15);
            //15分钟后缓存过期
            redisTemplate.expireAt("registration_payment_" + outTradeNo, now);

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
        } catch (Exception e) {
            if (redisTemplate.hasKey(key)) {
                //恢复缓存该日程已经挂号数量
                redisTemplate.opsForHash().increment(key, "num", -1);
            }
            throw e;
        }
    }
}
