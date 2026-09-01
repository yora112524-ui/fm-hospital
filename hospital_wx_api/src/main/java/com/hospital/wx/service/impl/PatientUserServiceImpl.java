package com.hospital.wx.service.impl;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hospital.common.utils.WxAppUtil;
import com.hospital.wx.dao.PatientUserDao;
import com.hospital.wx.dao.PatientUserInfoDao;
import com.hospital.wx.pojo.PatientUser;
import com.hospital.wx.service.PatientUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class PatientUserServiceImpl implements PatientUserService {

    @Autowired
    private WxAppUtil wxAppUtil;

    @Autowired
    private PatientUserDao patientUserDao;

    @Autowired
    private PatientUserInfoDao patientUserInfoDao;

    @Override
    @Transactional
    public Map<String, Object> loginOrRegister(String code, String nickname, String photo, String sex) {
        // 获取 openId
        String openId = wxAppUtil.getOpenId(code);

        // 检查用户是否存在
        Integer id = patientUserDao.existsUserByOpenId(openId);

        // 如果用户未注册，进行注册流程
        if (id == null) {
            id = registerNewUser(openId, nickname, photo, sex);
            return buildResultMap("注册成功", id);
        } else {
            // 如果用户已存在，返回登录成功
            return buildResultMap("登陆成功", id);
        }
    }

    // 注册新用户的逻辑
    private Integer registerNewUser(String openId, String nickname, String photo, String sex) {
        PatientUser entity = new PatientUser();
        entity.setOpenId(openId);
        entity.setNickname(nickname);
        entity.setPhoto(photo);
        entity.setSex(sex);
        entity.setStatus((byte) 1);

        // 保存新用户信息到数据库
        patientUserDao.insert(entity);

        // 返回新用户的 ID
        return entity.getId();
    }

    // 构建返回结果 Map
    private Map<String, Object> buildResultMap(String msg, Integer id) {
        Map<String, Object> result = new HashMap<>();
        result.put("msg", msg);
        result.put("id", id);

        // 获取用户的电话信息，如果存在则返回
        String tel = patientUserInfoDao.selectTelById(id);
        if (tel != null && tel.length() > 0) {
            result.put("tel", tel);
        }
        return result;
    }

    @Override
    public HashMap selectUserInfoById(int userId) {
        HashMap map = patientUserDao.selectUserInfoById(userId);
        String tel = patientUserInfoDao.selectTelById(userId);
        map.put("tel", tel);
        return map;
    }
}
