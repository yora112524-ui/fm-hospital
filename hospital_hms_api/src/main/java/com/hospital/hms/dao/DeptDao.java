package com.hospital.hms.dao;

import com.hospital.hms.pojo.Dept;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface DeptDao {

    public ArrayList<HashMap> selectAll();

    public HashMap selectById(int id);

    public ArrayList<HashMap> selectDeptByCondition(HashMap param);

    public long selectDeptByConditionCount(HashMap param);

    public long selectDeptCount(HashMap param);

    public boolean selectCanDelete(Integer[] ids);

    public int insert(Dept dept);

    public int update(Dept dept);
    public int deleteDeptByIds(Integer[] ids);
}