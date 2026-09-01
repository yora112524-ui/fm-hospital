package com.hospital.wx.service;
import com.hospital.common.utils.PageUtils;
import com.hospital.wx.pojo.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface UserService {
    Integer userLogin(HashMap map);

    Set<String> selectUserPrivileges(int userId);

    int updateUserPassword(Map map);

    HashMap selectUserDetails(int userId);

    PageUtils selectUserByCondition(HashMap param);

    int insert(User user);

    int update(HashMap param);

    int deleteUserByIds(Integer[] ids);

    HashMap selectById(int userId);

    ArrayList<HashMap> selectAllUser();
}
