package com.hospital.hms.service.impl;
import cn.hutool.core.map.MapUtil;
import com.hospital.hms.dao.MedicalDeptSubDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.MedicalDeptSub;
import com.hospital.hms.service.MedicalDeptSubService;
import com.hospital.hms.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MedicalDeptSubServiceImpl implements MedicalDeptSubService {

    @Autowired
    private MedicalDeptSubDao medicalDeptSubDao;

    @Override
    public PageUtils selectConditionByPage(Map param) {
        ArrayList<HashMap> list = null;
        long count = medicalDeptSubDao.selectConditionByPageCount(param);
        if (count > 0) {
            list = medicalDeptSubDao.selectConditionByPage(param);
        } else {
            list = new ArrayList<>();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, count, page, length);
        return pageUtils;
    }

    @Override
    @Transactional
    public void insert(MedicalDeptSub medicalDeptSub) {
        medicalDeptSubDao.insert(medicalDeptSub);
    }

    @Override
    public HashMap selectById(int id) {
        return medicalDeptSubDao.selectById(id);
    }

    @Override
    public void update(MedicalDeptSub medicalDeptSub) {
        medicalDeptSubDao.update(medicalDeptSub);
    }

    @Override
    public void deleteByIds(Integer[] ids) {
        long count = medicalDeptSubDao.selectIncludeDoctorCount(ids);
        if (count == 0) {
            medicalDeptSubDao.deleteByIds(ids);
        } else {
            throw new GlobalException("诊室存在关联医生，无法删除记录");
        }
    }

    @Override
    public ArrayList<HashMap> selectMedicalDeptSubList(int deptId) {
        return medicalDeptSubDao.selectMedicalDeptSubList(deptId);
    }
}
