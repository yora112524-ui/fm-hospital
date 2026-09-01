package com.hospital.hms.service.impl;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.hospital.hms.common.OperationMessage;
import com.hospital.hms.dao.PatientFaceDao;
import com.hospital.hms.dao.PatientUserInfoDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.FaceAuth;
import com.hospital.hms.service.PatientFaceService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.iai.v20180301.IaiClient;
import com.tencentcloudapi.iai.v20180301.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PatientFaceServiceImpl implements PatientFaceService {

    @Value("${tencent.cloud.secretId}")
    private String secretId;

    @Value("${tencent.cloud.secretKey}")
    private String secretKey;

    @Value("${tencent.cloud.face.groupName}")
    private String groupName;

    @Value("${tencent.cloud.face.region}")
    private String region;

    @Autowired
    private PatientFaceDao patientFaceDao;

    @Autowired
    private PatientUserInfoDao patientUserInfoDao;

    @Override
    public boolean hasFaceAuthInDay(Map param) {
        Integer id = patientFaceDao.hasFaceAuthInDay(param);
        boolean bool = (id != null);
        return bool;
    }

    /**
     * 人脸信息录入
     * */
    @Override
    public void createFaceModel(Map param) {
        // 从参数中获取用户ID和照片
        int userId = MapUtil.getInt(param, "userId");
        String photo = MapUtil.getStr(param, "photo");

        // 创建腾讯云身份认证凭证
        Credential cred = new Credential(secretId, secretKey);
        IaiClient client = new IaiClient(cred, region);
        //查询用户姓名和性别
        HashMap map = patientUserInfoDao.selectPatientUserInfoByUserId(userId);
        String name = MapUtil.getStr(map, "name");
        String sex = MapUtil.getStr(map, "sex");

        // 创建人员请求对象,请求腾讯云的服务器
        CreatePersonRequest req = new CreatePersonRequest();
        // 设置人员所属分组，放到你所指定的人员库中，后面可以从该人员库中查询到。
        req.setGroupId(groupName);
        // 将用户ID转换为字符串作为人员ID
        req.setPersonId(userId + "");
        // 根据性别设置性别参数（1代表男性，2代表女性）
        long gender = sex.equals("男") ? 1L : 2L;
        req.setGender(gender);
        // 设置图像质量控制参数
        req.setQualityControl(4L);
        // 设置唯一性控制参数
        req.setUniquePersonControl(4L);
        // 设置人员姓名
        req.setPersonName(name);
        // 设置图像数据
        req.setImage(photo);
        try {
            // 发起创建人员请求
            CreatePersonResponse resp = client.CreatePerson(req);
            // 如果返回的人脸ID不为空，表示人脸模型创建成功
            if (StrUtil.isNotBlank(resp.getFaceId())) {
                // 更新数据库，标记用户已经录入人脸模型
                patientUserInfoDao.updateExistFaceModel(new HashMap() {{
                    put("userId", userId);
                    put("existFaceModel", true);
                }});
            }
        }
        catch (TencentCloudSDKException e) {
            // 捕获腾讯云SDK异常并抛出全局异常
            throw new GlobalException(e);
        }
    }

    @Override
    @Transactional
    public boolean verifyFaceModel(int userId, String photo) {
        try {
            // 创建腾讯云凭证
            Credential cred = new Credential(secretId, secretKey);
            // 创建人脸识别客户端
            IaiClient client = new IaiClient(cred, region);

            // 执行人脸识别
            VerifyPersonRequest verifyPersonRequest = new VerifyPersonRequest();
            verifyPersonRequest.setPersonId(userId + ""); // 设置用户ID
            verifyPersonRequest.setImage(photo); // 设置照片
            verifyPersonRequest.setQualityControl(4L); // 设置质量控制参数
            // 发送请求
            VerifyPersonResponse verifyPersonResponse = client.VerifyPerson(verifyPersonRequest);
            // 获取识别结果
            Boolean isMatch = verifyPersonResponse.getIsMatch();

            // 如果不匹配，直接返回结果
            if (!isMatch) {
                return isMatch;
            }


            /**
             * 活体检测通常用于检测图像或视频中的动态人脸，以验证人脸是否属于真实的生物个体而不是静态图像或模型。活体检测会评估人脸在图像或视频中的特征变化，
             * 例如眨眼、张嘴、头部转动等，以确认其为真实的生物特征。因此，可以说活体检测是用于检测动态人脸的一种技术。
             * */
            // 执行静态活体检测
            DetectLiveFaceRequest detectLiveFaceRequest = new DetectLiveFaceRequest();
            detectLiveFaceRequest.setImage(photo); // 设置照片
            DetectLiveFaceResponse detectLiveFaceResponse = client.DetectLiveFace(detectLiveFaceRequest); // 发送请求

            // 获取活体检测结果
            Boolean isLiveness = detectLiveFaceResponse.getIsLiveness();
            // 综合人脸识别和活体识别结果
            boolean result = isMatch && isLiveness;

            // 如果身份验证通过，写入数据库
            if (result) {
                Integer patientUserInfoId = patientUserInfoDao.selectIdByUserId(userId); // 查询患者用户信息ID
                FaceAuth entity = new FaceAuth();
                entity.setPatientCardId(patientUserInfoId); // 设置患者卡号
                entity.setDate(DateUtil.today()); // 设置日期
                patientFaceDao.insert(entity); // 插入数据
            }

            return result; // 返回最终结果
        } catch (TencentCloudSDKException e) {
            log.error(OperationMessage.FACE_VERIFICATION_FAILED.toString(), e); // 记录错误日志
            return false; // 出现异常，返回false
        }
    }
}
