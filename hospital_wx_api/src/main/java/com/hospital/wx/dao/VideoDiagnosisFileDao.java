package com.hospital.wx.dao;
import com.hospital.wx.pojo.VideoDiagnoseFile;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mapper
public interface VideoDiagnosisFileDao {

    ArrayList<HashMap> selectImageByVideoDiagnoseId(int videoDiagnoseId);

    void insert(VideoDiagnoseFile entity);

    void delete(Map param);
    ArrayList<String> selectVideoDiagnoseImage(int videoDiagnoseId);
}




