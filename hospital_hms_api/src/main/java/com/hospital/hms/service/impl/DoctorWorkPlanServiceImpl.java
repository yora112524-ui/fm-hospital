package com.hospital.hms.service.impl;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hospital.hms.common.OperationMessage;
import com.hospital.hms.dao.DoctorWorkPlanDao;
import com.hospital.hms.dao.PatientRegistrationDao;
import com.hospital.hms.dao.PatientUserInfoDao;
import com.hospital.hms.exception.GlobalException;
import com.hospital.hms.exception.HMSErrorCode;
import com.hospital.hms.pojo.DoctorWorkPlan;
import com.hospital.hms.pojo.DoctorWorkPlanSchedule;
import com.hospital.hms.service.DoctorWorkPlanScheduleService;
import com.hospital.hms.service.DoctorWorkPlanService;
import com.hospital.hms.service.PatientFaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DoctorWorkPlanServiceImpl implements DoctorWorkPlanService {

    @Autowired
    private DoctorWorkPlanDao doctorWorkPlanDao;

    @Autowired
    private DoctorWorkPlanScheduleService doctorWorkPlanScheduleService;

    @Autowired
    private PatientRegistrationDao patientRegistrationDao;

    @Autowired
    PatientUserInfoDao patientUserInfoDao;

    @Autowired
    PatientFaceService patientFaceService;

    /**
     * param参数如下：
     * "startDate" -> "2025-02-13"
     * "endDate" -> "2025-02-20"
     * "deptId" -> xxx
     * "doctorName" -> xxx
     *
     * */
    @Override
    public JSONArray selectWorkPlanByTime(Map param, List dateList) {
        /**
         *
         * 选择日期：2025-02-13
         * list 数据如下：
         [
             {"date": "2025-02-13", "deptName": "口腔科", "workPlanId": 119, "deptSubId": 1, "doctorName": "韩倩倩", "deptSubName": "口腔颌面外科"},
             {"date": "2025-02-13", "deptName": "口腔科", "workPlanId": 126, "deptSubId": 1, "doctorName": "赵六", "deptSubName": "口腔颌面外科"},
             {"date": "2025-02-14", "deptName": "口腔科", "workPlanId": 127, "deptSubId": 1, "doctorName": "赵六", "deptSubName": "口腔颌面外科"},
             {"date": "2025-02-14", "deptName": "口腔科", "workPlanId": 121, "deptSubId": 1, "doctorName": "韩倩倩", "deptSubName": "口腔颌面外科"},
             {"date": "2025-02-15", "deptName": "口腔科", "workPlanId": 122, "deptSubId": 1, "doctorName": "韩倩倩", "deptSubName": "口腔颌面外科"},
             {"date": "2025-02-13", "deptName": "口腔科", "workPlanId": 124, "deptSubId": 2, "doctorName": "李雨萌", "deptSubName": "口腔颌面内科"},
             {"date": "2025-02-13", "deptName": "肿瘤科", "workPlanId": 125, "deptSubId": 26, "doctorName": "周嘉欣", "deptSubName": "肿瘤科门诊"}
         ]
         *
         * */
        ArrayList<HashMap> list = doctorWorkPlanDao.selectWorkPlanByTime(param);
//        String jsonStr = JSONUtil.toJsonStr(list);
//        System.out.println(jsonStr);
        // 创建一个Map，以deptSubId为key，存储每个科室的出诊计划
        Map<Integer, HashMap> tempResult = new HashMap<>();
        for (HashMap one : list) {
            // 从查询结果中获取相关字段
            String deptName = MapUtil.getStr(one, "deptName");
            int deptSubId = MapUtil.getInt(one, "deptSubId");
            String deptSubName = MapUtil.getStr(one, "deptSubName");
            String doctorName = MapUtil.getStr(one, "doctorName");
            String date = MapUtil.getStr(one, "date");

            /**
             * 如果当前deptSubId的记录不存在，则初始化
             * 使用LinkedHashMap是为了保证数据的有序（按照顺序存储）
             * */
            if (!tempResult.containsKey(deptSubId)) {
                HashMap temp = new HashMap() {{
                    put("deptName", deptName); // 存储科室名称
                    put("deptSubId", deptSubId); // 存储子科室ID
                    put("deptSubName", deptSubName); // 存储子科室名称
                    put("plan", new LinkedHashMap<>() {{ // 存储日期对应的医生排班
                        put(date, new ArrayList<>() {{ // 初始化当前日期的医生列表
                            add(doctorName);
                        }});
                    }});
                }};
                tempResult.put(deptSubId, temp); // 将当前科室的计划存入tempResult
            } else {
                // 若已存在该deptSubId，则获取对应的计划
                HashMap map = tempResult.get(deptSubId);

                //结构如下： "2025-02-13": ["韩倩倩", "赵六", "李雨萌"]
                LinkedHashMap plan = (LinkedHashMap) map.get("plan");

                // 若当前日期不存在，则初始化一个医生列表
                if (!plan.containsKey(date)) {
                    plan.put(date, new ArrayList<>());
                }

                // 获取当前日期的医生列表，并添加新的医生
                ArrayList doctors = (ArrayList) plan.get(date);
                doctors.add(doctorName);
            }
        }

        /**
         * tempResult = >
         * {
         *   "1": {
         *     "deptName": "口腔科",
         *     "deptSubId": 1,
         *     "deptSubName": "口腔颌面外科",
         *     "plan": {
         *       "2025-02-13": ["韩倩倩", "赵六", "李雨萌"],
         *       "2025-02-14": ["赵六", "韩倩倩"],
         *       "2025-02-15": ["韩倩倩"]
         *     }
         *   },
         *   "2": {
         *     "deptName": "口腔科",
         *     "deptSubId": 2,
         *     "deptSubName": "口腔颌面内科",
         *     "plan": {
         *       "2025-02-13": ["李雨萌"]
         *     }
         *   },
         *   "26": {
         *     "deptName": "肿瘤科",
         *     "deptSubId": 26,
         *     "deptSubName": "肿瘤科门诊",
         *     "plan": {
         *       "2025-02-13": ["周嘉欣"]
         *     }
         *   }
         * }
         *
         *
         * */

        /**
         * 确保dateList中的所有日期都存在于每个deptSubId的计划中
         * {
         *   "1": {
         *     "deptName": "口腔科",
         *     "deptSubId": 1,
         *     "deptSubName": "口腔颌面外科",
         *     "plan": {
         *       "2025-02-13": ["韩倩倩", "赵六", "李雨萌"],
         *       "2025-02-14": ["赵六", "韩倩倩"],
         *       "2025-02-15": ["韩倩倩"],
         *       "2025-02-16": [],
         *       "2025-02-17": [],
         *       "2025-02-18": [],
         *       "2025-02-19": [],
         *       "2025-02-20": []
         *     }
         *   },
         *   "2": {
         *     "deptName": "口腔科",
         *     "deptSubId": 2,
         *     "deptSubName": "口腔颌面内科",
         *     "plan": {
         *       "2025-02-13": ["李雨萌"],
         *       "2025-02-14": [],
         *       "2025-02-15": [],
         *       "2025-02-16": [],
         *       "2025-02-17": [],
         *       "2025-02-18": [],
         *       "2025-02-19": [],
         *       "2025-02-20": []
         *     }
         *   },
         *   "26": {
         *     "deptName": "肿瘤科",
         *     "deptSubId": 26,
         *     "deptSubName": "肿瘤科门诊",
         *     "plan": {
         *       "2025-02-13": ["周嘉欣"],
         *       "2025-02-14": [],
         *       "2025-02-15": [],
         *       "2025-02-16": [],
         *       "2025-02-17": [],
         *       "2025-02-18": [],
         *       "2025-02-19": [],
         *       "2025-02-20": []
         *     }
         *   }
         * }
         *
         * */

        tempResult.values().forEach(map -> {
            /**
            *     "plan": {
            *       "2025-02-13": ["韩倩倩", "赵六", "李雨萌"],
            *       "2025-02-14": ["赵六", "韩倩倩"],
            *       "2025-02-15": ["韩倩倩"]
            *     }
             *
             * */
            LinkedHashMap plan = (LinkedHashMap) map.get("plan");
            /**
             * "plan": {
             *          *       "2025-02-13": ["韩倩倩", "赵六", "李雨萌"],
             *          *       "2025-02-14": ["赵六", "韩倩倩"],
             *          *       "2025-02-15": ["韩倩倩"],
             *          *       "2025-02-16": [],
             *          *       "2025-02-17": [],
             *          *       "2025-02-18": [],
             *          *       "2025-02-19": [],
             *          *       "2025-02-20": []
             *          *     }
             * */
            dateList.forEach(date -> plan.putIfAbsent(date, new ArrayList<>()));
        });


        /**
         * 按日期排序等处理完后的结构如下：
         * {
         *   "1": {
         *     "deptName": "口腔科",
         *     "deptSubId": 1,
         *     "deptSubName": "口腔颌面外科",
         *     "plan": [
         *       {
         *         "date": "2025-02-13",
         *         "doctors": ["韩倩倩", "赵六", "李雨萌"]
         *       },
         *       {
         *         "date": "2025-02-14",
         *         "doctors": ["赵六", "韩倩倩"]
         *       },
         *       {
         *         "date": "2025-02-15",
         *         "doctors": ["韩倩倩"]
         *       },
         *       {
         *         "date": "2025-02-16",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-17",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-18",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-19",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-20",
         *         "doctors": []
         *       }
         *     ]
         *   },
         *   "2": {
         *     "deptName": "口腔科",
         *     "deptSubId": 2,
         *     "deptSubName": "口腔颌面内科",
         *     "plan": [
         *       {
         *         "date": "2025-02-13",
         *         "doctors": ["李雨萌"]
         *       },
         *       {
         *         "date": "2025-02-14",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-15",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-16",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-17",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-18",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-19",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-20",
         *         "doctors": []
         *       }
         *     ]
         *   },
         *   "26": {
         *     "deptName": "肿瘤科",
         *     "deptSubId": 26,
         *     "deptSubName": "肿瘤科门诊",
         *     "plan": [
         *       {
         *         "date": "2025-02-13",
         *         "doctors": ["周嘉欣"]
         *       },
         *       {
         *         "date": "2025-02-14",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-15",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-16",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-17",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-18",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-19",
         *         "doctors": []
         *       },
         *       {
         *         "date": "2025-02-20",
         *         "doctors": []
         *       }
         *     ]
         *   }
         * }
         * */
        tempResult.values().forEach(map -> {
            /**
             * // 使用TreeMap按日期升序排序
             * TreeMap<String, ArrayList> sortedPlan = new TreeMap<>((d1, d2) -> new DateTime(d1).isAfter(new DateTime(d2)) ? 1 : -1);
             * 这里创建了一个 TreeMap，目的是按日期对排班进行升序排序。
             *
             * TreeMap 是一个自排序的 Map，它根据键（这里是日期字符串）进行排序。默认情况下，TreeMap 会按字典顺序排序，但我们传入了一个自定义比较器，来控制日期的排序方式。
             * 传入的比较器逻辑是：
             * new DateTime(d1).isAfter(new DateTime(d2)) ? 1 : -1：如果 d1 的日期晚于 d2，就返回 1（即 d1 在 d2 后面）；否则返回 -1（即 d1 在 d2 前面）。这个比较器确保了日期按升序排列。
             * */
            TreeMap<String, ArrayList> sortedPlan = new TreeMap<>((d1, d2) -> new DateTime(d1).isAfter(new DateTime(d2)) ? 1 : -1);
            /**
             * 排序前的结果：
             * "plan": {
             *          *       "2025-02-17": [],
             *          *       "2025-02-16": [],
             *          *     }
             * */
            LinkedHashMap plan = (LinkedHashMap) map.get("plan");
            /**
             * 排序后的结果：
             * "plan": {
             *          *       "2025-02-16": [],
             *          *       "2025-02-17": [],
             *          *     }
             * */
            sortedPlan.putAll(plan);

            /**
             * 假设我们有一个科室的排班计划，plan 中的数据可能如下所示（无序）：
             *  {
             *     "2025-02-14": ["赵六", "韩倩倩"],
             *     "2025-02-13": ["韩倩倩", "赵六"]
             * }
             *
             * 经过排序后，sortedPlan 会变成：
             * {
             *     "2025-02-13": ["韩倩倩", "赵六"],
             *     "2025-02-14": ["赵六", "韩倩倩"]
             * }
             *
             * 最终，map.get("plan") 会变成：
             * [
             *     {"date": "2025-02-13", "doctors": ["韩倩倩", "赵六"]},
             *     {"date": "2025-02-14", "doctors": ["赵六", "韩倩倩"]}
             * ]
             *
             * */
            // 替换原有的计划结构，转换为List<Map>格式
            map.replace("plan", new ArrayList<>());
            sortedPlan.forEach((date, doctors) -> {
                ((ArrayList) map.get("plan")).add(new HashMap<String, Object>() {{
                    put("date", date); // 存储日期
                    put("doctors", doctors); // 存储该日期的医生列表
                }});
            });
        });

        // 将最终结果转换为JSONArray并返回，只需要返回值就可以了
        return JSONUtil.parseArray(tempResult.values());
    }

    /**
     * 保存出诊计划的具体逻辑。
     * */
    @Transactional
    @Override
    public String insert(Map param) {
        try {
            // 查询选择的医生当天是否存在出诊计划
            Integer id = doctorWorkPlanDao.checkDoctorWorkPlanForToday(param);
            if (id != null) {
                log.info("医生已经有今天的出诊计划，医生ID: {}", param.get("doctorId"));
                return HMSErrorCode.DUPLICATE_PLAN_SAVE.getMessage().toString();
            }
            //绑定提交的参数到DoctorWorkPlan实例
            DoctorWorkPlan doctorWorkPlan = BeanUtil.toBean(param, DoctorWorkPlan.class);
            // 设置该医生当天挂号人数上线
            doctorWorkPlan.setMaximum(Integer.parseInt(param.get("totalMaximum").toString()));
            // 保存出诊计划
            doctorWorkPlanDao.insert(doctorWorkPlan);
            log.info("医生的出诊计划插入成功，出诊计划ID: {}", doctorWorkPlan.getId());
            // 获取医生出诊的时间段
            Integer[] slots = (Integer[]) param.get("slots");
            int slotMaximum = Integer.parseInt(param.get("slotMaximum").toString());
            ArrayList<DoctorWorkPlanSchedule> scheduleList = new ArrayList<>();
            //根据时间段循环添加DoctorWorkPlanSchedule记录
            for (Integer slot : slots) {
                /**
                 * 循环时间段生成医生出诊表记录
                 * */
                DoctorWorkPlanSchedule doctorWorkPlanSchedule = BeanUtil.toBean(param, DoctorWorkPlanSchedule.class);
                //设置门诊日程表的ID
                doctorWorkPlanSchedule.setWorkPlanId(doctorWorkPlan.getId());
                //设置具体的时段
                doctorWorkPlanSchedule.setSlot(slot);
                //设置时段人数
                doctorWorkPlanSchedule.setMaximum(slotMaximum);
                scheduleList.add(doctorWorkPlanSchedule);
            }
            // 保存出诊计划详细信息
            doctorWorkPlanScheduleService.insert(scheduleList);
        } catch (Exception e) {
            log.error("插入医生出诊计划或出诊时间段时发生错误", e);
            throw new GlobalException("插入医生出诊计划或出诊时间段失败", e);
        }
        return "";
    }


    /*
    * 移动端接口
    * */
    @Override
    public ArrayList<String> selectCanRegisterInDateRange(Map param) {
        /*
            假设结果如下：
                2024-05-07
                2024-05-08
        * */
        ArrayList<String> list = doctorWorkPlanDao.selectCanRegisterInDateRange(param);
        //解析出startDate和endDate
        DateTime startDate = DateUtil.parse(MapUtil.getStr(param, "startDate"));
        DateTime endDate = DateUtil.parse(MapUtil.getStr(param, "endDate"));
        /**
         *
         * 基于startDate-endDate生成日期范围
         * 例如：  2024-05-07 - 2024-05-13
         * */
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        //创建结果集
        ArrayList result = new ArrayList();
        /**
         * 遍历range生成最终结果数据。
         * */
        while (range.hasNext()) {
            //例如：2024-05-07
            String date = range.next().toDateStr();
            if (list.contains(date)) {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "出诊");
                }});
            } else {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "无号");
                }});
            }
        }
        return result;
    }

    @Override
    public ArrayList<HashMap> selectDeptSubDoctorPlanInDay(Map param) {
        return doctorWorkPlanDao.selectDeptSubDoctorPlanInDay(param);
    }

    @Override
    public String checkRegisterCondition(Map param) {
        //条件1. 检查当天用户是否已经挂号3次以上
        //获取当前系统时间
        param.put("today", DateUtil.today());
        long count = patientRegistrationDao.selectRegistrationCountInToday(param);
        if (count == 3) {
            return OperationMessage.DAILY_REGISTRATION_LIMIT_REACHED.toString();
        }

        //条件2. 检查当天是否已经挂过该门诊的号
        Integer id = patientRegistrationDao.existsRegisterRecordInDay(param);
        if (id != null) {
            return OperationMessage.ROOM_ALREADY_REGISTERED.toString();
        }

        //条件3. 检查是否存在人脸面部数据（是否采集过人脸数据）
        /**
         * 如果没有满足条件3: 前端会跳转到人脸信息采集页面让患者进行人脸录入.
         * */
        int userId = MapUtil.getInt(param, "userId");
        Boolean bool = patientUserInfoDao.existsFaceModel(userId);
        if (bool == null || !bool) {
            return OperationMessage.FACE_MODEL_NOT_FOUND.toString();
        }

        //条件4.检查今日是否存在挂号用户的面部识别记录
        /**
         * 每次挂号的时候当天需要进行人脸验证才可以.
         * 有的是要求之前做过人脸验证就行了
         *
         * 如果当天用户做过人脸识别在会patient_face_auth里面保存一条记录
         * 如果没有满足条件4: 前端会跳转到人脸验证的页面让患者进行人脸验证.
         * */
//        bool = patientFaceService.hasFaceAuthInDay(param);
//        if (!bool) {
//            return OperationMessage.NO_FACE_VERIFICATION_RECORD.toString();
//        }

        return OperationMessage.REGISTRATION_ELIGIBLE.toString();
    }
}