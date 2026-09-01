package com.hospital.hms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.dao.DoctorDao;
import com.hospital.hms.dao.MedicalDeptSubDoctorDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.Doctor;
import com.hospital.hms.pojo.MedicalDeptSubDoctor;
import com.hospital.hms.service.DoctorService;
import com.hospital.common.utils.PageUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Slf4j
public class DoctorServiceImpl implements DoctorService {


    @Autowired
    private DoctorDao doctorDao;

    @Autowired
    private MedicalDeptSubDoctorDao medicalDeptSubDoctorDao;

    /**
     * 分页查询
     * */
    @Override
    public PageUtils selectConditionByPage(Map param) {
        // 确保参数不为空，并且包含 "page" 和 "length"
        int page = MapUtil.getInt(param, "page", 1);  // 默认为1
        int length = MapUtil.getInt(param, "length", 10);  // 默认为10
        // 查询满足条件的记录数
        long count = doctorDao.selectConditionByPageCount(param);
        // 如果没有记录，返回空列表
        if (count == 0) {
            return new PageUtils(Collections.emptyList(), count, page, length);
        }
        // 查询具体的记录
        List<HashMap> list = doctorDao.selectConditionByPage(param);
        // 返回分页结果
        return new PageUtils(list, count, page, length);
    }

    @Override
    public HashMap selectDoctorDetailById(int id) {
        HashMap hashMap = doctorDao.selectDoctorDetailById(id);
        /**
         * 字符串转换为JSON数组
         * 原始 tag -> ["领域专家","温暖贴心","人很好"]
         * 转换后：tag -> {JSONArray@13033}  size = 3
         *
         * 原始数据：
         * String tag = "[\"领域专家\", \"温暖贴心\", \"人很好\"]";
         *
         * 转换后的数据：
         * JSONArray tagArray = JSONUtil.parseArray("[\"领域专家\", \"温暖贴心\", \"人很好\"]");
         * System.out.println(tagArray.get(0)); // 输出 "领域专家"
         * */
        hashMap.replace("tag",JSONUtil.parseArray(MapUtil.getStr(hashMap,"tag")));
        return hashMap;
    }

    @Transactional
    @Override
    public void insert(Map param) {
        try {
            //将map转换为doctor实例
            Doctor doctor = BeanUtil.toBean(param, Doctor.class);
            //保存到数据库
            doctorDao.insert(doctor);

            //保存医生和诊室的关联关系到medical_dept_sub_doctor表

            /**
             * useGeneratedKeys="true" keyProperty="id"
             * 进行插入操作时，如果数据库中的主键是自动生成的（如自增主键），你希望插入数据后能够自动获取这个生成的主键值并赋值给 Java 对象的属性，
             * 可以通过这两个配置来实现。
             * */
            Integer doctorId = doctor.getId(); // 获取 doctorId

            //医生关联诊室
            Integer subId = MapUtil.getInt(param, "subId");
            MedicalDeptSubDoctor medicalDeptSubDoctor = new MedicalDeptSubDoctor();
            medicalDeptSubDoctor.setDoctorId(doctorId);
            medicalDeptSubDoctor.setDeptSubId(subId);
            medicalDeptSubDoctorDao.insert(medicalDeptSubDoctor);
        } catch (Exception e) {
            log.error("Error occurred while inserting doctor and medicalDeptSubDoctor: ", e);
            // 抛出异常以触发事务回滚
            throw e;
        }
    }

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final String DOCTOR_FILE_PREFIX = "doctor-";  // 文件名前缀
    private static final String FILE_EXTENSION = ".jpg";  // 文件扩展名
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;  // 最大文件上传大小，5MB

    @Override
    @Transactional
    public void updatePicture(MultipartFile file, Integer doctorId) {
        try {
            //组织文件名称
            String filename = DOCTOR_FILE_PREFIX + doctorId + FILE_EXTENSION;

            // 使用构建器模式初始化 MinioClient，连接到Minio服务器
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)  // Minio服务器地址
                    .credentials(accessKey, secretKey)  // Minio认证的accessKey和secretKey
                    .build();

            // 上传文件到Minio服务器
            /*
            * -1：表示流的大小不确定或不需要提前指定。MinioClient 会通过流的结束来自动判断文件的大小。
            * 它在处理不确定大小的文件时非常有用，尤其是当你没有提前知道文件的大小时。
            * */
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)  // 指定存储桶的名称
                            .object("doctor/" + filename)  // 设置存储路径和文件名
                            .stream(file.getInputStream(), -1, MAX_FILE_SIZE)  // 设置上传流、文件长度和最大文件大小，-1
                            .contentType("image/jpeg")  // 设置文件类型为JPEG图片
                            .build()
            );

            // 封装更新图片地址的参数，并更新数据库
            HashMap<String, Object> updateParams = new HashMap<>();
            updateParams.put("id", doctorId);  // 设置医生ID
            updateParams.put("photo", "/doctor/" + filename);  // 设置图片路径
            // 更新数据库中的图片路径信息
            doctorDao.updatePicture(updateParams);
        }catch (Exception e) {
            log.error("照片更新失败",e);
            throw new GlobalException("照片更新失败");
        }
    }

    @Override
    public HashMap selectById(int id) {
        // 从 doctorDao 获取结果，返回类型使用 Map 而不是原始 HashMap，确保类型安全
        HashMap result = doctorDao.selectById(id);
        // 检查 result 是否为 null 并且包含 "tag" 字段
        if (result != null && result.containsKey("tag")) {
            // 获取 "tag" 字段的值，假设其为字符串类型
            String tag = (String) result.get("tag");
            // 如果 tag 不为 null 且不为空
            if (tag != null && !tag.isEmpty()) {
                try {
                    // 将 tag 字符串解析为 JSONArray
                    JSONArray array = JSONUtil.parseArray(tag);
                    // 将解析后的 JSONArray 替换原来的 tag 字段值
                    result.put("tag", array);
                } catch (JSONException e) {
                    // 如果发生异常，记录日志
                    log.error("Error parsing tag JSON: ", e);
                }
            }
        }
        // 返回处理后的结果
        return result;
    }

    @Override
    @Transactional
    public void update(Map map) {
        try {
            // 先更新医生的信息
            doctorDao.update(map);
            // 修改键名：将 "id" 改为 "doctorId"
            Map<String, Object> updatedMap = MapUtil.renameKey(map, "id", "doctorId");
            // 再更新关联的诊室信息
            medicalDeptSubDoctorDao.updateDoctorSubDept(updatedMap);
        } catch (Exception e) {
            // 异常捕获，记录日志，并重新抛出异常确保事务回滚
            log.error("Error updating doctor and department information: ", e);
            throw new RuntimeException("Failed to update doctor and department information", e);
        }
    }

    @Override
    @Transactional
    public void deleteDoctorByIds(Integer[] ids) {
        try {
            // 删除医生集合
            doctorDao.deleteByIds(ids);
            // 删除该医生关联的诊室数据
            medicalDeptSubDoctorDao.deleteByDoctorId(ids);
        } catch (Exception e) {
            // 异常捕获并记录日志
            log.error("Error deleting doctors with ids: {}", ids, e);
            throw new RuntimeException("Failed to delete doctors and their related departments.", e);
        }
    }

    @Override
    public ArrayList<HashMap> selectDoctorBySubId(int subId) {
        return doctorDao.selectDoctorBySubId(subId);
    }

    @Override
    public HashMap selectDoctorInfoById(int id) {
        return doctorDao.selectDoctorInfoById(id);
    }
}
