package com.hospital.wx.service;
import com.hospital.wx.utils.PageUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorService {



    HashMap selectDoctorInfoById(int id);





    public PageUtils selectConditionByPage(Map param);

    public HashMap selectDoctorDetailById(int id);

    public void insert(Map param);

    public void updatePicture(MultipartFile file,Integer doctorId);

    public HashMap selectById(int id);

    public void update(Map map);

    public void deleteDoctorByIds(Integer[] ids);

    public ArrayList<HashMap> selectDoctorBySubId(int subId);



}
