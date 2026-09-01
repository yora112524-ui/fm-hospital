package com.hospital.wx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.hospital.wx.dao.DoctorDao;
import com.hospital.wx.dao.MedicalDeptSubDoctorDao;
import com.hospital.wx.exception.GlobalException;
import com.hospital.wx.pojo.Doctor;
import com.hospital.wx.pojo.MedicalDeptSubDoctor;
import com.hospital.wx.service.DoctorService;
import com.hospital.wx.utils.PageUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorDao doctorDao;

    @Autowired
    private MedicalDeptSubDoctorDao medicalDeptSubDoctorDao;

    /**
     * 分页查询的逻辑
     * */
    @Override
    public PageUtils selectConditionByPage(Map param) {
        ArrayList<HashMap> list = null;
        //先查询是否有满足条件的记录
        long count = doctorDao.selectConditionByPageCount(param);
        if (count > 0) {
            list = doctorDao.selectConditionByPage(param);
        } else {
            list = new ArrayList<>();
        }
        /*
        * 创建一个分页对象，并且返回
        * */
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, count, page, length);
        return pageUtils;
    }

    @Override
    public HashMap selectDoctorDetailById(int id) {
        HashMap hashMap = doctorDao.selectDoctorDetailById(id);
        //字符串转换为JSON数组
        hashMap.replace("tag",JSONUtil.parseArray(MapUtil.getStr(hashMap,"tag")));
        return hashMap;
    }

    @Override
    public void insert(Map param) {
        Doctor doctor = BeanUtil.toBean(param, Doctor.class);
        doctorDao.insert(doctor);

        //根据uuid查询医生主键值
        String uuid = doctor.getUuid();
        Integer doctorId = doctorDao.selectIdByUUID(uuid);

        //保存医生关联的诊室记录到medical_dept_sub_doctor表
        Integer subId = MapUtil.getInt(param, "subId");
        MedicalDeptSubDoctor medicalDeptSubDoctor = new MedicalDeptSubDoctor();
        medicalDeptSubDoctor.setDoctorId(doctorId);
        medicalDeptSubDoctor.setDeptSubId(subId);
        medicalDeptSubDoctorDao.insert(medicalDeptSubDoctor);
    }

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    @Transactional
    public void updatePicture(MultipartFile file, Integer doctorId) {
        try {
            String filename = "doctor-"+doctorId+".jpg";
            /*
            * 通过MinioClient操作Minio图片服务器，需要传递如下参数：
            *   1. 地址：endpoint
            *   2. 账号：accessKey
            *   3. 密码：secretKey
            *   4. bucket名：yiliao
            *
            * bucket：要根据实际上创建的来填写yiliao3
            *
            * */
            MinioClient minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey,secretKey).build();
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName)
                    .object("doctor/"+filename)
                    .stream(file.getInputStream(),-1,5 * 1024 * 1024)
                    .contentType("image/jpeg").build());

            //封装参数，更新数据库信息
            doctorDao.updatePicture(new HashMap() {{
                put("id",doctorId);
                put("photo","/doctor/"+filename);
            }});

        }catch (Exception e) {
            log.error("照片更新失败");
            e.printStackTrace();
            throw new GlobalException("照片更新失败");
        }
    }

    @Override
    public HashMap selectById(int id) {
        HashMap hashMap = doctorDao.selectById(id);
        if (hashMap != null) {
            String tag = (String) hashMap.get("tag");
            if (tag != null && !tag.isEmpty()) {
                try {
                    JSONArray array = JSONUtil.parseArray(tag);
                    hashMap.put("tag", array);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return hashMap;
    }

    @Override
    @Transactional
    public void update(Map map) {
        //先更新医生的信息
        doctorDao.update(map);
        map = MapUtil.renameKey(map, "id", "doctorId");
        //再更新关联的诊室
        medicalDeptSubDoctorDao.updateDoctorSubDept(map);
    }

    @Override
    @Transactional
    public void deleteDoctorByIds(Integer[] ids) {
        doctorDao.deleteByIds(ids);
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
