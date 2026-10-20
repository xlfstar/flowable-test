# Flowable 工作流示例 (Flowable 7 + Spring Boot 3 + MySQL + Vue3)

一个前后端分离的工作流示例项目，包含完整的**登录鉴权、组织架构、流程定义/部署、BPMN 流程设计、待办/审批、请假业务**。

- 后端：Java 17 + Spring Boot 3.2 + Flowable 7 + MyBatis-Plus + MySQL + JWT
- 前端：Vite + Vue3 + TypeScript + Element Plus + bpmn-js（流程设计器 / 流程图查看）
- 示例流程：**请假审批流程**（申请人所在部门领导审批；超过 3 天增加上级领导审批）

## 目录结构

```
flowable-test/
├── backend/                       # 后端 (Spring Boot 3 + Flowable 7)
│   ├── mvnw / mvnw.cmd            # Maven Wrapper, 无需本机安装 Maven
│   ├── pom.xml
│   ├── sql/
│   │   ├── init.sql               # 全新数据库初始化脚本
│   │   └── upgrade-org.sql        # 既有数据库升级脚本(组织架构)
│   └── src/main/
│       ├── java/com/example/flowable/
│       │   ├── common/            # 统一返回、异常处理
│       │   ├── config/            # 安全 / CORS / MyBatis-Plus / 数据初始化
│       │   ├── security/          # JWT + Spring Security
│       │   ├── entity/ mapper/    # 业务表实体与 Mapper
│       │   ├── service/           # Flowable 封装、组织架构、请假业务
│       │   ├── listener/          # 审批节点任务监听器
│       │   └── controller/        # REST 接口
│       └── resources/
│           ├── application.yml
│           └── processes/leave.bpmn20.xml   # 启动时自动部署
└── frontend/                      # 前端 (Vue3 + Element Plus + bpmn-js)
    ├── package.json
    └── src/
        ├── api/                   # axios 封装
        ├── views/                 # 页面
        │   ├── login/  dashboard/
        │   ├── org/               # 组织架构
        │   ├── process/           # definition / start / designer / instance
        │   ├── task/              # todo / done
        │   ├── form/              # list / designer (自定义表单)
        │   └── leave/             # apply / list
        ├── components/            # ProcessViewer.vue(流程查看) / DynamicForm.vue(动态表单渲染)
        └── bpmn/                  # 流程设计器 moddle 扩展与示例
```

## 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17+ |
| Maven | 无需安装（使用 `mvnw` 包装器） |
| MySQL | 8.0+（与 SQL Server 二选一） |
| SQL Server | 2019+（与 MySQL 二选一） |
| Node.js | 18+（推荐 20+） |

## 启动步骤

### 1. 初始化数据库

项目使用 **Flyway 自动创建业务表**，Flowable 引擎表由 Flowable 在启动时自动创建。

**MySQL 全新数据库**：只需要创建空数据库：

```sql
CREATE DATABASE flowable_demo DEFAULT CHARACTER SET utf8mb4;
```

**SQL Server 全新数据库**：只需要创建空数据库：

```sql
CREATE DATABASE flowable_demo COLLATE Chinese_PRC_CI_AS;
```

Flyway 会根据连接类型自动选择：

```text
backend/src/main/resources/db/migration/mysql/V1__init_business_tables.sql
backend/src/main/resources/db/migration/sqlserver/V1__init_business_tables.sql
```

正常情况下不需要手动执行建表脚本。也可以手动执行 `backend/sql/init.sql` 或对应的 Flyway SQL 文件。

**已有数据库升级**（旧版本项目升级到当前组织架构/文件表）：

```bash
mysql -uroot -p flowable_demo < backend/sql/upgrade-org.sql
mysql -uroot -p flowable_demo < backend/sql/upgrade-file.sql
```

升级脚本可重复执行。已有数据库首次启动时，Flyway 会以版本 1 建立基线，不会重复执行 V1 建表脚本。

### 2. 配置数据库连接

`application.yml` 不写死数据库驱动，Spring Boot 会根据 JDBC URL 自动选择 MySQL 或 SQL Server 驱动。

默认连接 MySQL：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/flowable_demo?...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
```

切换到 SQL Server 不需要改 Java 代码，只改连接信息：

```powershell
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=flowable_demo;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='YourStrong!Passw0rd'
```

或者使用内置的 SQL Server Profile：

```powershell
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=sqlserver
```

`application-sqlserver.yml` 里的默认密码只是示例，使用前请修改。

### 3. 启动后端

```bash
cd backend
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```

首次运行会自动下载 Maven 与依赖（约几分钟）。启动成功后：

- 后端地址：http://localhost:8080
- 接口文档（Swagger UI）：http://localhost:8080/swagger-ui.html

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 。前端通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`。

## 数据库自动识别（MySQL / SQL Server）

项目已经做成“连接哪种数据库就用哪种”：

- `pom.xml` 同时包含 `mysql-connector-j` 和 `mssql-jdbc`
- `application.yml` 不配置 `driver-class-name`，Spring Boot 根据 `spring.datasource.url` 自动选择驱动
- MyBatis-Plus 分页使用 `new PaginationInnerInterceptor()`，自动识别 MySQL / SQL Server 方言
- Flowable 根据 JDBC URL 自动选择数据库方言，`flowable.database-schema-update: true` 自动创建 `ACT_*` / `FLW_*` 表
- Flyway 根据 `{vendor}` 自动执行 `db/migration/mysql` 或 `db/migration/sqlserver` 下的业务表脚本

切换数据库时只需要改连接配置：

```powershell
# MySQL
$env:DB_URL='jdbc:mysql://localhost:3306/flowable_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='root'

# SQL Server
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=flowable_demo;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='YourStrong!Passw0rd'
```

SQL Server 注意事项：

- 推荐使用 `sa`，或确保数据库用户的默认 schema 是 `dbo`
- 用户需要 `db_owner` 权限，Flowable 才能在第一次启动时自动建引擎表
- 本地开发可以使用 `trustServerCertificate=true`，生产环境请配置可信证书
- 命名实例示例：

```text
jdbc:sqlserver://localhost\SQLEXPRESS;databaseName=flowable_demo;encrypt=true;trustServerCertificate=true
```

已有数据库第一次接入 Flyway 时会自动建立基线版本 1，不会重复执行 V1 建表脚本。

## 组织架构模型

| 表 | 说明 |
| --- | --- |
| `sys_department` | 部门表。`parent_id` 组成部门树，`leader_id` 指向部门负责人 |
| `sys_position` | 岗位表。`position_level` 表示岗位层级，数字越小级别越高 |
| `sys_user` | 用户表。新增 `dept_id`、`position_id` 关联部门和岗位 |
| `leave_request` | 请假表。新增申请人部门/岗位、部门领导、上级领导快照字段 |

### 审批人解析规则

`OrganizationService` 按以下规则动态解析审批人，BPMN 中不再写死 `manager`/`hr` 候选组：

1. 第一审批人 = 申请人所在部门 `leader_id` 对应的负责人。
2. 如果申请人本人就是本部门负责人，自动跳过本人，由**上级部门负责人**审批，避免自己审批自己。
3. 请假天数 `> 3` 时，增加第二审批人 = 部门树上**再上一级部门**的负责人。
4. 停用或不存在的负责人会被跳过，继续沿部门树向上查找；部门树存在循环或层级过深时拒绝发起。
5. 如果组织架构不足以完成审批（例如部门未配置负责人、超过 3 天但没有上级领导），提交申请会直接返回明确错误，不会产生卡死的流程实例。
6. 提交成功时，部门、岗位、第一/第二审批人写入 `leave_request` 作为快照，便于审计和历史追溯。

流程启动时把 `deptLeaderId`、`upperLeaderId` 作为流程变量传入，BPMN 通过 `${deptLeaderId}` / `${upperLeaderId}` 指定办理人。

## 默认组织与账号

默认组织架构（由 `DataInitializer` 幂等写入）：

```
集团总部 (负责人: 陈建国)
└── 科技公司 (负责人: 周明)
    ├── 技术部 (负责人: 王强)
    ├── 人事部 (负责人: 李芳)
    ├── 财务部 (负责人: 赵磊)
    ├── 业务部 (负责人: 李业务)
    ├── 顾问部 (负责人: 周顾问主管)
    └── 会计部 (负责人: 吴会计主管)
```

默认账号（密码均为 `123456`）：

| 用户名 | 姓名 | 部门 | 岗位 | 角色 | 说明 |
| --- | --- | --- | --- | --- | --- |
| zhangsan | 张三 | 技术部 | 工程师 | employee | 普通员工，可发起请假 |
| lisi | 李四 | 技术部 | 高级工程师 | employee | 普通员工 |
| manager | 王强 | 技术部 | 部门经理 | manager | 技术部负责人 |
| zongjingli | 周明 | 科技公司 | 总经理 | executive | 科技公司负责人 |
| dongshizhang | 陈建国 | 集团总部 | 董事长 | executive | 集团负责人 |
| hrManager | 李芳 | 人事部 | 部门经理 | manager | 人事部负责人 |
| hr | 刘敏 | 人事部 | 专员 | hr | 人事专员 |
| wangwu | 王五 | 人事部 | 专员 | employee | 普通员工 |
| caiwuManager | 赵磊 | 财务部 | 部门经理 | manager | 财务部负责人 |
| ywy | 李业务 | 业务部 | 业务员 | sales | 记账合同交接发起人 |
| caiwu | 陈财务 | 财务部 | 财务专员 | finance | 财务审核 / 财务派工 / 最终确认 |
| gws | 周顾问主管 | 顾问部 | 顾问主管 | consultant_manager | 顾问主管派工 |
| gw | 林顾问 | 顾问部 | 顾问 | consultant | 顾问审核 |
| kjs | 吴会计主管 | 会计部 | 会计主管 | accounting_manager | 会计主管派工 |
| kj | 赵会计 | 会计部 | 会计 | accountant | 会计审核 |
| admin | 系统管理员 | 科技公司 | 系统管理员 | admin | 可查看所有任务 |

## 审批场景示例

| 申请人 | 请假天数 | 审批路径 |
| --- | --- | --- |
| zhangsan（技术部工程师） | 2 天 | 王强（技术部负责人） |
| zhangsan（技术部工程师） | 5 天 | 王强（技术部负责人）→ 周明（科技公司负责人） |
| manager（技术部负责人） | 5 天 | 周明（科技公司负责人）→ 陈建国（集团负责人） |
| hr（人事部专员） | 5 天 | 李芳（人事部负责人）→ 周明（科技公司负责人） |
| manager（技术部负责人） | 2 天 | 周明（科技公司负责人，自动跳过本人） |

前端「请假申请」页面会在提交前调用 `/api/org/approval-chain` 展示实际审批路径；「组织架构」页面可查看部门树、岗位和成员。

## 流程设计器（中文）与可配置审批人

流程设计器的工具栏、画布工具提示、节点右键菜单、元素类型菜单和节点类型显示均已中文化。选中用户任务后，右侧「审批人配置」可以直接选择审批人类型，不需要手写 BPMN 表达式：

| 审批人类型 | 生成表达式 | 说明 |
| --- | --- | --- |
| 部门领导 | `${approverResolver.deptLeader(applyUserId)}` | 申请人所在部门负责人；申请人本人是负责人时自动向上 |
| 上级部门领导 | `${approverResolver.upperDeptLeader(applyUserId)}` | 部门树上一级负责人 |
| 本部门指定岗位 | `${approverResolver.positionInDept(applyUserId, 'P003')}` | 申请人所在部门 + 岗位匹配唯一用户 |
| 指定岗位（全局唯一） | `${approverResolver.position(applyUserId, 'P002')}` | 岗位全局只能匹配到一个用户 |
| 指定用户 | `${approverResolver.user('6')}` | 固定用户 ID |
| 角色 / 候选组 | `flowable:candidateGroups="hr"` | 拥有该 `sys_user.role` 的用户可在待办中看到任务 |
| 发起人 | `${initiator}` | 当前发起流程的用户 |
| 表单字段 / 流程变量 | `${approverResolver.user(approverId)}` | 变量值必须是用户 ID |
| 自定义表达式 | 原样写入 `flowable:assignee` | 高级用法 |

统一解析器：`backend/src/main/java/com/example/flowable/service/ApproverResolver.java`。  
流程通过本项目的 `/api/process-instances` 启动时会自动写入 `applyUserId`，因此部门领导、岗位等表达式可以直接使用。

## 记账合同交接流程

示例流程 key：`contractHandoverProcess`  
BPMN：`backend/src/main/resources/processes/contract-handover.bpmn20.xml`

```text
业务员提交交接资料
  → 财务审核
      ├─ 不通过 → 业务员补充资料 → 财务审核
      └─ 通过 → 财务派工（选择顾问主管、会计主管）
                  → 顾问主管派工（选择顾问）
                  → 顾问审核
                      ├─ 不通过 → 业务员补充资料
                      └─ 通过 → 会计主管派工（选择会计）
                                  → 会计审核
                                  → 财务最终确认
                                  → 交接完成
```

关键规则：

- 财务审核的 5 项检查全部为“是”才可进入财务派工，否则退回业务员补充资料。
- 财务派工节点由财务选择 `consultantManagerId`、`accountingManagerId`，不是写死。
- 顾问主管派工和会计主管派工按顺序执行，不是并行。
- 顾问审核不通过时直接退回业务员补充资料，不会流转到会计主管。
- 会计主管只有在顾问审核通过后才会生成「会计主管派工」待办；在此之前会计主管没有任何待办，也不能操作。
- 财务最终确认通过后流程结束。
- 当前节点办理人可以在流程实例详情页直接填写表单并提交；非办理人只能查看。

### 表单

| formKey | 表单名称 |
| --- | --- |
| contractHandoverStartForm | 记账合同交接-资料提交 |
| contractSupplementForm | 记账合同交接-补充资料 |
| contractFinanceReviewForm | 记账合同交接-财务审核 |
| financeDispatchForm | 记账合同交接-财务派工 |
| consultantDispatchForm | 记账合同交接-顾问主管派工 |
| consultantReviewForm | 记账合同交接-顾问审核 |
| accountingDispatchForm | 记账合同交接-会计主管派工 |
| accountingReviewForm | 记账合同交接-会计审核 |
| contractFinanceFinalForm | 记账合同交接-财务最终确认 |

### 测试账号

| 用户名 | 角色 | 用途 |
| --- | --- | --- |
| ywy | sales | 发起交接、补充资料 |
| caiwu | finance | 财务审核、财务派工、最终确认 |
| gws | consultant_manager | 顾问主管派工 |
| gw | consultant | 顾问审核 |
| kjs | accounting_manager | 会计主管派工 |
| kj | accountant | 会计审核 |

### 操作步骤

1. 用 `ywy / 123456` 登录，进入「发起流程」，选择「记账合同交接流程」。
2. 用 `caiwu / 123456` 登录，在「我的待办」处理「财务审核」。
3. 财务审核通过后，处理「财务派工」，选择顾问主管 `gws` 和会计主管 `kjs`。
4. 用 `gws` 登录，处理「顾问主管派工」，选择顾问 `gw`。
5. 用 `gw` 登录，处理「顾问审核」。
   - 如果选择不通过，会直接退回 `ywy` 的「业务员补充资料」。
   - 通过后才会生成「会计主管派工」给 `kjs`。
6. 用 `kjs` 登录，处理「会计主管派工」，选择会计 `kj`。
7. 用 `kj` 登录，处理「会计审核」。
8. 用 `caiwu` 登录，处理「财务最终确认」，填写最终完成交接月份和开始核算提成月份。

### 流程实例详情

在「流程实例」列表点击「详情」，进入独立的流程实例详情页：

- 绿色节点：已完成
- 红色节点：当前节点
- 点击已完成节点：查看该节点提交的表单
- 点击当前节点：当前办理人可以直接填写表单并提交
- 同一节点执行多次时，可以切换查看每一次的表单快照
- 审批记录在右侧时间线展示

任务表单在每次提交时会保存 `taskFormData_<taskId>` 流程变量快照，因此重复执行的节点也能看到每一次各自填写的内容。

### 表单设计器新增类型

- `section`：分组标题
- `user`：组织用户选择（支持角色过滤）
- `image`：图片上传
- `file`：文件上传
- `visibleWhen`：条件显示（例如无期初资料时隐藏明细）

上传接口：

```text
POST /api/files/upload
GET  /api/files/{id}
```

文件存储目录由 `app.upload-dir` 配置，默认 `./uploads`；已有数据库升级请执行 `backend/sql/upgrade-file.sql`。

## 请假流程逻辑

```
提交申请
   │
   ▼
部门领导审批 ──驳回──→ 审批驳回
   │同意
   ▼
天数 > 3 ? ──否──→ 审批通过
   │是
   ▼
上级领导审批 ──同意──→ 审批通过
        └──────驳回──→ 审批驳回
```

BPMN 关键配置：

- `deptLeaderApproval`：`flowable:assignee="${deptLeaderId}"`
- `upperLeaderApproval`：`flowable:assignee="${upperLeaderId}"`
- 分支条件：`${days > 3}` / `${days <= 3}`
- 审批节点创建时通过 `approvalVariableResetListener` 清理上一次的 `approved` 变量，避免多级审批沿用上一级审批结果。

## 新增组织架构接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/org/departments` | 部门树（含负责人姓名） |
| GET | `/api/org/positions` | 岗位列表 |
| GET | `/api/org/users` | 组织成员列表 |
| GET | `/api/org/approval-chain?days=5` | 当前用户按天数解析出的审批路径 |

## 功能说明

- **登录鉴权**：JWT + Spring Security，令牌存于 `localStorage`，请求头 `Authorization: Bearer <token>`。
- **组织架构**：部门树、岗位、成员展示；部门和岗位数据由数据库维护，示例数据由 `DataInitializer` 初始化。
- **流程定义**：列表、上传 BPMN 部署、挂起/激活、删除、查看 XML/流程图。
- **流程设计器**：基于 bpmn-js，支持拖拽建模、属性面板（ID/名称/`flowable:assignee`/`flowable:candidateGroups` 等）、XML 直编、导出 XML/SVG、一键部署。
- **流程实例**：运行中实例、我发起的实例、独立详情页、流程图节点点击、当前节点办理、审批记录、撤销。
- **待办/已办**：任务查询、认领、同意/驳回（写入 `approved` 变量与审批意见）。任务名称可直接进入流程实例详情，任务列表与审批记录会显示用户真实姓名。
- **自定义表单**：可视化表单设计器（拖拽/排序字段，支持文本、数字、下拉、单选、多选、日期、开关、分组标题、组织用户、图片上传、文件上传、条件显示等），表单定义落库 `wf_form`；通过 BPMN 节点上的 `flowable:formKey` 绑定表单，任务办理页与流程发起页自动按定义动态渲染，提交数据写入流程变量。
- **流程实例表单快照**：每次任务提交会保存 `taskFormData_<taskId>` 快照，流程实例详情页可以按节点、按执行次数查看当时填写的表单。
- **文件上传**：图片/文件先上传到 `POST /api/files/upload`，表单中保存文件元数据和访问地址；文件下载地址为 `/api/files/{id}`。
- **请假业务**：提交申请自动解析审批人并启动流程，流程结束后自动回写申请状态。

## 常见问题

- **启动报 `sys_department` / `sys_position` 表不存在**：已有数据库请先执行 `backend/sql/upgrade-org.sql`。
- **启动报 `dept_id` / `position_id` 字段不存在**：同上，执行升级脚本后重启。
- **上传文件报 `sys_file` 表不存在**：已有数据库请执行 `backend/sql/upgrade-file.sql`。
- **上传文件后访问 404**：确认 `app.upload-dir` 目录存在且后端有写权限；默认是后端启动目录下的 `uploads`。
- **提交请假提示未配置部门领导**：确认 `sys_user.dept_id` 已填写，且对应 `sys_department.leader_id` 指向启用用户。
- **升级前已存在的用户没有部门**：`DataInitializer` 会自动补全内置演示账号；自定义用户需要在 `sys_user` 中补充 `dept_id`/`position_id`。
- **旧流程实例仍走旧审批节点**：Flowable 按流程定义版本运行，升级前的运行中实例继续使用旧定义；新提交的申请使用新版本。
- **启动报数据表不存在**：确认 `nullCatalogMeansCurrent=true` 在 JDBC URL 中（已默认配置），并确认数据库已创建。
- **中文字体乱码（流程图）**：`application.yml` 已配置 `activity-font-name: 微软雅黑`，可按系统调整。
- **端口冲突**：修改 `application.yml` 的 `server.port` 与 `frontend/vite.config.ts` 的代理目标。
