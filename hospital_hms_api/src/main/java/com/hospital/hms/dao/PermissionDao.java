package com.hospital.hms.dao;

import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface PermissionDao {
    public ArrayList<HashMap> selectAllPermission();
}