package com.hospital.hms.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hospital.hms.dao.*;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.VideoDiagnose;
import com.hospital.hms.pojo.VideoDiagnoseFile;
import com.hospital.hms.service.PaymentService;
import com.hospital.hms.service.VideoDiagnosisService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class VideoDiagnosisServiceImpl implements VideoDiagnosisService {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    private MinioClient client;

    @PostConstruct
    public void init() {
        this.client = new MinioClient.Builder().endpoint(endpoint)
                .credentials(accessKey, secretKey).build();
    }

    @Resource
    private DoctorDao doctorDao; // 注入医生数据访问对象，用于与医生相关的数据交互

    @Resource
    private UserDao userDao; // 注入用户数据访问对象，用于获取用户信息

    @Resource
    private PatientUserDao patientUserDao;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private VideoDiagnosisFileDao videoDiagnosisFileDao;


    @Resource
    private VideoDiagnosisDao videoDiagnosisDao;

    @Resource
    private PaymentService paymentService;


    private String notifyUrl = "/video_diagnosis/transactionCallback";

    private static final String ONLINE_DOCTOR_PREFIX = "online_doctor_"; // 定义医生在线缓存的前缀常量

    @Override
    public void online(int userId) {
        // 根据用户ID查找医生ID，登录用户必须是医生。
        int doctorId = this.selectRefIdIdAndJobById(userId);
        // 生成医生在线缓存的键
        String key = ONLINE_DOCTOR_PREFIX + doctorId;

        // 判断是否存在该医生的上线缓存
        if (redisTemplate.hasKey(key)) {
            return; // 如果已存在缓存，直接返回，避免重复创建
        }

        // 获取医生详细信息的查询
        HashMap map = doctorDao.getDoctorDetailsForCache(doctorId);
        // 创建医生上线缓存
        createDoctorOnlineCache(key, map);
    }

    private void createDoctorOnlineCache(String key, HashMap map) {
        // 将医生详细信息与默认状态一起存入Redis缓存
        redisTemplate.opsForHash().putAll(key, new HashMap<>(map) {{
            put("open", false); // 初始化挂号开放状态为false
            put("currentPatient", "none"); // 当前患者ID初始化为none
            put("currentOrder", "none"); // 当前问诊订单号初始化为none
            put("currentHandle", false); // 当前问诊是否处理初始化为false
            put("currentStart", "none"); // 当前问诊开始时间初始化为none
            put("currentEnd", "none"); // 当前问诊结束时间初始化为none
            put("currentPayment", false); // 当前问诊是否付款初始化为false
            put("currentStatus", 1); // 当前问诊状态初始化为1（未开始）
            put("currentNotify", false); // 当前是否已通知医生初始化为false
            put("nextPatient", "none"); // 候诊患者ID初始化为none
            put("nextOrder", "none"); // 等候问诊的订单号初始化为none
            put("nextStart", "none"); // 等候问诊的开始时间初始化为none
            put("nextEnd", "none"); // 等候问诊的结束时间初始化为none
            put("nextPayment", false); // 排队问诊是否付款初始化为false
            put("nextNotify", false); // 候诊是否已通知医生初始化为false
        }});
    }

    private int selectRefIdIdAndJobById(int userId) {
        // 根据用户ID查找关联的医生ID
        HashMap map = userDao.selectRefIdIdAndJobById(userId);
        String job = MapUtil.getStr(map, "job"); // 获取用户的职业信息
        // 检查用户是否为医生
        if (!"医生".equals(job)) {
            throw new GlobalException("当前用户不是医生"); // 抛出异常，如果用户不是医生
        }
        Integer doctorId = MapUtil.getInt(map, "refId"); // 获取关联的医生ID
        // 检查当前用户是否是医生
        if (doctorId == null) {
            throw new GlobalException("当前用户没有关联医生表"); // 抛出异常，如果用户没有关联医生
        }
        return doctorId; // 返回医生ID
    }

    @Override
    public boolean offline(int userId) {
        // 查找医生的主键值
        int doctorId = this.selectRefIdIdAndJobById(userId);
        // 生成医生在线缓存的键
        String key = ONLINE_DOCTOR_PREFIX + doctorId;

        // 判断是否存在上线缓存
        if (!redisTemplate.hasKey(key)) {
            return true; // 如果不存在缓存，医生可以下线
        }

        // 检查是否有正在执行的问诊和等待的问诊
        List<String> list = redisTemplate.opsForHash().multiGet(key, Arrays.asList("currentOrder", "nextOrder"));

        String currentOrder = list.get(0); // 获取当前问诊订单号
        String nextOrder = list.get(1); // 获取下一个等候问诊的订单号
        // 存在当前问诊或者排队问诊，医生就不能下线
        if (!"none".equals(currentOrder) || !"none".equals(nextOrder)) {
            return false; // 如果有正在进行的问诊或排队问诊，返回false
        }

        // 删除医生的问诊缓存
        redisTemplate.delete(key); // 清除缓存
        return true; // 返回true，表示医生已成功下线
    }

    @Override
    public void updateOpenFlag(int userId, boolean open) {
        // 查找医生的主键值
        int doctorId = this.selectRefIdIdAndJobById(userId);
        // 生成医生在线缓存的键
        String key = ONLINE_DOCTOR_PREFIX + doctorId;

        // 如果不存在上线缓存，医生都没上线，所以就不需要更新open属性值
        if (!redisTemplate.hasKey(key)) {
            return; // 如果不存在缓存，直接返回
        }
        // 更新open属性值，开放或者关闭挂号
        redisTemplate.opsForHash().put(key, "open", open); // 更新挂号状态
    }

    /*
    * 查询在线医生列表
    * */
    public List<Map<String, Object>> selectOnlineDoctorList(String subName, String job) {
        List<Map<String, Object>> list = new ArrayList<>();

        // 获取所有在线医生的键集合
        Set<String> keys = redisTemplate.keys(ONLINE_DOCTOR_PREFIX + "*");

        for (String key : keys) {
            Map<String, Object> entries = redisTemplate.opsForHash().entries(key);

            // 过滤掉未开放状态的医生
            if (!MapUtil.getBool(entries, "open")) {
                continue;
            }

            // 获取属性值
            String tempSubName = MapUtil.getStr(entries, "subName");
            String tempJob = MapUtil.getStr(entries, "job");

            // 根据诊室和职称进行过滤
            if ((subName != null && !subName.equals(tempSubName)) ||
                    (job != null && !job.equals(tempJob))) {
                continue;
            }

            // 创建并填充医生信息
            Map<String, Object> map = new HashMap<>();
            map.put("doctorId", MapUtil.getInt(entries, "doctorId"));
            map.put("name", MapUtil.getStr(entries, "name"));
            map.put("photo", MapUtil.getStr(entries, "photo"));
            map.put("job", tempJob);
            map.put("description", MapUtil.getStr(entries, "description"));
            map.put("remark", MapUtil.getStr(entries, "remark"));
            map.put("price", MapUtil.getStr(entries, "price"));
            list.add(map);
        }
        return list;
    }

    @Override
    @Transactional
    public HashMap createVideoDiagnoseOrder(int userId, VideoDiagnose entity) {
        HashMap result = new HashMap();
        HashMap map = patientUserDao.selectOpenIdByUserId(userId);
        String openId = MapUtil.getStr(map, "openId");
        int patientCardId = MapUtil.getInt(map, "patientCardId");
        entity.setPatientCardId(patientCardId);
        String key = "online_doctor_" + entity.getDoctorId();
        //从缓存中获取该医生挂号金额
        String price = redisTemplate.opsForHash().get(key, "price").toString();
        int amount = new BigDecimal(price).multiply(new BigDecimal(100)).intValue();
        entity.setAmount(new BigDecimal(price));

        //尝试用Redis事务挂号
        boolean execute = (Boolean) redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获取医生上线缓存的Version
                operations.watch(key);
                //获取医生上线缓存数据
                Map entries = operations.opsForHash().entries(key);
                boolean open = MapUtil.getBool(entries, "open");
                //如果医生没开启挂号，就不能挂号
                if (!open) {
                    return false;
                }
                String nextPatient = MapUtil.getStr(entries, "nextPatient");
                //如果存在候诊的患者就不能挂号（只能存在一位候诊患者）
                if (!"none".equals(nextPatient)) {
                    return false;
                }
                try {
                        //开启Redis事务
                        operations.multi();
                        //把自己登记为候诊患者
                        operations.opsForHash().put(key, "nextPatient", patientCardId);
                        //提交事务
                        operations.exec();
                        return true;
                    } catch (Exception e) {
                        log.debug("事物提交失败", e);
                        return false;
                    }
                }
                });
            result.put("flag", execute);
            if (!execute) {
                return result;
            }
            DateTime now = new DateTime();
            //获取医生上线缓存数据
            Map entries = redisTemplate.opsForHash().entries(key);
            //当前问诊患者ID
            String currentPatient = MapUtil.getStr(entries, "currentPatient");

            String expectStart = null;
            String expectEnd = null;
            //如果没有当前问诊患者，定义预计开始时间和结束时间
            if ("none".equals(currentPatient)) {
                expectStart = now.offsetNew(DateField.MINUTE, 1).toString("yyyy-MM-dd HH:mm:ss");
                expectEnd = now.offsetNew(DateField.MINUTE, 16).toString("yyyy-MM-dd HH:mm:ss");
            }
            //如果有当前问诊患者，就以当前问诊结束时间预估开始和结束时间
            else {
                DateTime currentEnd = new DateTime(MapUtil.getStr(entries, "currentEnd"));
                expectStart = currentEnd.offsetNew(DateField.MINUTE, 1).toString("yyyy-MM-dd HH:mm:ss");
                expectEnd = currentEnd.offsetNew(DateField.MINUTE, 16).toString("yyyy-MM-dd  HH:mm:ss");
            }
            entity.setExpectStart(expectStart);
            entity.setExpectEnd(expectEnd);

            String outTradeNo = IdUtil.simpleUUID().toUpperCase();
            //设置支付单过期时间为1分钟。如果超过这个时间不付款，支付单自动关闭
            String timeExpire = now.offsetNew(DateField.MINUTE, 1).toString("yyyy-MM-dd'T'HH:mm:ss'+08:00'");

            //生成微信支付订单
            ObjectNode objectNode = paymentService.unifiedOrder(outTradeNo, openId, amount,"挂号费",notifyUrl,timeExpire);
            String prepayId = objectNode.get("prepay_id").textValue(); //预支付订单ID
            entity.setOutTradeNo(outTradeNo);
            entity.setPrepayId(prepayId);
            //保存视频问诊挂号记录
            videoDiagnosisDao.insert(entity);
            //查询视频问诊记录的主键值
            HashMap data = videoDiagnosisDao.selectByOutTradeNo(outTradeNo);
            int id = MapUtil.getInt(data, "id");
            HashMap cache = new HashMap();
            cache.put("nextOrder", id);
            cache.put("nextPayment", false);
            cache.put("nextStart", expectStart);
            cache.put("nextEnd", expectEnd);
            cache.put("nextNotify", false);
            //更新缓存数据
            redisTemplate.opsForHash().putAll(key, cache);
            //微信付款的相关信息
             result.put("outTradeNo", outTradeNo);
             result.put("prepayId", prepayId);
             result.put("timeStamp", objectNode.get("timeStamp").asText());
             result.put("nonceStr", objectNode.get("nonceStr").asText());
             result.put("package", objectNode.get("package").asText());
             result.put("signType", objectNode.get("signType").asText());
             result.put("paySign", objectNode.get("paySign").asText());
             result.put("videoDiagnoseId", id);
             result.put("expectStart", expectStart);
             result.put("expectEnd", expectEnd);
             //创建付款缓存，1分钟之后销毁
             String paymentKey = "patient_video_diagnose_payment#" + id;
             redisTemplate.opsForValue().set(paymentKey, false);
             //接收付款通知消息和主动查询付款结果都需要短暂的时间，所以缓存过期时间要设置大于60秒
             redisTemplate.expireAt(paymentKey, now.offset(DateField.SECOND, 90));
             return result;
    }

    @Override
    public HashMap selectVideoDiagnoseInfo(int userId) {
        HashMap map = new HashMap();
        int doctorId = this.selectRefIdIdAndJobById(userId);
        String key = "online_doctor_" + doctorId;
        if (!redisTemplate.hasKey(key)) {
            return map;
        }
        Map entries = redisTemplate.opsForHash().entries(key);
        String currentOrder = MapUtil.getStr(entries, "currentOrder");
        String nextOrder = MapUtil.getStr(entries, "nextOrder");
        if (!"none".equals(currentOrder)) {
            int diagnoseId = Integer.parseInt(currentOrder);
            HashMap currentInfo = videoDiagnosisDao.selectVideoDiagnoseInfo(diagnoseId);
            map.put("currentInfo", currentInfo);
        }
        if (!"none".equals(nextOrder)) {
            int diagnoseId = Integer.parseInt(nextOrder);
            HashMap nextInfo = videoDiagnosisDao.selectVideoDiagnoseInfo(diagnoseId);
            map.put("nextInfo", nextInfo);
        }
        return map;
    }

    @Override
    public HashMap refreshInfo(int userId) {
        int doctorId = this.selectRefIdIdAndJobById(userId);
        String key = "online_doctor_" + doctorId;
        //把医生上线缓存的部分属性数据取出来，保存到这个Map中返回给浏览器
        HashMap map = new HashMap();
        //不存在医生上线缓存的情况
        if (!redisTemplate.hasKey(key)) {
            map.put("status", "offline");
            return map;
        }

        Map entries = redisTemplate.opsForHash().entries(key);
        map.put("status", "online");
        map.put("open", MapUtil.getBool(entries, "open"));
        map.put("currentOrder", MapUtil.getStr(entries, "currentOrder"));
        Integer currentStatus = MapUtil.getInt(entries, "currentStatus");
        map.put("currentStatus", currentStatus);
        //如果患者已经付款会有程序往上线缓存中添加以及视频问诊的RoomId
        if (currentStatus != null && currentStatus == 2) {
            map.put("roomId", MapUtil.getStr(entries, "roomId"));
        }
        map.put("currentStart", MapUtil.getStr(entries, "currentStart"));
        map.put("currentEnd", MapUtil.getStr(entries, "currentEnd"));
        return map;
    }

    @Override
    @Transactional
    public void updatePayment(Map param) {
        String outTradeNo = MapUtil.getStr(param, "outTradeNo");
        //根据流水号查询挂号信息
        HashMap data = videoDiagnosisDao.selectByOutTradeNo(outTradeNo);
        int id = MapUtil.getInt(data, "id");
        int doctorId = MapUtil.getInt(data, "doctorId");

        String key = "online_doctor_" + doctorId;
        //获取医生上线缓存
        Map entries = redisTemplate.opsForHash().entries(key);
        //获取当前问诊挂号单ID
        String currentOrder = MapUtil.getStr(entries, "currentOrder");
        //获取排队问诊挂号单ID
        String nextOrder = MapUtil.getStr(entries, "nextOrder");

        //如果挂号单是当前问诊的，就更新上线缓存
        if (currentOrder.equals(id + "")) {
            redisTemplate.opsForHash().put(key, "currentPayment", true);
            redisTemplate.opsForHash().put(key, "currentNotify", false);
        }
        //如果挂号单是排队问诊的，就更新上线缓存
        else if (nextOrder.equals(id + "")) {
            redisTemplate.opsForHash().put(key, "nextPayment", true);
            redisTemplate.opsForHash().put(key, "nextNotify", false);
        }
        //如果挂号单既不属于当前问诊页不属于排队问诊，就执行退款
        else {
            log.error("没有找到缓存，无法跟新付款缓存");
            //TODO 执行退款或者通知运维人员
        }
        //更新数据库中的付款状态
        videoDiagnosisDao.updatePayment(param);
    }

    @Transactional
    @Override
    public boolean selectPaymentResult(String outTradeNo) {
//        String transactionId = paymentService.searchPaymentResult(outTradeNo);
        String transactionId = null;
        if (transactionId != null) {
            this.updatePayment(new HashMap() {{
                put("outTradeNo", outTradeNo);
                put("transactionId", transactionId);
                put("paymentStatus", 2);
            }});
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ArrayList<HashMap> selectImageByVideoDiagnoseId(int videoDiagnoseId) {
        ArrayList<HashMap> list = videoDiagnosisFileDao.selectImageByVideoDiagnoseId(videoDiagnoseId);
        return list;
    }

    @Override
    @Transactional
    public String uploadImage(MultipartFile file, Integer videoDiagnoseId) {
        try {
            // 随机生成文件名称
            String filename = IdUtil.simpleUUID() + ".jpg";

            // 照片存放在Minio存储桶中的相对路径
            String path = "patient-wx/video_diagnose/" + filename;

            // 在Minio中保存照片（文件不能超过5M）
            this.client.putObject(PutObjectArgs.builder()
                    .bucket("hospital")
                    .object(path)
                    .stream(file.getInputStream(), -1, 5 * 1024 * 1024)
                    .contentType("image/jpeg")
                    .build()
            );

            // 创建 VideoDiagnoseFile 实体并设置属性
            VideoDiagnoseFile entity = new VideoDiagnoseFile();
            entity.setVideoDiagnoseId(videoDiagnoseId);
            entity.setFilename(filename);
            entity.setPath(path);

            // 文件属性信息保存到数据表
            videoDiagnosisFileDao.insert(entity);

            return filename;
        } catch (Exception e) {
            log.error("保存上传图片失败", e);
            throw new GlobalException("保存上传图片失败");
        }
    }

    @Override
    @Transactional
    public void deleteImage(Map param) {
        Integer videoDiagnoseId = MapUtil.getInt(param, "videoDiagnoseId");

        // 判断是否包含 filename 参数
        String filename = MapUtil.getStr(param, "filename");

        // 删除指定照片
        if (filename != null) {
            this.removeImageFile(filename);
        }
        // 删除该视频问诊的所有照片
        else {
            // 查询该视频问诊的所有照片
            ArrayList<String> list = videoDiagnosisFileDao.selectVideoDiagnoseImage(videoDiagnoseId);
            list.forEach(this::removeImageFile);
        }

        // 删除数据表记录
        videoDiagnosisFileDao.delete(param);
    }



    private void removeImageFile(String filename) {
        try {
            String path = "patient-wx/video_diagnose/" + filename;
            // 删除文件
            this.client.removeObject(RemoveObjectArgs.builder()
                    .bucket("hospital")
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("删除视频问诊图片失败", e);
            throw new GlobalException("删除视频问诊图片失败");
        }
    }

    @Override
    public String selectRoomId(int doctorId) {
        String key = "online_doctor_" + doctorId;
        Map entries = redisTemplate.opsForHash().entries(key);
        String roomId = MapUtil.getStr(entries, "roomId");
        return roomId;
    }
}