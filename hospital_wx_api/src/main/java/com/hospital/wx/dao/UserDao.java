package com.hospital.wx.dao;
import com.hospital.wx.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mapper
public interface UserDao {
    Integer login(HashMap map);

    Set<String> selectUserPrivileges(int userId);
    /**
     * 通过用户id查询关联的医生ID和Job信息
     * */
    HashMap selectDoctorIdByUserId(int id);

    Integer selectUserIdByRefId(Map param);

    int updateUserPassword(Map map);

    HashMap selectUserDetails(int userId);

    ArrayList<HashMap> selectUserByCondition(HashMap param);

    long selectUserByConditionCount(HashMap params);

    int insert(User user);

    int update(HashMap param);

    int deleteUserByIds(Integer[] ids);

    HashMap selectById(int userId);

}