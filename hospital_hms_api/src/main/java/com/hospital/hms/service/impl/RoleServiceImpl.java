package com.hospital.hms.service.impl;
import com.hospital.hms.dao.RoleDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.Role;
import com.hospital.hms.service.RoleService;
import com.hospital.common.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleDao roleDao;

    @Override
    public ArrayList<HashMap> selectAllRole() {
        return roleDao.selectAllRole();
    }

    @Override
    public HashMap selectById(int id) {
        return roleDao.selectById(id);
    }

    @Override
    public PageUtils selectRoleByCondition(HashMap param) {
        ArrayList<HashMap> list = roleDao.selectRoleByCondition(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtils pageUtils = new PageUtils(list, roleDao.selectRoleByConditionCount(param), start, length);
        return pageUtils;
    }

    @Override
    public int update(Role role) {
        return roleDao.update(role);
    }

    @Override
    public int insert(Role role) {
        return roleDao.insert(role);
    }

    @Override
    public boolean selectCanDelete(Integer[] ids) {
        return roleDao.selectCanDelete(ids);
    }

    @Override
    public ArrayList<Integer> selectUserByRoleId(int roleId) {
        return roleDao.selectUserByRoleId(roleId);
    }



    @Override
    public int deleteRoleByIds(Integer[] ids) {
        if (!roleDao.selectCanDelete(ids)) {
            throw new GlobalException("无法删除关联用户的角色");
        }
        return roleDao.deleteRoleByIds(ids);
    }
}
