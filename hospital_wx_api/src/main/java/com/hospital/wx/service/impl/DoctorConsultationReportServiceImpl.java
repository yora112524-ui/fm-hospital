package com.hospital.wx.service.impl;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hospital.wx.dao.DoctorConsultationReportDao;
import com.hospital.wx.service.DoctorConsultationReportService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class DoctorConsultationReportServiceImpl implements DoctorConsultationReportService {

    @Resource
    private DoctorConsultationReportDao doctorConsultationReportDao;

    public HashMap selectConsultationReportByRegistrationId(Map param) {
        HashMap map = this.doctorConsultationReportDao.selectConsultationReportByRegistrationId(param);
        String birthday = MapUtil.getStr(map, "patientBirthday");
        DateTime dateTime = new DateTime(birthday, "yyyy-MM-dd");
        int age = DateUtil.ageOfNow(dateTime);
        map.put("patientAge", age);
        map.remove("patientBirthday");
        String rp = MapUtil.getStr(map, "rp");
        JSONArray array = JSONUtil.parseArray(rp);
        map.replace("rp", array);
        return map;
    }
}
