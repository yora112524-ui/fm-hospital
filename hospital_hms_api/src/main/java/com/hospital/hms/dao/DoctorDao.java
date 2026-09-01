package com.hospital.hms.dao;
import com.hospital.hms.pojo.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface DoctorDao {

    /**
     * 分页查询
     * */
    ArrayList<HashMap> selectConditionByPage(Map param);

    long selectConditionByPageCount(Map param);

    HashMap selectDoctorDetailById(int id);

    void insert(Doctor doctor);
    Integer selectIdByUUID(String uuid);
    void updatePicture(Map map);

    HashMap selectById(int id);

    void update(Map map);

    void deleteByIds(Integer[] ids);

    ArrayList<HashMap> selectDoctorBySubId(int subId);

    HashMap selectDoctorInfoById(int id);

    /**
     * 查询需要缓存的医生信息
     * */
    HashMap getDoctorDetailsForCache(int doctorId);

}
