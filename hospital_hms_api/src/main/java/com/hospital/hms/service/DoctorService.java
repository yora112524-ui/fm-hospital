package com.hospital.hms.service;
import com.hospital.common.utils.PageUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorService {
    PageUtils selectConditionByPage(Map param);

    HashMap selectDoctorDetailById(int id);

    void insert(Map param);

    void updatePicture(MultipartFile file,Integer doctorId);

    HashMap selectById(int id);

    void update(Map map);

    void deleteDoctorByIds(Integer[] ids);

    ArrayList<HashMap> selectDoctorBySubId(int subId);

    HashMap selectDoctorInfoById(int id);

}
