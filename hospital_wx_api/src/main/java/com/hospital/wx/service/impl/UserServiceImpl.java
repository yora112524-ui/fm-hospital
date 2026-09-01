package com.hospital.wx.service.impl;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.hospital.common.utils.PageUtils;
import com.hospital.wx.dao.UserDao;
import com.hospital.wx.pojo.User;
import com.hospital.wx.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    // 定义常量，用于截取 MD5 加密结果中的前缀和后缀部分
    private static final int PREFIX_LENGTH = 6;  // 从MD5哈希中获取的前缀长度
    private static final int SUFFIX_LENGTH = 3;  // 从MD5哈希中获取的后缀长度

    @Autowired
    private UserDao userDao;


    /**
     * 用户登录
     * */
    @Override
    public Integer userLogin(HashMap map) {
        // 从map中提取用户名和密码
        String username = (String) map.get("username");  // 获取用户名
        String password = (String) map.get("password");  // 获取密码

        // 使用自定义方法对密码进行加密处理
        password = encryptPassword(username, password);

        // 将加密后的密码重新放入map中
        map.put("password", password);

        // 调用 userDao 的登录方法，执行数据库操作并返回结果
        return userDao.login(map);
    }

    // 提取出密码加密的逻辑
    private String encryptPassword(String username, String password) {
        // 创建MD5实例来进行加密
        MD5 md5 = MD5.create();
        // 使用MD5对用户名进行加密生成哈希字符串
        String temp = md5.digestHex(username);
        // 从MD5结果中提取出前缀部分（前6个字符）
        String tempStart = StrUtil.subWithLength(temp, 0, PREFIX_LENGTH);
        // 从MD5结果中提取出后缀部分（最后3个字符）
        String tempEnd = StrUtil.subSuf(temp, temp.length() - SUFFIX_LENGTH);
        // 将前缀、密码和后缀拼接起来，并对拼接结果进行MD5加密
        return md5.digestHex(tempStart + password + tempEnd);
    }

    @Override
    public Set<String> selectUserPrivileges(int userId) {
        return userDao.selectUserPrivileges(userId);
    }

    @Override
    public int updateUserPassword(Map map) {
        //将map中的参数可以在这里对密码进行加密处理，或者在数据库中处理也可以。
        return userDao.updateUserPassword(map);
    }

    @Override
    public HashMap selectUserDetails(int userId) {
        return userDao.selectUserDetails(userId);
    }

    @Override
    public PageUtils selectUserByCondition(HashMap param) {
        ArrayList<HashMap> list = userDao.selectUserByCondition(param);
        int start=(Integer) param.get("start");
        int length=(Integer) param.get("length");
        PageUtils pageUtils = new PageUtils(list,userDao.selectUserByConditionCount(param),start,length);
        return pageUtils;
    }

    @Override
    @Transactional
    public int insert(User user) {
        //对密码进行加密
        user.setPassword(encryptPassword(user.getUsername(), user.getPassword()));
        return userDao.insert(user);
    }

    @Override
    public int update(HashMap param) {
        return userDao.update(param);
    }

    @Override
    public int deleteUserByIds(Integer[] ids) {
        return userDao.deleteUserByIds(ids);
    }

    @Override
    public HashMap selectById(int userId) {
        return userDao.selectById(userId);
    }

    @Override
    public ArrayList<HashMap> selectAllUser() {
        return null;
//        return userDao.selectAllUser();
    }
}
