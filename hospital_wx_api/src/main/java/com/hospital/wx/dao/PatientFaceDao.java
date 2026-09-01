package com.hospital.wx.dao;
import com.hospital.wx.pojo.FaceAuth;
import org.apache.ibatis.annotations.Mapper;
import java.util.Map;
@Mapper
public interface PatientFaceDao {

     Integer checkFaceAuthRecordForToday(Map param);

     int insert(FaceAuth entity);
}