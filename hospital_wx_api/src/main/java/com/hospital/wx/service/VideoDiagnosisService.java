package com.hospital.wx.service;

import com.hospital.wx.pojo.VideoDiagnose;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface VideoDiagnosisService {
    //医生上线
    void online(int userId);

    //医生下线
    boolean offline(int userId);

    // 开放/关闭 挂号按钮
    void updateOpenFlag(int userId, boolean open);

    //查询在线医生列表
    List<Map<String, Object>> selectOnlineDoctorList(String subName, String job);

    //创建挂号单
    HashMap createVideoDiagnoseOrder(int userId, VideoDiagnose entity);


    //根据挂号单ID查询患者信息
    HashMap selectVideoDiagnoseInfo(int userId);

    HashMap refreshInfo(int userId);

    void updatePayment(Map param);

    boolean selectPaymentResult(String outTradeNo);

    ArrayList<HashMap> selectImageByVideoDiagnoseId(int videoDiagnoseId);

    String uploadImage(MultipartFile file, Integer videoDiagnoseId);

    void deleteImage(Map param);

    String selectRoomId(int doctorId);


}
