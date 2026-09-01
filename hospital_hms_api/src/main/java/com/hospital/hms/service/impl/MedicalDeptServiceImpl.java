package com.hospital.hms.service.impl;
import cn.hutool.core.map.MapUtil;
import com.hospital.hms.dao.MedicalDeptDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.pojo.MedicalDept;
import com.hospital.hms.service.MedicalDeptService;
import com.hospital.common.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class MedicalDeptServiceImpl implements MedicalDeptService {

    @Autowired
    private MedicalDeptDao medicalDeptDao;

    @Override
    public PageUtils selectConditionByPage(Map param) {
        // 查询总记录数
        long count = medicalDeptDao.selectConditionByPageCount(param);

        // 如果没有数据，直接返回空的分页对象
        if (count == 0) {
            return new PageUtils(Collections.emptyList(), count,
                    MapUtil.getInt(param, "page"),
                    MapUtil.getInt(param, "length"));
        }

        // 查询分页数据
        List<HashMap<String, Object>> list = medicalDeptDao.selectConditionByPage(param);
        // 封装分页结果
        return new PageUtils(list, count,
                MapUtil.getInt(param, "page"),
                MapUtil.getInt(param, "length"));
    }



    @Override
    public HashMap selectDeptAndSub() {
        ArrayList<HashMap> list = medicalDeptDao.selectDeptAndSub();
        //定义结果集，保证数据存储是有序的
        LinkedHashMap<String,ArrayList> map = new LinkedHashMap();
        // 遍历list结果集，然后把数据组织成为满足要求的格式，添加map，再返回。
        for (HashMap<String, Object> instance : list) {
            String deptName = (String) instance.get("deptName"); // 获取科室名称
            Integer subId = (Integer) instance.get("subId"); // 获取诊室ID
            String subName = (String) instance.get("subName"); // 获取子诊室名称
            /**
             * 组织数据为如下结构
             *
             * map {
             *     key -> deptName
             *     value -> ArrayList{
             *         map{
             *             "subId": 1,
             *              "subName": "口腔颌面外科"
             *         },
             *         map {
             *              "subId": 2,
             *              "subName": "口腔颌面内科"
             *         }
             *     }
             * }
             *
             * */

            map.computeIfAbsent(deptName, k -> new ArrayList())
                    .add(new HashMap<>() {{
                        put("subId",subId);
                        put("subName",subName);
                    }});
        }
        return map;
    }

    @Override
    public ArrayList<HashMap> selectAllDeptNameAndId() {
        return medicalDeptDao.selectAllDeptNameAndId();
    }



    @Override
    @Transactional
    public void insert(MedicalDept medicalDept) {
        medicalDeptDao.insert(medicalDept);
    }

    @Override
    public HashMap selectById(int id) {
        return medicalDeptDao.selectById(id);
    }

    @Override
    @Transactional
    public void update(MedicalDept medicalDept) {
        medicalDeptDao.update(medicalDept);
    }

    @Override
    @Transactional
    public void deleteByIds(Integer[] ids) {
        //判断该科室下是否有关联诊室
        long count = medicalDeptDao.selectSubCountByIds(ids);
        if (count == 0) {
            //只有在没有关联诊室的情况下才能删除
            medicalDeptDao.deleteByIds(ids);
        } else {
            throw new GlobalException("科室存在关联诊室，无法删除记录");
        }
    }

    @Override
    public ArrayList<HashMap> selectMedicalDeptList(Map param) {
        return medicalDeptDao.selectMedicalDeptList(param);
    }
}
