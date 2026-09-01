package com.hospital.hms.config;
import cn.hutool.core.map.MapUtil;
import com.hospital.hms.dao.DoctorWorkPlanScheduleDao;
import com.hospital.hms.dao.PatientRegistrationDao;
import com.hospital.hms.dao.VideoDiagnosisDao;
import com.hospital.hms.service.VideoDiagnosisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class KeyExpiredListener extends KeyExpirationEventMessageListener {

//    @Resource
//    private VideoDiagnosisService videoDiagnosisService;
//
//    @Autowired
//    private PatientRegistrationDao medicalRegistrationDao;
//
//    @Autowired
//    private DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    @Resource
    private VideoDiagnosisDao videoDiagnosisDao;

    @Autowired
    private RedisTemplate redisTemplate;

    public KeyExpiredListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }


    @Override
    @Transactional
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        //跟视频问诊有关的缓存被销毁
        if (key.startsWith("video_diagnose_")) {

        }
        //视频问诊挂号缓存被销毁
        else if (key.startsWith("patient_video_diagnose_payment")) {
            //获得挂号单ID
            int id = Integer.parseInt(key.split("#")[1]);
            //查询付款状态
            HashMap map = videoDiagnosisDao.selectPaymentStatus(id);
            int paymentStatus = MapUtil.getInt(map, "paymentStatus");

            int doctorId = MapUtil.getInt(map, "doctorId");
            key = "online_doctor_" + doctorId;

            //只要不是付款成功，就销毁该患者候诊缓存
            if (paymentStatus != 2) {
                Map entries = redisTemplate.opsForHash().entries(key);
                //判断候诊缓存是不是当前的挂号单
                if (id == MapUtil.getInt(entries, "nextOrder")) {
                    redisTemplate.opsForHash().putAll(key, new HashMap() {{
                        put("nextPatient", "none"); //候诊患者Id
                        put("nextOrder", "none"); //等候问诊的订单号
                        put("nextStart", "none"); //等候问诊的开始时间
                        put("nextEnd", "none"); //等候问诊的结束时间
                        put("nextPayment", false); //等候问诊是否付款
                        put("nextNotify", false); //是否通知了医生端
                    }});
                }
                //TODO 使用WebSocket通知视频问诊页面刷新
                //关闭视频问诊订单（3状态）
                videoDiagnosisDao.closePayment(new HashMap() {{
                    put("id", id);
                }});
            }
        }
    }

//    @Override
//    @Transactional
//    public void onMessage(Message message, byte[] pattern) {
//        //获取过期数据的Key
//        String key = message.toString();
//        //判断过期的数据是否为挂号单
//        if (key.startsWith("registration_payment_")) {
//            //从Key中提取挂号单流水号
//            String outTradeNo = key.split("_")[2];
//            //更新挂号支付订单状态为4
//            medicalRegistrationDao.cancelUnpaidAppointments(outTradeNo);
////            medicalRegistrationDao.discardPayment(outTradeNo);
//            //出诊计划已挂号人数减去1
//            doctorWorkPlanDao.releaseNumByOutTradeNo(outTradeNo);
//            //出诊时段已挂号人数减去1
//            doctorWorkPlanScheduleDao.releaseNumByOutTradeNo(outTradeNo);
//            //查询挂号单的workPlanId和scheduleId
//            HashMap map = medicalRegistrationDao.searchWorkPlanIdAndScheduleId(outTradeNo
//            int scheduleId = MapUtil.getInt(map, "doctorScheduleId");
//            int workPlanId = MapUtil.getInt(map, "workPlanId");
//
//            key = "doctor_schedule_" + scheduleId;
//            if (redisTemplate.hasKey(key)) {
//                //更新缓存中已挂号人数的数量减去1
//                redisTemplate.opsForHash().increment(key, "num", -1);
//            }
//        }
//        super.onMessage(message, pattern);
//    }

}