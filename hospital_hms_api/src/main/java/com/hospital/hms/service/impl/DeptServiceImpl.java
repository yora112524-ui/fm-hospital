package com.hospital.hms.service.impl;
import com.hospital.hms.dao.DeptDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.Dept;
import com.hospital.hms.service.DeptService;
import com.hospital.common.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptDao deptDao;

    @Override
    public ArrayList<HashMap> selectAll() {
        return deptDao.selectAll();
    }

    @Override
    public HashMap selectById(Integer id) {
        return deptDao.selectById(id);
    }

    @Override
    public PageUtils selectDeptByCondition(HashMap param) {
        ArrayList<HashMap> list = deptDao.selectDeptByCondition(param);
        long count = deptDao.selectDeptByConditionCount(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtils pageUtils = new PageUtils(list, count, start, length);
        return pageUtils;
    }

    @Override
    public int insert(Dept dept) {
        return deptDao.insert(dept);
    }

    @Override
    public int update(Dept dept) {
        return deptDao.update(dept);
    }

    @Override
    public int deleteDeptByIds(Integer[] ids) {
        if (!deptDao.selectCanDelete(ids)) {
            throw new GlobalException("无法删除关联用户的部门");
        }
        int rows = deptDao.deleteDeptByIds(ids);
        return rows;
    }
}
