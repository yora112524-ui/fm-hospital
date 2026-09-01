package com.hospital.hms.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import com.hospital.hms.controller.form.InsertWorkPlanForm;
import com.hospital.hms.controller.form.SelectDoctorWorkPlanInTime;
import com.hospital.hms.exception.HMSErrorCode;
import com.hospital.hms.service.DoctorWorkPlanService;
import com.hospital.hms.utils.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/doctor/work/plan")
@Tag(name = "DoctorWorkPlanController", description = "门诊管理接口")
@Slf4j
public class DoctorWorkPlanController {
    @Autowired
    private DoctorWorkPlanService doctorWorkPlanService;

    @Operation(summary = "添加医生出诊计划")
    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT", "SCHEDULE:INSERT"}, mode = SaMode.OR)
    public CommonResult insert(@RequestBody @Valid InsertWorkPlanForm form) {
        try {
            Map param = BeanUtil.beanToMap(form);
            String msg = doctorWorkPlanService.insert(param);
            if (msg.equals(HMSErrorCode.DUPLICATE_PLAN_SAVE.getMessage().toString())) {
                return CommonResult.failed(HMSErrorCode.DUPLICATE_PLAN_SAVE);
            }
            return CommonResult.ok().put(CommonResult.RETURN_RESULT,HMSErrorCode.PLAN_SAVE_OK.getMessage());
        } catch (Exception e) {
            // 记录异常日志
            log.error("添加医生出诊计划时出错，form: {}", form, e);
            // 返回统一的失败信息
            return CommonResult.failed(HMSErrorCode.PLAN_SAVE_FAILED);
        }
    }

    @Operation(summary = "出诊计划查询")
    @PostMapping("/selectWorkPlanByTime")
    @SaCheckPermission(value = {"ROOT", "SCHEDULE:SELECT"}, mode = SaMode.OR)
    public CommonResult selectWorkPlanByTime(@RequestBody @Valid SelectDoctorWorkPlanInTime form) {
        try {
            Map<String, Object> param = BeanUtil.beanToMap(form);

            /**
             *  生成连续的日期
             *  dateList = {ArrayList@18405}  size = 8
             *  0 = "2025-02-13"
             *  1 = "2025-02-14"
             *  2 = "2025-02-15"
             *  3 = "2025-02-16"
             *  4 = "2025-02-17"
             *  5 = "2025-02-18"
             *  6 = "2025-02-19"
             *  7 = "2025-02-20"
             * */
            List<String> dateList = generateDateList(form.getStartDate(), form.getEndDate(), false);

            // 获取出诊计划数据
            JSONArray workPlanData = doctorWorkPlanService.selectWorkPlanByTime(param, dateList);

            /**
             *  生成格式化日期（用于前端表头显示）
             *  formattedDateList = {ArrayList@18468}  size = 8
             *  0 = "02月13日 (星期四) "
             *  1 = "02月14日 (星期五) "
             *  2 = "02月15日 (星期六) "
             *  3 = "02月16日 (星期日) "
             *  4 = "02月17日 (星期一) "
             *  5 = "02月18日 (星期二) "
             *  6 = "02月19日 (星期三) "
             *  7 = "02月20日 (星期四) "
             *
             * */
            List<String> formattedDateList = generateDateList(form.getStartDate(), form.getEndDate(), true);

            return CommonResult.ok()
                    .put(CommonResult.RETURN_RESULT, workPlanData)
                    .put("dateList", formattedDateList);
        } catch (Exception e) {
            log.error("查询医生出诊计划时出错，form: {}", form, e);
            return CommonResult.failed(HMSErrorCode.PLAN_QUERY_FAILED);
        }
    }

    /**
     * 根据开始和结束日期生成日期列表。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param isFormatted 是否格式化日期（true：格式化日期；false：原始日期字符串）
     * @return 日期列表
     */
    private List<String> generateDateList(String startDate, String endDate, boolean isFormatted) {
        DateRange range = DateUtil.range(new DateTime(startDate), new DateTime(endDate), DateField.DAY_OF_MONTH);
        List<String> dateList = new ArrayList<>();
        range.forEach(date -> {
            if (isFormatted) {
                // 格式化日期，如：03月19日（星期一）
                dateList.add(date.toString("MM月dd日") + " (" + date.dayOfWeekEnum().toChinese() + ") ");
            } else {
                // 原始日期字符串
                dateList.add(date.toDateStr());
            }
        });
        return dateList;
    }

}
