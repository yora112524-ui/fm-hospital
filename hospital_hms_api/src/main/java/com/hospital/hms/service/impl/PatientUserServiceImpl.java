package com.hospital.hms.service.impl;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.dao.PatientUserDao;
import com.hospital.hms.dao.PatientUserInfoDao;
import com.hospital.hms.pojo.PatientUser;
import com.hospital.hms.service.PatientUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class PatientUserServiceImpl implements PatientUserService {
    @Value("${wechat.app-id}")
    private String appId;

    @Value("${wechat.app-secret}")
    private String appSecret;

    @Autowired
    private PatientUserDao patientUserDao;

    @Autowired
    private PatientUserInfoDao patientUserInfoDao;


    @Override
    public HashMap loginOrRegister(String code, String nickname, String photo, String sex) {
        /**
         * 将临时授权码兑换为open_id
         * 每个微信的用户都对应一个唯一的open_id。
         * */
        String openId = getOpenId(code);
        //构建返回结果
        HashMap result = new HashMap();
        //判断当前登录的用户是否为已注册用户
        Integer id = patientUserDao.selectUserExistsByOpenId(openId);
        //之前没有登录过
        if (id == null) {
            //把登录信息保存在数据库中
            PatientUser entity = new PatientUser();
            entity.setOpenId(openId);
            entity.setNickname(nickname);
            entity.setPhoto(photo);
            entity.setSex(sex);
            entity.setStatus((byte)1);
            //保存新用户信息到数据库
            patientUserDao.insert(entity);
            //重新得到用户的ID
            id = patientUserDao.selectUserExistsByOpenId(openId);
            result.put("msg", "注册成功");
        } else {
            //如果之前登录过，消息就是登陆成功
            result.put("msg", "登陆成功");
        }
        String tel = patientUserInfoDao.selectTelById(id);
        if (tel != null && tel.length() > 0) {
            result.put("tel",tel);
        }
        result.put("id", id);
        return result;
    }

    /**
     * 根据临时的授权码转换为OpenId
     * */
   private String getOpenId(String code) {
        //微信官方提供的服务器URL
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        //组织请求参数
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        //发送POST请求到微信服务器，进行兑换
        String response = HttpUtil.post(url, map);
        //将响应的数据解析为json格式，然后取出openId
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        //判断是否生成openId
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    @Override
    public HashMap selectUserInfoById(int userId) {
        HashMap map = patientUserDao.selectUserInfoById(userId);
        String tel = patientUserInfoDao.selectTelById(userId);
        map.put("tel", tel);
        return map;
    }
}
