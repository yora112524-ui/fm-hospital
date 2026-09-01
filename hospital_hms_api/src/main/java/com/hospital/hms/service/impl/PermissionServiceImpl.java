package com.hospital.hms.service.impl;

import com.hospital.hms.dao.PermissionDao;
import com.hospital.hms.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionDao permissionDao;

    @Override
    public ArrayList<HashMap> selectAllPermission() {
        return permissionDao.selectAllPermission();
    }
}
