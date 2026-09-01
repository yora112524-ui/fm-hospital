# FM Hospital - 智慧医院预约挂号与远程问诊平台

基于 Spring Boot 的智慧医院后端项目，采用 Maven 多模块架构拆分公共组件、医院管理端 API 与患者微信端 API，覆盖科室医生管理、医生排班、患者档案、预约挂号、人脸核验及视频问诊等业务。

## 技术栈

- Java 15、Spring Boot 2.5.2、Spring MVC、Jetty
- MyBatis、MySQL、Druid
- Redis、Sa-Token
- WebSocket、Quartz、RabbitMQ
- MinIO、腾讯云 IAI / TRTC
- Maven、SpringDoc、Validation

## 项目模块

```text
fm_hospital
├── common             公共响应、分页、异常及工具类
├── hospital_hms_api   医院管理端 API（默认端口 9091）
└── hospital_wx_api    患者微信端 API（默认端口 9092）
```

## 核心功能

- 科室、门诊、医生及出诊计划管理
- 患者微信登录、患者档案与就诊卡管理
- 基于 Redis 乐观锁的并发号源预占
- 基于 Redis TTL 的未支付订单超时关闭与号源释放
- 基于 Sa-Token + Redis 的 RBAC 权限控制
- 腾讯云人脸建模、身份比对与活体检测
- MinIO 医生头像及问诊图片存储
- Redis Hash + WebSocket 视频问诊状态通知

## 并发挂号流程

```text
查询排班缓存
  -> WATCH 监听号源 Key
  -> 校验 maximum / num
  -> MULTI 开启事务
  -> 原子增加预约人数
  -> EXEC 提交
  -> 创建挂号记录
  -> 支付成功确认 / 超时释放号源
```

## 本地运行

1. 创建 MySQL 数据库 `fm_hospital`，导入：

   `hospital_hms_api/src/main/resources/sql/fm_hospital.sql`

2. 启动 MySQL、Redis 和 MinIO。

3. 根据需要配置环境变量：

```text
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET
WECHAT_APP_ID, WECHAT_APP_SECRET
TENCENT_CLOUD_APP_ID, TENCENT_CLOUD_SECRET_ID, TENCENT_CLOUD_SECRET_KEY
TENCENT_FACE_GROUP, TENCENT_FACE_REGION
TENCENT_TRTC_APP_ID, TENCENT_TRTC_SECRET_KEY
```

4. 在项目根目录构建：

```bash
mvn clean package -DskipTests
```

5. 分别启动 `HmsApiApplication` 和 `WxApplication`。

## 说明

- 微信支付相关代码在当前学习环境中使用 Mock 结果，生产接入需要补充商户配置、回调验签、幂等与退款流程。
- 配置文件使用环境变量读取敏感信息，证书和本地开发配置不会提交到仓库。
