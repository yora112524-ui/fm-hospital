package com.hospital.hms.exception;

public enum HMSErrorCode {

    /**
     * 错误类型一：
     * */
    DELETE_DOCTOR_FAILED(1001, "删除医生信息失败"),
    INVALID_DOCTOR_ID(1002, "无效的医生ID"),
    INVALID_CLINIC_ID(1003, "根据诊室ID查询医生信息失败"),
    PLAN_SAVE_FAILED(1004, "添加出诊计划时出错"),
    DUPLICATE_PLAN_SAVE(1005, "该医生已存在出诊计划"),
    PLAN_QUERY_FAILED(1006, "出诊计划查询出错"),
    PLAN_UPDATE_FAILED(1007, "更新医生出诊计划时出错"),
    PLAN_DELETE_FAILED(1008, "删除医生出诊计划时出错"),
    PLAN_NOT_FOUND(1009, "通过WorkPlanId查询医生出诊排班时出错"),

    /**
     * 错误类型二：
     * */
    PLAN_EXISTS("已经存在出诊计划，不能重复添加"),
    PLAN_SAVE_OK("添加完成"),
    DAILY_REGISTRATION_LIMIT_REACHED("已经达到当天挂号上限"),
    ROOM_ALREADY_REGISTERED("已经挂过该诊室的号"),
    FACE_MODEL_NOT_FOUND("不存在面部模型"),
    NO_FACE_VERIFICATION_RECORD("当日没有人脸验证记录"),
    REGISTRATION_ELIGIBLE("满足挂号条件"),
    FACE_VERIFICATION_FAILED("人脸验证失败"),
    REGISTRATION_EXISTS("该出诊计划已经有患者挂号，禁止删除");
    ;

    private final Integer code;
    private final String message;

    // 构造函数
    HMSErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    HMSErrorCode(String message) {
        this.code = null;  // 对于没有 code 的错误码，code 设置为 null
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
