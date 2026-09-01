package com.hospital.hms.service;
import com.hospital.hms.pojo.Dept;
import com.hospital.common.utils.PageUtils;
import java.util.ArrayList;
import java.util.HashMap;

public interface DeptService {

    ArrayList<HashMap> selectAll();

    HashMap selectById(Integer id);

    PageUtils selectDeptByCondition(HashMap param);

    int insert(Dept dept);

    int update(Dept dept);

    int deleteDeptByIds(Integer[] ids);
}
