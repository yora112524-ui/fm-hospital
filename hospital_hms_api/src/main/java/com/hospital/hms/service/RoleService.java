package com.hospital.hms.service;
import com.hospital.hms.pojo.Role;
import com.hospital.common.utils.PageUtils;
import java.util.ArrayList;
import java.util.HashMap;

public interface RoleService {
    ArrayList<HashMap> selectAllRole();

    HashMap selectById(int id);

    PageUtils selectRoleByCondition(HashMap param);

    boolean selectCanDelete(Integer[] ids);

    ArrayList<Integer> selectUserByRoleId(int roleId);

    int update(Role role);

    int insert(Role role);

    int deleteRoleByIds(Integer[] ids);
}
