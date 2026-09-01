package com.hospital.wx.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.hospital.wx.controller.form.DoctorScheduleSlotVO;
import com.hospital.wx.dao.DoctorWorkPlanDao;
import com.hospital.wx.dao.DoctorWorkPlanScheduleDao;
import com.hospital.wx.exception.GlobalException;
import com.hospital.wx.pojo.DoctorWorkPlanSchedule;
import com.hospital.wx.service.DoctorWorkPlanScheduleService;
import com.hospital.wx.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DoctorWorkPlanScheduleServiceImpl implements DoctorWorkPlanScheduleService {
    private String WORK_PLAN_SCHEDULE_KEY = Constants.WORK_PLAN_SCHEDULE_KEY;
    private HashMap<String, String> APPOINTMENT_SLOT = new HashMap<>(new HashMap() {{
        put("1","08:00");
        put("2","08:30");
        put("3","09:00");
        put("4","09:30");
        put("5","10:00");
        put("6","10:30");
        put("7","11:00");
        put("8","11:30");
        put("9","13:00");
        put("10","13:30");
        put("11","14:00");
        put("12","14:30");
        put("13","15:00");
        put("14","15:30");
        put("15","16:00");
    }});
    @Autowired
    private DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    @Autowired
    private DoctorWorkPlanDao doctorWorkPlanDao;

    @Autowired
    RedisTemplate redisTemplate;

    @Transactional
    void insertScheduleHandle(ArrayList<DoctorWorkPlanSchedule> list) {
        for (DoctorWorkPlanSchedule entity : list) {
            doctorWorkPlanScheduleDao.insert(entity);
        }
    }

    @Override
    public void insert(ArrayList<DoctorWorkPlanSchedule> list) {
        insertScheduleHandle(list);
        //避免患者端挂号出现超售的现象，所以需要讲添加的出诊计划保存到redis中
        addScheduleCache(list);
    }

    private void addScheduleCache(ArrayList<DoctorWorkPlanSchedule> list) {
        //如果list中没有元素，就不需要创建缓存
        if (list == null || list.size() == 0) {
            return;
        }
        //一个出诊计划中插入到schedule数据表的时间段记录的外键值相同，取出列表第一个元素获得外键
        int workPlanId = list.get(0).getWorkPlanId();
        //查询数据库记录
        ArrayList<HashMap> newList = doctorWorkPlanScheduleDao.selectScheduleByWorkPlanId(workPlanId);
        for (HashMap one : newList) {
            int id = MapUtil.getInt(one, "scheduleId");
            int slot = MapUtil.getInt(one, "slot");
            //定义缓存记录的Key，保存到redis的key
            String key = WORK_PLAN_SCHEDULE_KEY + id;
            //把出诊时间段缓存到Redis
            /*
              保存到redis为如下结构：
                doctor_schedule_100 scheduleId  100
                doctor_schedule_100 slot  1
            * */
            redisTemplate.opsForHash().putAll(key, one);
            //出诊日期, 例如：2024-04-23
            String date = MapUtil.getStr(one, "date");

            /*
            * 获取该时间段对应的起始时间，再根据slot的值获取到对应的时段
            * 例如： slot = 1 就会返回 08:00
            *
            *
            * */
            String time = APPOINTMENT_SLOT.get(String.valueOf(slot));

            /*
            * 设置缓存过期时间 2024-04-23 08:00 表示这条数据在指定的时间会被删除。
            * 如果患者在2024-04-23 08:00挂号的时候就不能挂2024-04-23 08:00这个时间的号了
            * 因为该时间已经开始接诊了。只能在2024-04-23 08:00时间之前该该时段的号。
            * */
            redisTemplate.expireAt(key, DateUtil.parse(date + " " + time));
        }
    }
        @Override
    public ArrayList selectScheduleByDeptSub(Map param) {
        ArrayList<HashMap> list = doctorWorkPlanScheduleDao.selectScheduleByDeptSub(param);
        ArrayList<HashMap> result = new ArrayList();
        int tempDoctorId = 0;
        HashMap doctor = new HashMap();

        for (HashMap map : list) {
            int doctorId = MapUtil.getInt(map, "doctorId");
            int slot = MapUtil.getInt(map, "slot");
            //如果当前记录跟上一条记录的医生不是同一个人
            if (tempDoctorId != doctorId) {
                tempDoctorId = doctorId;
                doctor = map;
                doctor.replace("slot", new ArrayList<Integer>() {{
                    add(slot);
                }});
                result.add(doctor);
            }
            //如果当前记录与上一条记录是同一个医生
            else if (tempDoctorId == doctorId) {
                ArrayList<Integer> slotList = (ArrayList) doctor.get("slot");
                slotList.add(slot);
            }
        }
        //筛选哪些时段出诊，哪些时段不出诊
        for (HashMap map : result) {
            ArrayList<Integer> slot = (ArrayList) map.get("slot");
            ArrayList tempSlot = new ArrayList();
            for (int i = 1; i <= 15; i++) {
                tempSlot.add(slot.contains(i));
            }
            map.replace("slot", tempSlot);
        }
        return result;
    }

    @Override
    public HashMap selectScheduleByWorkPlanId(int workPlanId) {
        ArrayList<HashMap> list = doctorWorkPlanScheduleDao.selectScheduleByWorkPlanId(workPlanId);
        HashMap result = new HashMap();
        ArrayList temp = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            HashMap map = list.get(i);
            int scheduleId = MapUtil.getInt(map, "scheduleId");
            int doctorId = MapUtil.getInt(map, "doctorId");
            int maximum = MapUtil.getInt(map, "maximum");
            int num = MapUtil.getInt(map, "num");
            int slot = MapUtil.getInt(map, "slot");
            if (i == 0) {
                result.put("doctorId", doctorId);
                result.put("maximum", maximum);
            }
            temp.add(new HashMap<>() {{
                put("scheduleId", scheduleId);
                put("slot", slot);
                put("num", num);
            }});
        }
        result.put("slots", temp);
        return result;
    }

    @Override
    public void updateSchedule(Map param) {
        //删除或者添加出诊时段
        ArrayList<DoctorWorkPlanSchedule> addList = this.insertOrDeleteScheduleHandle(param);
        //把新添加的出诊时段缓存到Redis
        addScheduleCache(addList);
    }


    @Transactional
    ArrayList<DoctorWorkPlanSchedule> insertOrDeleteScheduleHandle(Map param) {
        //更新接诊人数上限
        doctorWorkPlanDao.updateMaximum(param);

        Integer workPlanId = MapUtil.getInt(param, "workPlanId");

        //待处理的时间段列表（添加或者删除）
        ArrayList<DoctorScheduleSlotVO> slots = (ArrayList<DoctorScheduleSlotVO>) param.get("slots");
        //用于存放添加时间段的列表
        ArrayList<DoctorWorkPlanSchedule> addList = new ArrayList();
        //用于存放删除时间段的列表
        ArrayList<Integer> removeList = new ArrayList();
        //分析哪些是添加时间段，哪些是删除时间段
        slots.forEach(one -> {
            //判断添加记录
            if (one.getOperate().equals("insert")) {
                DoctorWorkPlanSchedule entity = new DoctorWorkPlanSchedule();
                entity.setWorkPlanId(workPlanId);
                entity.setMaximum(one.getMaximum());
                entity.setSlot(one.getSlot());
                addList.add(entity); //时间段保存到添加列表
            } else {
                removeList.add(one.getScheduleId()); //时间段保存到删除列表
            }
        });
        //判断删除列表是否有元素
        if (removeList.size() > 0) {
            //查询这些待删除时间段的总挂号人数
            long sum = doctorWorkPlanScheduleDao.selectSumNumByIds(removeList);
            //如果这些待删除的时间段总挂号人数大于0，说明这些时间段中有的存在患者挂号，所以不能删
            if (sum > 0) {
                throw new GlobalException("修改失败，本次修改与已经挂号记录冲突");
            }
            //如果没有患者挂号，就删除这些时间段记录
            doctorWorkPlanScheduleDao.deleteByIds(removeList);
            //删除Redis上面的时间段缓存s
            removeList.forEach(one -> {
                String key = WORK_PLAN_SCHEDULE_KEY + one;
                redisTemplate.delete(key);
            });
        }
        //处理添加时间段列表
        addList.forEach(one -> {
            //添加时间段记录
            doctorWorkPlanScheduleDao.insert(one);
        });
        return addList;
    }


    @Override
    @Transactional
    public void deleteByWorkPlanId(int workPlanId) {
        ArrayList<HashMap> list = doctorWorkPlanScheduleDao.selectScheduleByWorkPlanId(workPlanId);
        //删除时段记录信息
        doctorWorkPlanScheduleDao.deleteByWorkPlanId(workPlanId);
        //删除redis缓存中的数据
        list.forEach(one -> {
            int scheduleId = MapUtil.getInt(one, "scheduleId");
            String key = WORK_PLAN_SCHEDULE_KEY + scheduleId;
            redisTemplate.delete(key);
        });
    }

    @Override
    public ArrayList<HashMap> selectDoctorWorkPlanSchedule(Map param) {
        return doctorWorkPlanScheduleDao.selectDoctorWorkPlanSchedule(param);
    }
}