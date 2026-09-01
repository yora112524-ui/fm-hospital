package com.hospital.hms.dao;

import com.hospital.hms.pojo.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;


@Mapper
public interface RoleDao {
    public ArrayList<HashMap> selectAllRole();

    public HashMap selectById(int id);

    public ArrayList<HashMap> selectRoleByCondition(HashMap params);

    public long  selectRoleByConditionCount(HashMap params);

    public boolean selectCanDelete(Integer[] ids);

    public ArrayList<Integer> selectUserByRoleId(int roleId);

    public int update(Role role);

    public int insert(Role role);

    public int deleteRoleByIds(Integer[] ids);
}