package com.hospital.hms.dao;
import com.hospital.hms.pojo.PatientUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;

@Mapper
public interface PatientUserDao {

    public int insert(PatientUser entity);
    public Integer selectUserExistsByOpenId(String openId);
    public HashMap selectUserInfoById(int userId);

    public HashMap selectOpenIdByUserId(int userId);


}