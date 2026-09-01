package com.hospital.wx.service.impl;
import cn.hutool.core.map.MapUtil;
import com.hospital.wx.dao.MedicalDeptDao;
import com.hospital.wx.exception.GlobalException;
import com.hospital.wx.pojo.MedicalDept;
import com.hospital.wx.service.MedicalDeptService;
import com.hospital.wx.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class MedicalDeptServiceImpl implements MedicalDeptService {

    @Autowired
    private MedicalDeptDao medicalDeptDao;

    @Override
    public HashMap selectDeptAndSub() {
        ArrayList<HashMap> list = medicalDeptDao.selectDeptAndSub();
        LinkedHashMap<String,ArrayList> map = new LinkedHashMap<String,ArrayList>();
        // 遍历部门和子部门信息列表
        for (HashMap<String, Object> instance : list) {
            Integer subId = (Integer) instance.get("subId"); // 获取子部门ID
            String deptName = (String) instance.get("deptName"); // 获取科室名称
            String subName = (String) instance.get("subName"); // 获取子诊室名称
            // 将子部门信息添加到对应的部门列表中
            /*
            * computeIfAbsent：
            *   如果 Map 中存在指定的键，那么不会执行任何操作，直接返回该键对应的值，然后添加元素。
            * 例如：
            Map<String, List<Integer>> map = new HashMap<>();
            // 如果 "key" 不存在，则创建一个新的列表，并将其与 "key" 关联
            map.computeIfAbsent("key", k -> new ArrayList<>()).add(1);
            // 如果 "key" 存在，则直接返回现有列表，并向其添加元素
            map.computeIfAbsent("key", k -> new ArrayList<>()).add(2);
            // 打印结果
            System.out.println(map); // 输出：{key=[1, 2]}

            第二次调用 computeIfAbsent 方法时，不会创建新的列表，而是直接返回已存在的列表，并向其添加元素。
            *
            * */
            map.computeIfAbsent(deptName, k -> new ArrayList<>())
                    .add(new HashMap<>() {{
                        put("subId", subId);
                        put("subName", subName);
                    }});
        }
        return map;
    }

    @Override
    public ArrayList<HashMap> selectAll() {
        return medicalDeptDao.selectAll();
    }

    @Override
    public PageUtils selectConditionByPage(Map param) {
        ArrayList<HashMap> list = null;
        long count = medicalDeptDao.selectConditionByPageCount(param);
        if (count > 0) {
            list = medicalDeptDao.selectConditionByPage(param);
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
