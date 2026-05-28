# DGA 授权中心与权限治理项目开发台账清单

生成日期：2026-05-26  
适用范围：DGA 平台用户与权限管理、授权中心、RBAC、历史权限接管、开放授权接口、风险治理、AI 治理 Agent、集群与后端授权能力集成。

## 1. 总体目标

建设一个可完整运行、可审计、可持续治理的多集群多授权后端权限管理平台。平台需要同时支持 CDH/Hive/Sentry、HDP/Hive/Ranger、StarRocks SQL、Doris SQL 等授权后端，并以 DGA 本地 RBAC 模型作为统一治理入口。

核心原则：

1. 角色是“可授权权限范围”，不是“绑定即全量授予”。
2. 实际权限必须通过明确动作落地：角色子集授权、历史权限接管、直接例外授权、开放接口直接授权。
3. LDAP 组继承权限必须保留来源，不能误当成用户私有权限。
4. 历史权限接管默认非破坏性，只写 DGA 本地记录，不自动修改 Hive/Sentry/Ranger/SQL 后端。
5. 对外接口直接面向用户授权/回收，不绕过 DGA 审计记录。
6. AI Agent 只做分析、建议、报告和受控计划，不直接绕过管理员确认执行高危动作。

## 2. 总体开发顺序

| 阶段 | 目标 | 主要内容 | 前置依赖 |
|---|---|---|---|
| P0 | 平台基础 | 登录鉴权、管理员权限、集群和端点配置、数据库结构 | 无 |
| P1 | 身份基础 | DGA 用户、LDAP 用户、LDAP 组、SQL 引擎用户 | P0 |
| P2 | 授权后端抽象 | 统一 AuthorizationService，接入 Sentry/Ranger/StarRocks/Doris | P0、P1 |
| P3 | 权限账本 | 资源发现、实时权限、DGA 本地授权记录、同步 | P2 |
| P4 | RBAC 角色模型 | 角色目录、权限范围、绑定关系、批量绑定 | P3 |
| P5 | 授权中心流程 | 子集授权、子集回收、直接例外、强制按用户回收、权限校验 | P4 |
| P6 | 历史兼容 | 后端角色接管、历史用户权限接管、差异对账 | P4、P5 |
| P7 | 开放接口 | 对外查询、用户创建、直接授权、直接回收、接口文档 | P3、P5 |
| P8 | 风险治理 | 风险扫描、证据采集、责任人、治理闭环 | P3、P5 |
| P9 | AI Agent | 风险解释、治理计划、报告、受控执行审计 | P8 |
| P10 | 测试与发布 | 单元测试、集成测试、UI 验证、迁移脚本、上线检查 | 全部阶段 |

## 3. 模块清单总览

| 模块 | 前端页面/组件 | 后端接口/服务 | 核心数据对象 |
|---|---|---|---|
| 平台认证 | 登录页、平台用户管理相关入口 | `/api/auth/*`、`AuthController`、`JwtService`、`AdminGuard` | `users` |
| 集群管理 | 集群配置页面 | `/api/clusters/*`、`ClusterController`、`ClusterService` | `dga_cluster`、`dga_cluster_endpoint` |
| 用户管理 | `AccessIndex.vue`、`UserList.vue`、`CreateUserModal.vue`、`PermissionPanel.vue` | `/api/access/user`、`/api/access/users`、`/api/access/import*` | `dga_users` |
| LDAP 管理 | `LdapGroupManager.vue`、`PermissionPanel.vue` | `/api/access/ldap-groups*`、`/api/access/user/{username}/ldap-*`、`LdapService` | `ldap_group_empty_state`、LDAP 外部目录 |
| 授权后端能力 | `AuthorizationCenter.vue`、`AuthorizationWorkbenchFilters.vue` | `/api/access/capabilities*`、`AuthorizationService`、各 Provider | `dga_cluster_endpoint` |
| 资源发现 | 授权中心资源选择控件 | `/api/access/resources/*`、`/openapi/v1/authz/resources/*` | 后端实时元数据 |
| 权限账本 | `PermissionVerificationPanel.vue`、`RangerCard.vue` | `/api/access/resources/permissions`、`/api/access/sync/{username}` | `user_resource_access`、`user_hive_access` |
| RBAC 角色 | `RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue` | `/api/access/roles*`、`AuthRoleService` | `auth_role`、`auth_role_permission`、`auth_user_role`、`auth_role_assignment_audit` |
| 授权执行 | `RoleGrantWorkbench.vue` | `/api/access/grants/batch`、`/api/access/revokes/batch` | `user_resource_access` |
| 历史权限接管 | `HistoricalPermissionAdoptionModal.vue` | `/api/access/roles/{roleCode}/historical-adoption*`、`HistoricalPermissionAdoptionService` | `user_resource_access`、`auth_user_role` |
| 开放接口 | `docs/authz-openapi.md` | `/openapi/v1/authz/*`、`OpenAuthzController` | `user_resource_access` |
| 风险治理 | `AccessGovernancePanel.vue`、`UserRiskPanel.vue` | `/api/access/governance/*`、`AccessGovernanceService` | `access_governance_issue`、`access_activity_evidence`、`access_owner` |
| AI Agent | `AccessGovernancePanel.vue` | `/api/access/governance/agent/*`、`AccessGovernanceAgentService`、`DeepSeekClient` | `access_governance_ai_advice`、`access_governance_ai_plan`、`access_governance_ai_action_audit` |

## 4. 详细开发台账

### DEV-001 平台登录、JWT 鉴权与管理员权限模型

- 当前状态：已实现，需持续回归。
- 开发目标：提供平台登录、JWT 鉴权、普通管理员/超级管理员/Root 管理员权限区分，确保所有写接口可被统一保护。
- 开发顺序：第一优先级，所有后续接口依赖该模块。
- 依赖关系：无。
- 涉及页面：登录页、平台用户管理入口。
- 涉及接口：`POST /api/auth/login`、`POST /api/auth/register`、`GET /api/auth/platform-users`、`PUT /api/auth/platform-users/{username}/super-admin`。
- 涉及数据对象：`users`、`User`、`UserRepository`。
- 涉及服务：`AuthController`、`JwtService`、`JwtAuthenticationFilter`、`AdminGuard`、`SecurityConfig`。
- 验收标准：
  - 正确账号可登录并返回 JWT。
  - JWT 过期或非法时返回 401。
  - 非管理员不能调用授权写接口。
  - 删除、超级管理员配置等高危动作仅 Root 管理员可执行。
- 测试和验证方式：
  - 使用 curl 登录并携带 token 调用只读/写接口。
  - 用普通用户 token 调用 `/api/access/grants/batch` 预期 403。
  - 用过期或篡改 token 调用 `/api/access/roles` 预期 401。

### DEV-002 集群与授权端点配置

- 当前状态：已实现，需结合环境持续验证。
- 开发目标：统一维护 CDH、HDP、StarRocks、Doris 等集群和 LDAP/Hive/Ranger/SQL 端点配置，作为授权后端选择和能力识别基础。
- 开发顺序：P0，在用户、资源、授权后端前完成。
- 依赖关系：DEV-001。
- 涉及页面：集群管理页面、授权中心集群下拉、用户管理集群下拉。
- 涉及接口：`GET /api/clusters`、`POST /api/clusters`、`PUT /api/clusters/{id}`、`DELETE /api/clusters/{id}`、`GET /api/clusters/{clusterCode}/endpoints`、`POST /api/clusters/{clusterCode}/endpoints`、`POST /api/clusters/{clusterCode}/endpoints/test`。
- 涉及数据对象：`dga_cluster`、`dga_cluster_endpoint`、`Cluster`、`ClusterEndpoint`。
- 涉及服务：`ClusterController`、`ClusterService`、`HiveServer2ConnectionService`。
- 验收标准：
  - 能创建、更新、软删除集群。
  - 能配置 HiveServer2、LDAP、Ranger、StarRocks、Doris 等端点。
  - 端点连通性测试能返回明确成功/失败信息。
  - 授权中心只展示有效集群和可用授权后端。
- 测试和验证方式：
  - 使用前端集群页面配置端点并执行测试。
  - 调用 `/api/clusters` 确认可见集群。
  - 调用 `/api/access/capabilities/backends?cluster=CDH` 确认授权后端识别正确。

### DEV-003 数据库基础结构与迁移脚本治理

- 当前状态：部分已实现，需补强迁移一致性。
- 开发目标：确保运行环境、测试环境、生产升级脚本包含当前授权中心、RBAC、治理、AI Agent 所需全部表和字段。
- 开发顺序：P0，与 DEV-001、DEV-002 并行推进。
- 依赖关系：无。
- 涉及页面：无直接页面。
- 涉及接口：所有需要数据库持久化的接口。
- 涉及数据对象：`users`、`dga_users`、`user_resource_access`、`auth_role`、`auth_role_permission`、`auth_user_role`、`auth_role_assignment_audit`、`access_governance_issue`、`access_activity_evidence`、`access_owner`、`access_governance_ai_*`。
- 涉及文件：`dga-backend/src/main/resources/schema.sql`、`docs/sql/*.sql`、`docs/database-change-log.md`。
- 验收标准：
  - 全新环境执行 schema 后能启动后端。
  - 生产升级脚本包含 RBAC 表、`user_resource_access` RBAC 字段、治理 AI 表。
  - 文档 SQL 与运行 schema 不存在关键表缺失。
  - 所有新增表有必要索引和唯一约束。
- 测试和验证方式：
  - 在空库执行 schema 初始化并启动后端。
  - 在旧库执行升级脚本后运行 RBAC、历史接管、AI Agent 基本接口。
  - 使用 SQL 检查关键表和字段是否存在。

### DEV-004 DGA 用户创建、导入、查询与删除保护

- 当前状态：已实现，需结合外部接口文档持续校验。
- 开发目标：管理 DGA 中的受管大数据用户，支持创建、导入、查询、保护、删除和本地用户状态维护。
- 开发顺序：P1，依赖平台认证和集群配置。
- 依赖关系：DEV-001、DEV-002、DEV-003。
- 涉及页面：`AccessIndex.vue`、`UserList.vue`、`CreateUserModal.vue`、`PermissionPanel.vue`。
- 涉及接口：`POST /api/access/user`、`GET /api/access/users`、`POST /api/access/import`、`POST /api/access/import-auth-backend`、`PUT /api/access/user/{username}/protection`、`DELETE /api/access/user/{username}`。
- 涉及数据对象：`dga_users`、`DgaUser`、`DgaUserRepository`。
- 涉及服务：`AccessController`、`DgaUserSchemaService`、`AuthorizationService`、`LdapService`。
- 验收标准：
  - 能按集群创建用户，重复创建同集群未删除用户返回 409。
  - OpenLDAP 用户、SQL 后端用户创建路径可区分。
  - 受保护用户不能被普通管理员误删。
  - 用户列表能按集群、关键字、来源、状态展示。
- 测试和验证方式：
  - 前端创建 OpenLDAP 用户并查询列表。
  - 前端创建 StarRocks/Doris 用户并确认后端用户存在。
  - 用普通管理员删除受保护用户预期失败。
  - 调用 `/api/access/users?cluster=CDH` 校验分页和字段。

### DEV-005 LDAP 用户、组、主组、附加组和账号修复

- 当前状态：已实现，需加强端到端验证。
- 开发目标：提供 OpenLDAP 用户/组治理能力，支撑 Sentry/Ranger 场景下的用户组授权模型。
- 开发顺序：P1，在 Sentry/Ranger 权限管理前完成。
- 依赖关系：DEV-002、DEV-004。
- 涉及页面：`PermissionPanel.vue`、`LdapGroupManager.vue`、`CreateUserModal.vue`、`AuthorizationWorkbenchFilters.vue`。
- 涉及接口：`GET /api/access/ldap-groups`、`POST /api/access/ldap-groups`、`PUT /api/access/ldap-groups/{groupName}`、`DELETE /api/access/ldap-groups/{groupName}`、`PUT /api/access/ldap-groups/{groupName}/members`、`GET /api/access/user/{username}/ldap-profile`、`PUT /api/access/user/{username}/ldap-group`、`PUT /api/access/user/{username}/ldap-supplementary-groups`、`PUT /api/access/user/{username}/ldap-password`、`PUT /api/access/user/{username}/ldap-lock`、`POST /api/access/user/{username}/repair-ldap`。
- 涉及数据对象：LDAP 外部目录、`ldap_group_empty_state`、`LdapGroupEmptyState`。
- 涉及服务：`LdapService`、`AccessGovernanceService`。
- 验收标准：
  - 能查询 LDAP 用户完整属性、主组、附加组、锁定状态。
  - 能创建、更新、删除 LDAP posixGroup。
  - 能维护用户主组和附加组。
  - Sentry/Ranger 授权中心可基于组过滤目标用户。
- 测试和验证方式：
  - 在前端用户详情修改用户主组并重新查询 LDAP profile。
  - 创建空组后运行治理扫描，确认可识别 stale empty group。
  - 在授权中心选择 GROUP subject，验证目标用户候选只展示组内用户。

### DEV-006 授权后端能力模型与 Provider 抽象

- 当前状态：已实现，需持续扩展能力描述。
- 开发目标：用统一 `AuthorizationService` 屏蔽 Sentry、Ranger、StarRocks、Doris 的差异，为前端提供一致能力和资源接口。
- 开发顺序：P2，所有授权执行和资源发现依赖此模块。
- 依赖关系：DEV-002、DEV-003、DEV-005。
- 涉及页面：`AuthorizationCenter.vue`、`AuthorizationWorkbenchFilters.vue`、`CreateUserModal.vue`、`UserList.vue`。
- 涉及接口：`GET /api/access/capabilities`、`GET /api/access/capabilities/backends`、`GET /api/access/resources/databases`、`GET /api/access/resources/tables`、`GET /api/access/resources/principals`、`GET /api/access/resources/permissions`。
- 涉及数据对象：`dga_cluster_endpoint`、授权后端实时资源。
- 涉及服务：`AuthorizationService`、`AuthorizationProvider`、`AuthorizationCapability`、`AuthorizationProviderDescriptor`、`AuthorizationSupport`。
- 验收标准：
  - 同一前端页面能根据 capability 显示不同集群后端能力。
  - LDAP 型后端显示 USER/GROUP，SQL 型后端只显示 USER。
  - 不同后端资源查询和权限查询走统一接口。
  - Provider 不支持的动作返回清晰错误，不静默失败。
- 测试和验证方式：
  - 分别选择 CDH/SENTRY、HDP/RANGER、StarRocks、Doris，检查 capability 返回和 UI 显示。
  - 运行 `authorizationCenterHelpers.test.js` 验证 capability 解释逻辑。

### DEV-007 CDH/Hive/Sentry 授权后端接入

- 当前状态：已实现，需继续验证复杂 Sentry grant 文本。
- 开发目标：支持 CDH Hive + Sentry 的库表查询、用户权限查询、角色权限、LDAP 组继承、用户私有角色处理。
- 开发顺序：P2，优先于历史权限接管和 Sentry 后端角色导入。
- 依赖关系：DEV-005、DEV-006。
- 涉及页面：`AuthorizationCenter.vue`、`RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue`、`HistoricalPermissionAdoptionModal.vue`。
- 涉及接口：资源查询接口、`/api/access/grants/batch`、`/api/access/revokes/batch`、`/api/access/roles/backend`、`/api/access/roles/backend/import`。
- 涉及数据对象：Sentry 后端角色、LDAP 组、`user_resource_access`、`auth_role*`。
- 涉及服务：`CdhSentryAuthorizationProvider`、`HiveAuthService`、`HistoricalPermissionAdoptionService`。
- 验收标准：
  - 能识别 USER、USER_ROLE、GROUP_ROLE 权限来源。
  - 能正确解析 `GRANT SELECT ON DATABASE ...` 等原始授权文本。
  - 强制按用户回收不误回收 LDAP 组继承权限。
  - 后端角色盘点和导入不修改 Sentry 后端状态。
- 测试和验证方式：
  - 运行 `CdhSentryAuthorizationProviderTest`。
  - 运行 `HistoricalPermissionAdoptionServiceTest` 中 raw grant 解析用例。
  - 手工查询一个通过 LDAP 组继承权限的用户，确认权限来源展示为 GROUP_ROLE。

### DEV-008 HDP/Hive/Ranger 授权后端接入

- 当前状态：已实现，需加强策略回收回归。
- 开发目标：支持 HDP Hive + Ranger 的用户/组策略授权和回收，并适配 Ranger 无原生 DB 角色的特点。
- 开发顺序：P2，在 RBAC 子集授权前完成。
- 依赖关系：DEV-005、DEV-006。
- 涉及页面：`AuthorizationCenter.vue`、`RoleGrantWorkbench.vue`、`PermissionPanel.vue`、`RangerCard.vue`。
- 涉及接口：`/api/access/resources/*`、`/api/access/grants/batch`、`/api/access/revokes/batch`、`/api/access/sync/{username}`。
- 涉及数据对象：Ranger policy、`user_resource_access`、LDAP 组。
- 涉及服务：`RangerAuthorizationProvider`、`RangerService`。
- 验收标准：
  - Ranger 用户/组主体可查询。
  - USER/GROUP 授权和回收落到正确 Ranger policy item。
  - 角色绑定不假设 Ranger 有原生数据库角色。
  - 组授权时目标用户仅用于校验，不误改授权对象。
- 测试和验证方式：
  - 在 HDP/Ranger 选择 GROUP 授权对象，执行子集授权和回收。
  - 校验 Ranger policy 变化。
  - 同步用户权限后确认 live vs recorded 状态正确。

### DEV-009 StarRocks SQL 授权后端接入

- 当前状态：已实现基础能力，需补充端到端用例。
- 开发目标：支持 StarRocks SQL 用户、库表、权限、角色能力，并且在前端隐藏组概念。
- 开发顺序：P2，可与 Doris 并行。
- 依赖关系：DEV-002、DEV-006。
- 涉及页面：`AuthorizationCenter.vue`、`CreateUserModal.vue`、`UserList.vue`。
- 涉及接口：`/api/access/resources/*`、`/api/access/grants/batch`、`/api/access/revokes/batch`、`/api/access/user`、`/api/access/import-auth-backend`。
- 涉及数据对象：StarRocks SQL 用户/角色/权限、`dga_users`、`user_resource_access`。
- 涉及服务：`StarRocksSqlAuthorizationProvider`、`AuthorizationService`。
- 验收标准：
  - 前端只展示 USER subject。
  - 能创建 StarRocks 用户。
  - 能查询库表和用户权限。
  - 能执行直接授权、回收和角色相关动作。
- 测试和验证方式：
  - 在 StarRocks 集群创建用户并查询 `SHOW USERS`。
  - 授予库级 SELECT 后调用 `/api/access/resources/permissions` 确认权限存在。
  - 回收后确认权限消失或变为 recorded-only。

### DEV-010 Doris SQL 授权后端接入

- 当前状态：已实现基础能力，需补充端到端用例。
- 开发目标：支持 Doris SQL 用户、库表、权限、角色能力，并且在前端隐藏组概念。
- 开发顺序：P2，与 StarRocks 并行。
- 依赖关系：DEV-002、DEV-006。
- 涉及页面：`AuthorizationCenter.vue`、`CreateUserModal.vue`、`UserList.vue`。
- 涉及接口：`/api/access/resources/*`、`/api/access/grants/batch`、`/api/access/revokes/batch`、`/api/access/user`、`/api/access/import-auth-backend`。
- 涉及数据对象：Doris SQL 用户/角色/权限、`dga_users`、`user_resource_access`。
- 涉及服务：`DorisSqlAuthorizationProvider`、`AuthorizationService`。
- 验收标准：
  - 前端只展示 USER subject。
  - 能创建 Doris 用户。
  - 能查询库表和用户权限。
  - 能执行授权和回收。
- 测试和验证方式：
  - 在 Doris 集群执行用户创建、授权、回收闭环。
  - 调用 `/api/access/capabilities` 确认 `authBackend=DORIS_SQL`。

### DEV-011 资源发现与权限同步

- 当前状态：已实现，需加强分页/大资源量验证。
- 开发目标：统一查询用户、组、数据库、表、实时权限，并能把后端权限同步到 DGA 本地视图用于校验。
- 开发顺序：P3，在 RBAC 和授权执行前完成。
- 依赖关系：DEV-006 至 DEV-010。
- 涉及页面：`AuthorizationCenter.vue`、`RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue`、`PermissionVerificationPanel.vue`、`RangerCard.vue`。
- 涉及接口：`GET /api/access/resources/principals`、`GET /api/access/resources/databases`、`GET /api/access/resources/tables`、`GET /api/access/resources/permissions`、`POST /api/access/sync/{username}`。
- 涉及数据对象：后端实时资源、`user_resource_access`、`user_hive_access`。
- 涉及服务：`AuthorizationService`、`AccessController`。
- 验收标准：
  - 不同后端都能返回库表列表。
  - 权限快照包含 resourceType、databaseName、tableName、permission、source、grantText。
  - 同步权限不会破坏已有 DGA 审计字段。
  - 内部系统库过滤规则符合预期。
- 测试和验证方式：
  - 对 CDH/HDP/SR/Doris 分别调用资源查询接口。
  - 选择用户后点击“同步权限”，确认权限校验面板刷新。
  - 大库表量情况下检查页面加载和接口响应。

### DEV-012 DGA 本地权限账本与 live vs recorded 校验

- 当前状态：已实现，需持续完善差异解释。
- 开发目标：以 `user_resource_access` 作为统一权限账本，记录来源、角色、主体、审批、过期、风险、所有者、复核状态，并与 live 权限做差异对账。
- 开发顺序：P3，授权、回收、治理全部依赖。
- 依赖关系：DEV-003、DEV-011。
- 涉及页面：`PermissionVerificationPanel.vue`、`RangerCard.vue`、`RoleGrantWorkbench.vue`。
- 涉及接口：`GET /api/access/resources/permissions`、`GET /api/access/user/access`、`POST /api/access/sync/{username}`。
- 涉及数据对象：`user_resource_access`、`UserResourceAccess`、`UserResourceAccessRepository`。
- 验收标准：
  - `MATCHED` 表示 live 与 recorded 一致。
  - `LIVE_ONLY` 表示后端有权限但 DGA 未记录。
  - `RECORDED_ONLY` 表示 DGA 有记录但后端无实时权限。
  - `ROLE_ADOPTION`、`DIRECT_EXCEPTION`、`OPENAPI` 等来源能正确展示。
- 测试和验证方式：
  - 手工构造 live-only 权限后刷新校验面板。
  - 通过开放接口授权后确认 recorded 来源为 OPENAPI。
  - 历史接管后确认 source 和 grantMode 展示正确。

### DEV-013 RBAC 角色目录与角色元数据

- 当前状态：已实现，需规范角色命名和治理字段。
- 开发目标：建立 DGA 本地角色目录，角色作为授权范围模板，记录集群、后端、业务域、状态、有效期等治理信息。
- 开发顺序：P4，先于角色权限和绑定。
- 依赖关系：DEV-003、DEV-006。
- 涉及页面：`AuthorizationCenter.vue`、`RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue`。
- 涉及接口：`GET /api/access/roles`、`POST /api/access/roles`、`PUT /api/access/roles/{roleCode}`、`DELETE /api/access/roles/{roleCode}`、`GET /api/access/roles/{roleCode}/effective-permissions`。
- 涉及数据对象：`auth_role`、`AuthRole`、`AuthRoleRepository`。
- 涉及服务：`AuthRoleService`。
- 验收标准：
  - 能创建、编辑、禁用、软删除角色。
  - 角色按集群和授权后端过滤。
  - 删除角色不自动破坏后端权限。
  - 角色命名符合“业务域_数据层_权限级别_环境”规范。
- 测试和验证方式：
  - 前端创建角色并查询列表。
  - 修改角色状态后验证授权工作台按钮状态。
  - 删除角色后确认列表不展示且后端权限未被误回收。

### DEV-014 RBAC 角色权限范围维护

- 当前状态：已实现，需继续覆盖表级展开和 ALL 权限兼容。
- 开发目标：维护角色可授权资源范围，支持库级、表级权限和数据库权限展开为表级子集。
- 开发顺序：P4，在角色目录后、授权执行前。
- 依赖关系：DEV-011、DEV-013。
- 涉及页面：`RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue`。
- 涉及接口：`POST /api/access/roles/{roleCode}/permissions`、`DELETE /api/access/roles/{roleCode}/permissions/{permissionId}`、`GET /api/access/roles/{roleCode}/permissions/{permissionId}/tables`。
- 涉及数据对象：`auth_role_permission`、`AuthRolePermission`、`AuthRolePermissionRepository`。
- 涉及服务：`AuthRoleService`、`AccessController`。
- 验收标准：
  - 能添加数据库级和表级权限范围。
  - 不能重复添加等价权限。
  - 删除权限范围不自动误删后端用户权限。
  - 数据库级角色权限可展开为具体表用于子集授权。
- 测试和验证方式：
  - 运行 `AccessControllerTableSubsetTest`。
  - 前端给角色添加数据库级 SELECT，再展开选择部分表授权。
  - 使用 ALL 权限验证是否覆盖 SELECT 子集。

### DEV-015 RBAC 角色绑定语义

- 当前状态：已实现关键语义，需持续回归。
- 开发目标：绑定角色只建立用户/组可授权范围，不默认下发完整角色权限；不同后端根据能力设置 backendSyncStatus。
- 开发顺序：P4，在子集授权前。
- 依赖关系：DEV-013、DEV-014。
- 涉及页面：`RoleGrantWorkbench.vue`、`AuthorizationWorkbenchFilters.vue`。
- 涉及接口：`POST /api/access/roles/{roleCode}/assignments`、`DELETE /api/access/roles/{roleCode}/assignments`。
- 涉及数据对象：`auth_user_role`、`auth_role_assignment_audit`、`AuthUserRole`、`AuthRoleAssignmentAudit`。
- 涉及服务：`AuthRoleService`、`AuthorizationService`。
- 验收标准：
  - Sentry/Ranger 组场景绑定后不会自动授予完整角色权限。
  - 绑定记录可标记为 `LOCAL_ONLY` 或对应后端同步状态。
  - 删除绑定不自动回收用户已经通过其他路径拥有的权限。
  - 审计表记录绑定、回收、接管等动作。
- 测试和验证方式：
  - 运行 `AuthRoleServiceTest`。
  - 前端绑定角色后查询用户 live 权限，确认没有自动新增整角色权限。
  - 再执行子集授权，确认只有选中子集下发。

### DEV-016 批量角色绑定与 dry-run

- 当前状态：已实现，需扩展 UI 大批量体验。
- 开发目标：为历史用户批量补齐 DGA 角色绑定关系，先 dry-run 展示影响，再确认执行。
- 开发顺序：P4，在单个角色绑定稳定后。
- 依赖关系：DEV-004、DEV-015。
- 涉及页面：`RoleGrantWorkbench.vue`。
- 涉及接口：`POST /api/access/roles/{roleCode}/assignments/batch/dry-run`、`POST /api/access/roles/{roleCode}/assignments/batch`。
- 涉及数据对象：`auth_user_role`、`auth_role_assignment_audit`、`dga_users`。
- 涉及 DTO：`BatchRoleAssignmentRequest`、`BatchRoleAssignmentResult`、`BatchRoleAssignmentItem`。
- 验收标准：
  - 支持换行/逗号分隔用户名解析。
  - dry-run 能识别不存在、重复、已绑定、可绑定用户。
  - 执行结果逐用户返回成功/失败/跳过原因。
  - 单批最大数量限制生效。
- 测试和验证方式：
  - 运行 `AuthRoleServiceTest` 批量绑定用例。
  - 前端输入混合用户列表执行 dry-run，确认结果分类。
  - 执行后查询角色绑定列表。

### DEV-017 角色子集授权

- 当前状态：已实现，需继续验证多后端行为。
- 开发目标：管理员在角色范围内选择部分权限，下发给当前用户或组，确保不越过角色权限边界。
- 开发顺序：P5，依赖角色、绑定、权限范围。
- 依赖关系：DEV-011、DEV-014、DEV-015。
- 涉及页面：`RoleGrantWorkbench.vue`、`PermissionVerificationPanel.vue`。
- 涉及接口：`POST /api/access/grants/batch`。
- 涉及数据对象：`user_resource_access`、`auth_role_permission`、`auth_user_role`。
- 涉及 DTO：`BatchGrantRequest`、`BatchGrantRequest.RolePermissionSelection`。
- 验收标准：
  - 只能授权角色范围内权限。
  - 数据库级角色范围可选择部分表下发。
  - `roleSubsetMode` 下 payload 必须包含角色和选中权限。
  - 成功后 DGA 本地记录包含 roleCode、grantMode、subjectType、subjectName。
- 测试和验证方式：
  - 使用 CDH/HDP/SR/Doris 分别执行一条子集授权。
  - 尝试选择角色范围外权限，预期后端拒绝。
  - 刷新权限校验面板，确认 `MATCHED`。

### DEV-018 角色子集回收

- 当前状态：已实现，需继续验证 Ranger/Sentry 差异。
- 开发目标：仅回收管理员选中的角色子集权限，不误删角色外权限、LDAP 组继承权限或其他角色来源权限。
- 开发顺序：P5，在子集授权后。
- 依赖关系：DEV-012、DEV-017。
- 涉及页面：`RoleGrantWorkbench.vue`、`PermissionVerificationPanel.vue`。
- 涉及接口：`POST /api/access/revokes/batch`。
- 涉及数据对象：`user_resource_access`、`auth_role_permission`。
- 涉及服务：`AuthorizationService`、各 Provider。
- 验收标准：
  - 只回收请求中明确选择的权限。
  - 回收后本地记录软删除或标记为已回收。
  - Ranger 走策略项清理，不调用不存在的原生角色回收能力。
  - Sentry 用户直授权/用户私有角色/组继承路径分开处理。
- 测试和验证方式：
  - 对已授权用户执行部分回收，确认剩余权限仍存在。
  - 对 GROUP_ROLE 权限尝试按用户强制回收，确认被阻止。
  - 刷新 live vs recorded 状态。

### DEV-019 直接例外授权与回收

- 当前状态：已实现，需强化审批字段校验。
- 开发目标：支持无法纳入角色范围的临时直接例外授权，并要求记录原因、工单、审批人、风险和过期时间。
- 开发顺序：P5，在 RBAC 主流程之后。
- 依赖关系：DEV-012、DEV-011。
- 涉及页面：`RoleGrantWorkbench.vue`。
- 涉及接口：`POST /api/access/grants/batch`、`POST /api/access/revokes/batch`。
- 涉及数据对象：`user_resource_access` 字段 `grant_mode`、`exception_reason`、`ticket_no`、`approver`、`expires_at`、`risk_level`。
- 涉及服务：`AccessController`、`AuthorizationService`。
- 验收标准：
  - 直接例外不要求角色绑定。
  - 高风险或临时权限必须有理由、审批人、过期时间。
  - 例外授权来源清晰标记为 `DIRECT_EXCEPTION`。
  - 回收直接例外不会影响角色子集权限。
- 测试和验证方式：
  - 前端填写直接例外授权表单并提交。
  - 查询 `user_resource_access` 确认审批字段落库。
  - 回收后确认记录被软删除或状态更新。

### DEV-020 强制按用户回收

- 当前状态：已实现，需继续 UI 验证。
- 开发目标：基于目标用户已有 live/recorded 权限打开选择弹窗，管理员选择具体权限进行回收，不依赖当前选中角色。
- 开发顺序：P5，在权限账本和回收能力后。
- 依赖关系：DEV-012、DEV-018。
- 涉及页面：`RoleGrantWorkbench.vue`、`PermissionVerificationPanel.vue`。
- 涉及接口：`POST /api/access/revokes/batch`、`GET /api/access/resources/permissions`。
- 涉及数据对象：`user_resource_access`、后端 live grants。
- 涉及 DTO：`BatchGrantRequest.forceUserRevoke`。
- 验收标准：
  - 按用户回收按钮基于用户权限可用性启用。
  - 弹窗展示用户实际已有权限，不按角色范围过滤。
  - LDAP 组继承权限默认不可按用户回收。
  - 后端不会要求 `roleCode`。
- 测试和验证方式：
  - 选择有 live-only 权限用户，打开强制回收弹窗。
  - 选择一条 USER/USER_ROLE 权限回收，确认后端权限消失。
  - 选择 GROUP_ROLE 权限确认按钮不可用或有明确提示。

### DEV-021 权限校验与差异解释面板

- 当前状态：已实现，需扩展更多说明文案。
- 开发目标：让管理员在每次授权、回收、接管后可以看到 live vs recorded 差异，并理解 `LIVE_ONLY`、`RECORDED_ONLY`、`MATCHED`。
- 开发顺序：P5，与授权执行同步完成。
- 依赖关系：DEV-012。
- 涉及页面：`PermissionVerificationPanel.vue`、`RoleGrantWorkbench.vue`。
- 涉及接口：`GET /api/access/resources/permissions`、`POST /api/access/sync/{username}`。
- 涉及数据对象：`user_resource_access`、后端实时权限。
- 验收标准：
  - 差异状态统计准确。
  - 权限来源文案区分 OPENAPI、ROLE_ADOPTION、DIRECT_EXCEPTION、GROUP_ROLE。
  - 同步按钮刷新实时权限。
  - 用户能理解 live-only 不代表 DGA 已授权。
- 测试和验证方式：
  - 构造三种状态并截图确认展示。
  - 历史接管后确认面板说明“未修改后端授权”。

### DEV-022 后端原生角色盘点与接管

- 当前状态：已实现 Sentry 重点能力，需扩展其他后端策略。
- 开发目标：只读盘点后端已有角色、权限和可发现的组绑定，并接管到 DGA 本地角色元数据，不修改后端授权。
- 开发顺序：P6，在 RBAC 角色模型稳定后。
- 依赖关系：DEV-007、DEV-013、DEV-014、DEV-015。
- 涉及页面：`AuthorizationCenter.vue`、`RoleCatalogPanel.vue`。
- 涉及接口：`GET /api/access/roles/backend`、`POST /api/access/roles/backend/import`。
- 涉及数据对象：后端角色、`auth_role`、`auth_role_permission`、`auth_user_role`、`auth_role_assignment_audit`。
- 涉及服务：`AuthRoleService`、`CdhSentryAuthorizationProvider`。
- 验收标准：
  - 盘点操作不创建、不删除、不授权、不回收后端角色。
  - 能选择后端角色导入为 DGA 角色。
  - 已存在角色幂等处理。
  - 导入动作有审计记录。
- 测试和验证方式：
  - 运行 `CdhSentryAuthorizationProviderTest`。
  - 在前端点击“同步后端角色”，导入后查询 DGA 角色列表。
  - 对后端执行 SHOW ROLES，确认后端状态未被修改。

### DEV-023 历史用户权限接管预览

- 当前状态：已实现，需持续增加真实样本。
- 开发目标：把历史用户 live 权限与 DGA 角色范围做服务端 diff，识别 exact/subset/superset/partial/group-inherited/no-overlap/already-adopted。
- 开发顺序：P6，在角色范围和权限校验稳定后。
- 依赖关系：DEV-012、DEV-014、DEV-021。
- 涉及页面：`HistoricalPermissionAdoptionModal.vue`、`RoleGrantWorkbench.vue`。
- 涉及接口：`POST /api/access/roles/{roleCode}/historical-adoption/preview`。
- 涉及数据对象：`auth_role_permission`、`user_resource_access`、后端 live grants。
- 涉及服务：`HistoricalPermissionAdoptionService`。
- 验收标准：
  - 能解析结构化权限和 Sentry 原始 grant 文本。
  - 能区分 direct/user-role 与 LDAP group inherited。
  - 结果包含 summary、items、warnings、snapshotHash。
  - 组继承权限默认不自动选中。
- 测试和验证方式：
  - 运行 `HistoricalPermissionAdoptionServiceTest`。
  - 用真实 Sentry 用户执行预览，确认有交集时被识别。
  - 修改后端权限后使用旧 snapshotHash 接管预期失败。

### DEV-024 历史用户权限接管执行

- 当前状态：已实现，需继续完善角色外权限治理引导。
- 开发目标：管理员确认后把历史权限写入 DGA 本地记录和 LOCAL_ONLY 角色绑定，不修改后端授权。
- 开发顺序：P6，在预览稳定后。
- 依赖关系：DEV-023、DEV-015。
- 涉及页面：`HistoricalPermissionAdoptionModal.vue`、`PermissionVerificationPanel.vue`。
- 涉及接口：`POST /api/access/roles/{roleCode}/historical-adoption`。
- 涉及数据对象：`user_resource_access`、`auth_user_role`、`auth_role_assignment_audit`。
- 涉及服务：`HistoricalPermissionAdoptionService`、`AuthRoleService`。
- 验收标准：
  - exact/subset/superset/partial 场景按确认项执行。
  - `GROUP_ROLE` 接管按组来源记录，不当成用户私有权限。
  - 重复点击幂等，不生成重复记录。
  - 不调用任何后端 grant/revoke/assign API。
- 测试和验证方式：
  - 运行 `HistoricalPermissionAdoptionServiceTest` 并验证 no backend mutation。
  - 前端 exact match 直接接管后刷新校验面板。
  - group inherited 勾选后必须确认才可提交。

### DEV-025 角色外历史权限治理闭环

- 当前状态：待补强。
- 开发目标：对历史接管预览中的 `LIVE_EXTRA_DIRECT` / `LIVE_EXTRA_GROUP_INHERITED` 标注为待治理，提供后续处理路径。
- 开发顺序：P6，在历史接管基础能力后。
- 依赖关系：DEV-023、DEV-024、DEV-028。
- 涉及页面：`HistoricalPermissionAdoptionModal.vue`、`AccessGovernancePanel.vue`。
- 涉及接口：历史接管接口、治理 issue 接口。
- 涉及数据对象：`user_resource_access`、`access_governance_issue`。
- 验收标准：
  - 角色外权限不会被自动塞进当前角色。
  - 页面明确给出扩展当前角色、选择其他角色、保留例外、回收权限四种路径。
  - 可选生成治理 issue 或待办。
- 测试和验证方式：
  - 构造 superset/partial 用户，确认 extra 权限展示为待治理。
  - 选择治理动作后确认不会直接修改后端，除非管理员显式执行。

### DEV-026 对外开放查询接口

- 当前状态：已实现并有文档。
- 开发目标：为外部审批/工单系统提供集群、用户、库表、用户权限查询能力。
- 开发顺序：P7，在资源发现和权限查询稳定后。
- 依赖关系：DEV-006、DEV-011、DEV-012。
- 涉及页面：`docs/authz-openapi.md`。
- 涉及接口：`GET /openapi/v1/authz/clusters`、`GET /openapi/v1/authz/principals`、`GET /openapi/v1/authz/resources/databases`、`GET /openapi/v1/authz/resources/tables`、`GET /openapi/v1/authz/permissions`。
- 涉及数据对象：后端实时资源、`user_resource_access`。
- 涉及服务：`OpenAuthzController`、`AuthorizationService`。
- 验收标准：
  - 所有查询接口 bearer token 可访问。
  - 分页、keyword 过滤有效。
  - 只返回对接需要字段，不暴露敏感配置。
- 测试和验证方式：
  - 按 `docs/authz-openapi.md` curl 案例逐条执行。
  - 用无效 token 预期 401。

### DEV-027 对外直接授权与回收接口

- 当前状态：已实现并有文档。
- 开发目标：为外部审批系统提供直接用户授权/回收落地能力，并写入 DGA 本地审计记录。
- 开发顺序：P7，在授权执行稳定后。
- 依赖关系：DEV-017、DEV-018、DEV-026。
- 涉及页面：`docs/authz-openapi.md`。
- 涉及接口：`POST /openapi/v1/authz/grants`、`POST /openapi/v1/authz/revokes`。
- 涉及数据对象：`user_resource_access`。
- 涉及服务：`OpenAuthzController`、`AuthorizationService`。
- 验收标准：
  - 接口明确不走 RBAC 角色绑定和角色子集。
  - 成功授权/回收写入或更新 `user_resource_access`，来源为 `OPENAPI`。
  - 非管理员调用返回 403。
  - `TABLE` 级请求未传 tableName 返回 400。
- 测试和验证方式：
  - 使用文档 curl 授权数据库级和表级权限。
  - 授权后查 `/openapi/v1/authz/permissions`。
  - 回收后再次查询确认权限变化。

### DEV-028 对外用户创建接口治理

- 当前状态：文档已补充，建议后续封装受限 OpenAPI。
- 开发目标：允许外部审批系统在授权前创建用户，但避免暴露内部兼容字段和高危账号能力。
- 开发顺序：P7，在 DGA 用户创建稳定后。
- 依赖关系：DEV-004、DEV-026。
- 涉及页面：`docs/authz-openapi.md`。
- 涉及接口：当前 `POST /api/access/user`；建议新增 `POST /openapi/v1/authz/users`。
- 涉及数据对象：`dga_users`、LDAP 外部目录、StarRocks/Doris 用户。
- 涉及服务：`AccessController`、建议后续 `OpenAuthzController`。
- 验收标准：
  - 当前文档明确用户创建不是 `/openapi/v1/authz` 下接口。
  - 外部调用方只获得必要字段能力。
  - 创建用户不会自动授权、不会自动绑定 RBAC 角色。
  - 重复创建返回清晰冲突错误。
- 测试和验证方式：
  - 使用 `POST /api/access/user` 创建测试用户。
  - 创建后调用 `/openapi/v1/authz/principals` 能查到用户。
  - 查询权限确认无自动授权。

### DEV-029 风险治理扫描与证据采集

- 当前状态：已实现基础能力，需扩展证据源准确性。
- 开发目标：扫描长期未使用权限、高危权限、无责任人权限、超出角色基线权限、空 LDAP 组、用户生命周期风险等问题。
- 开发顺序：P8，在权限账本和用户体系稳定后。
- 依赖关系：DEV-004、DEV-012、DEV-015。
- 涉及页面：`AccessGovernancePanel.vue`、`UserRiskPanel.vue`。
- 涉及接口：`POST /api/access/governance/scan`、`GET /api/access/governance/summary`、`GET /api/access/governance/issues`、`GET /api/access/governance/sources`。
- 涉及数据对象：`access_governance_issue`、`access_activity_evidence`、`user_resource_access`、`dga_users`、`ldap_group_empty_state`。
- 涉及服务：`AccessGovernanceService`。
- 验收标准：
  - 扫描能按集群和来源执行。
  - 风险 issue 包含严重级别、证据、建议动作、检测时间。
  - 重复扫描不产生不可控重复 issue。
  - Summary 与列表数据一致。
- 测试和验证方式：
  - 构造无 owner、高危 ALL、长期未活跃权限后执行 scan。
  - 检查 issue 列表和 summary 计数。
  - 清理结果后再次扫描确认可重建。

### DEV-030 权限责任人与复核闭环

- 当前状态：已实现基础能力，需结合组织流程完善。
- 开发目标：给权限分配责任人、复核时间和处理状态，形成权限治理闭环。
- 开发顺序：P8，在风险扫描后。
- 依赖关系：DEV-029。
- 涉及页面：`AccessGovernancePanel.vue`。
- 涉及接口：`GET /api/access/governance/owners`、`POST /api/access/governance/owners`、`PUT /api/access/governance/permissions/{accessId}/owner`、`PUT /api/access/governance/permissions/{accessId}/review`、`PUT /api/access/governance/issues/{id}/claim`、`PUT /api/access/governance/issues/{id}/review`、`PUT /api/access/governance/issues/{id}/resolve`。
- 涉及数据对象：`access_owner`、`access_governance_issue`、`user_resource_access`。
- 涉及服务：`AccessGovernanceService`。
- 验收标准：
  - 能创建/查询 owner。
  - issue 能认领、复核、关闭。
  - 权限记录能绑定 owner 和 review 状态。
  - 操作人和时间可追溯。
- 测试和验证方式：
  - 在治理页面创建 owner 并绑定权限。
  - 认领 issue 后复核并关闭。
  - 查询数据库确认状态和时间字段更新。

### DEV-031 风险治理前端工作台

- 当前状态：已实现，需加强筛选和大列表体验。
- 开发目标：提供风险总览、来源筛选、问题列表、证据、建议、治理动作、清理结果等完整 UI。
- 开发顺序：P8，与治理接口同步。
- 依赖关系：DEV-029、DEV-030。
- 涉及页面：`AccessGovernancePanel.vue`、`UserRiskPanel.vue`。
- 涉及接口：全部 `/api/access/governance/*`。
- 涉及数据对象：`access_governance_issue`、`access_activity_evidence`、`access_owner`。
- 验收标准：
  - 页面能显示扫描摘要和 issue 列表。
  - 支持按来源、严重级别、状态、关键字过滤。
  - 扫描、清理、认领、复核、关闭动作有反馈。
  - 空状态和错误状态有明确提示。
- 测试和验证方式：
  - 启动前端手工执行扫描和筛选。
  - 模拟空结果、接口失败、长列表场景。

### DEV-032 AI 治理 Agent 配置与 LLM 调用

- 当前状态：已实现基础能力，需补齐配置和安全验证。
- 开发目标：接入大模型服务，用于权限治理风险解释、处置建议、报告生成。
- 开发顺序：P9，在治理 issue 稳定后。
- 依赖关系：DEV-029、DEV-030。
- 涉及页面：`AccessGovernancePanel.vue`。
- 涉及接口：`GET /api/access/governance/agent/config/status`、`POST /api/access/governance/agent/analyze`、`POST /api/access/governance/agent/plan`、`POST /api/access/governance/agent/report`。
- 涉及数据对象：`access_governance_ai_advice`、`access_governance_ai_plan`。
- 涉及服务：`AccessGovernanceAgentService`、`DeepSeekClient`、`AgentLlmProperties`。
- 验收标准：
  - 配置缺失时页面显示不可用状态。
  - analyze/plan/report 返回结构化结果。
  - LLM 输出落库保存，能追溯 issueId 和 operator。
  - prompt 不包含明文密码、token、端点密钥。
- 测试和验证方式：
  - 配置测试 LLM key 后调用 analyze。
  - 移除 key 后确认接口返回可解释错误。
  - 检查 AI advice/plan 表记录。

### DEV-033 AI 治理 Agent 受控执行与动作审计

- 当前状态：待开发或待补齐。
- 开发目标：把 AI 计划转为管理员确认后的可执行动作，并记录动作审计；AI 不能绕过管理员直接执行。
- 开发顺序：P9，在 AI 分析和治理动作稳定后。
- 依赖关系：DEV-030、DEV-032、DEV-018、DEV-019。
- 涉及页面：`AccessGovernancePanel.vue`。
- 涉及接口：建议实现 `POST /api/access/governance/agent/actions/execute`。
- 涉及数据对象：`access_governance_ai_action_audit`、`access_governance_issue`、`user_resource_access`。
- 涉及服务：`AccessGovernanceAgentService`、`AuthorizationService`、`AccessGovernanceService`。
- 验收标准：
  - AI 只能生成建议动作，管理员确认后才执行。
  - 支持动作类型白名单，例如标记复核、创建治理 issue、建议回收、生成报告。
  - 高危回收、删除、权限变更必须二次确认。
  - 每次执行写入 action audit，包含输入、输出、操作者、时间、结果。
- 测试和验证方式：
  - 用 mock plan 执行低风险动作，确认 audit 写入。
  - 未确认动作调用 execute 预期失败。
  - AI 建议包含非法动作时后端拒绝。

### DEV-034 外部接口文档与接入治理

- 当前状态：已维护，需随接口变更同步。
- 开发目标：为外部系统提供清晰、准确、不过度暴露能力的接口文档和联调清单。
- 开发顺序：P7-P10 持续维护。
- 依赖关系：DEV-026、DEV-027、DEV-028。
- 涉及页面/文档：`docs/authz-openapi.md`。
- 涉及接口：`/api/auth/login`、`/api/access/user`、`/openapi/v1/authz/*`。
- 涉及数据对象：`user_resource_access`、`dga_users`。
- 验收标准：
  - 文档中的路径和字段与代码一致。
  - 明确开放接口与内部授权中心流程的边界。
  - 包含错误码、curl 示例、联调清单。
  - 不写入真实密码、token 或敏感端点凭据。
- 测试和验证方式：
  - 按文档 curl 从登录到创建用户、授权、查询、回收全流程执行。
  - 对照 `OpenAuthzController` 和 `AccessController` 检查路径一致性。

### DEV-035 旧用户管理入口与新授权中心整合

- 当前状态：已实现路由跳转，需逐步收敛旧流程。
- 开发目标：保留旧用户列表/详情入口，同时将授权动作引导到新授权中心，避免旧 GrantModal 与新 RBAC 流程分裂。
- 开发顺序：P10，在授权中心主流程稳定后。
- 依赖关系：DEV-004、DEV-017。
- 涉及页面：`AccessIndex.vue`、`UserList.vue`、`PermissionPanel.vue`、`GrantModal.vue`、`AuthorizationCenter.vue`。
- 涉及接口：用户管理、权限查询、授权中心相关接口。
- 涉及数据对象：`dga_users`、`user_resource_access`。
- 验收标准：
  - 从用户列表点击授权能带 username/cluster 跳转授权中心。
  - 旧 GrantModal 不再作为主要授权路径造成审计分叉。
  - 用户详情中的权限和风险信息与授权中心一致。
- 测试和验证方式：
  - 从 `/access` 选择用户跳转 `/authorization-center`，确认上下文带入。
  - 对同一用户在两个页面查看权限记录，结果一致。

### DEV-036 前端交互、响应式和可用性验证

- 当前状态：基础可用，需补充系统性 UI 测试。
- 开发目标：确保授权中心、角色管理、治理页面在常见分辨率和高风险操作下有清晰提示和防误操作能力。
- 开发顺序：P10，贯穿每个 UI 功能上线。
- 依赖关系：所有前端功能。
- 涉及页面：`AuthorizationCenter.vue`、`AuthorizationWorkbenchFilters.vue`、`RoleCatalogPanel.vue`、`RoleGrantWorkbench.vue`、`HistoricalPermissionAdoptionModal.vue`、`PermissionVerificationPanel.vue`、`AccessGovernancePanel.vue`。
- 涉及接口：所有页面依赖接口。
- 涉及数据对象：无直接新增。
- 验收标准：
  - 目标用户和授权对象不再被非预期联动改写。
  - 破坏性动作有确认或明确按钮状态。
  - loading、空状态、错误状态清晰。
  - 1100px 和 640px 以下布局可用，无横向溢出。
- 测试和验证方式：
  - 启动 Vite 前端手工走授权主流程。
  - 使用 Playwright 或浏览器手工验证目标用户/授权对象联动。
  - 运行 `npm --prefix dga-frontend run build`。

### DEV-037 前后端自动化测试体系

- 当前状态：已有部分单元测试，需扩展端到端测试。
- 开发目标：用自动化测试覆盖核心授权安全边界，防止后续改动破坏 RBAC、历史接管、强制回收、开放接口。
- 开发顺序：P10，重要功能完成后补齐。
- 依赖关系：所有核心模块。
- 涉及页面：授权中心和治理页面。
- 涉及接口：`/api/access/*`、`/openapi/v1/authz/*`。
- 涉及数据对象：测试库中的 RBAC、用户、权限、治理表。
- 现有测试文件：`AuthRoleServiceTest`、`AccessControllerTableSubsetTest`、`HistoricalPermissionAdoptionServiceTest`、`CdhSentryAuthorizationProviderTest`、`authorizationCenterHelpers.test.js`。
- 验收标准：
  - 后端核心服务测试全部通过。
  - 前端 helper 测试通过。
  - 至少覆盖一个 UI 黄金路径：创建角色、绑定、子集授权、权限校验、回收。
  - 外部 OpenAPI curl 或集成测试覆盖授权/回收。
- 测试和验证方式：
  - `mvn -f dga-backend/pom.xml test`
  - `npm --prefix dga-frontend test -- authorizationCenterHelpers.test.js`
  - `npm --prefix dga-frontend run build`
  - Playwright/手工浏览器验证关键流程。

### DEV-038 安全、审计和凭据治理

- 当前状态：部分已实现，需专门审查。
- 开发目标：确保项目不会泄露密码/token，所有高危动作有操作者、时间、结果、原因可追溯。
- 开发顺序：P10，上线前必须完成。
- 依赖关系：DEV-001、DEV-012、DEV-015、DEV-027、DEV-032。
- 涉及页面：所有涉及密码、授权、回收、删除、AI 执行的页面。
- 涉及接口：所有写接口。
- 涉及数据对象：`dga_access_log`、`auth_role_assignment_audit`、`user_resource_access`、`access_governance_ai_action_audit`。
- 验收标准：
  - 文档和代码库不保存真实密码、token、LLM key。
  - 授权、回收、角色绑定、历史接管、AI 执行动作均可审计。
  - 高危动作权限校验完整。
  - 对外接口不会暴露内部端点配置和敏感字段。
- 测试和验证方式：
  - 搜索配置文件和文档中的明文凭据并迁移到环境变量。
  - 手工执行一条授权、一条回收、一条角色绑定，检查审计记录。
  - 使用普通用户调用写接口确认 403。

### DEV-039 发布部署、环境配置和回滚方案

- 当前状态：需按上线环境补齐。
- 开发目标：保障项目在测试/生产环境可安装、升级、回滚和排障。
- 开发顺序：P10，上线前最终阶段。
- 依赖关系：DEV-003、DEV-037、DEV-038。
- 涉及页面：无直接页面。
- 涉及接口：健康检查、核心登录和授权接口。
- 涉及数据对象：全量业务表。
- 涉及文件：`application-*.properties`、SQL 升级脚本、部署脚本、运行日志配置。
- 验收标准：
  - 测试环境和生产环境配置分离。
  - 数据库升级脚本可重复审查并有回滚说明。
  - 启动后健康检查、登录、集群查询、授权后端能力查询可用。
  - 失败时有清晰回滚步骤。
- 测试和验证方式：
  - 在测试环境执行一次冷启动部署。
  - 备份生产库后演练升级脚本。
  - 按上线检查清单执行冒烟测试。

## 5. 功能依赖关系图

```text
平台登录/AdminGuard
  -> 集群/端点配置
    -> AuthorizationService + Provider
      -> 资源发现/实时权限
        -> user_resource_access 权限账本
          -> RBAC 角色目录/权限范围/绑定
            -> 子集授权/子集回收/直接例外/强制回收
              -> 权限校验 live vs recorded
              -> 历史权限接管
              -> 开放授权接口
              -> 风险治理扫描
                -> AI Agent 分析/计划/报告/受控执行
```

## 6. 关键验收主流程

### 6.1 CDH/Sentry RBAC 主流程

1. 配置 CDH 集群、HiveServer2、LDAP 端点。
2. 创建或导入 LDAP 用户和组。
3. 创建 DGA 角色并添加数据库/表权限范围。
4. 绑定 LDAP 组到角色，确认不会自动下发完整角色权限。
5. 在角色范围内选择部分权限执行子集授权。
6. 刷新权限校验，确认 `MATCHED`。
7. 执行子集回收，确认只回收选中权限。
8. 对已有 Sentry 历史权限执行预览和接管，确认只写本地记录。

### 6.2 HDP/Ranger RBAC 主流程

1. 配置 HDP 集群、Ranger、HiveServer2、LDAP 端点。
2. 查询 Ranger 用户/组 principal。
3. 创建角色和权限范围。
4. 选择 GROUP 授权对象和目标校验用户。
5. 绑定角色，仅建立范围。
6. 子集授权落到 Ranger policy。
7. 子集回收清理具体 Ranger policy item。
8. 校验 live vs recorded。

### 6.3 StarRocks/Doris 主流程

1. 配置 SQL JDBC 端点。
2. 创建或导入 SQL 后端用户。
3. 前端确认只显示 USER subject。
4. 查询库表和用户权限。
5. 执行授权、回收和校验。

### 6.4 对外接口主流程

1. 外部系统登录获取 JWT。
2. 查询 `/openapi/v1/authz/clusters`。
3. 如用户不存在，调用 `/api/access/user` 创建用户。
4. 查询用户、库、表。
5. 调用 `/openapi/v1/authz/grants` 授权。
6. 调用 `/openapi/v1/authz/permissions` 校验。
7. 调用 `/openapi/v1/authz/revokes` 回收。

### 6.5 治理与 AI Agent 主流程

1. 执行治理扫描。
2. 查看 summary 和 issue 列表。
3. 绑定 owner，认领 issue。
4. 调用 AI analyze 解释风险。
5. 调用 AI plan 生成处置建议。
6. 管理员确认后执行治理动作。
7. 生成治理报告。
8. 全流程可审计。

## 7. 当前已识别风险与补强项

| 编号 | 风险/缺口 | 影响 | 建议处理 |
|---|---|---|---|
| RISK-001 | `docs/sql` 快照可能落后于运行 `schema.sql` | 新环境或生产升级缺表/缺字段 | 补齐 RBAC、AI Agent 表和 `user_resource_access` 新字段迁移 |
| RISK-002 | AI Agent action execute 规划存在但执行接口未完整落地 | AI 只能分析/计划，不能闭环执行审计 | 实现受控执行接口和 action audit 写入 |
| RISK-003 | 用户创建接口当前复用 `/api/access/user`，不是专用 OpenAPI | 对外能力边界不够清晰 | 后续新增受限版 `/openapi/v1/authz/users` |
| RISK-004 | 前端缺少组件级和 e2e 测试 | 授权中心复杂联动容易回归 | 补 Playwright 或 Vue 组件测试覆盖关键路径 |
| RISK-005 | 配置文件和文档需要凭据治理审查 | 明文凭据泄露风险 | 上线前做 secret scan，迁移到环境变量/密钥系统 |
| RISK-006 | Ranger/Sentry/SQL 后端行为差异大 | 同一 UI 动作可能语义不同 | 每个后端建立独立验收用例和回归数据 |
| RISK-007 | 历史权限角色外权限可能被忽略 | RBAC 治理闭环不完整 | 将 extra 权限纳入待治理 issue 或明确处理路径 |

## 8. 上线前总体验收清单

- [ ] 后端单元测试通过：`mvn -f dga-backend/pom.xml test`
- [ ] 前端 helper 测试通过：`npm --prefix dga-frontend test -- authorizationCenterHelpers.test.js`
- [ ] 前端构建通过：`npm --prefix dga-frontend run build`
- [ ] CDH/Sentry 完成角色绑定、子集授权、子集回收、历史接管验证。
- [ ] HDP/Ranger 完成组授权、组内用户校验、Ranger policy 回收验证。
- [ ] StarRocks 完成用户创建、授权、回收验证。
- [ ] Doris 完成用户创建、授权、回收验证。
- [ ] OpenAPI 文档 curl 示例全部跑通。
- [ ] 权限校验面板可解释 `MATCHED`、`LIVE_ONLY`、`RECORDED_ONLY`。
- [ ] 风险治理扫描、认领、复核、关闭流程跑通。
- [ ] AI Agent analyze/plan/report 在配置存在和缺失两种场景都有明确表现。
- [ ] 生产升级 SQL 覆盖当前 schema。
- [ ] 密码、token、LLM key、数据库连接信息不出现在文档和代码仓库中。
- [ ] 高危动作均有管理员权限校验和审计记录。

## 9. 推荐近期迭代优先级

1. 补齐 SQL 迁移：RBAC 表、AI Agent 表、`user_resource_access` 字段与索引。
2. 完成 AI Agent 受控执行和 action audit，或者在 UI 上明确当前仅支持分析/计划/报告。
3. 给开放用户创建能力封装受限版 `/openapi/v1/authz/users`。
4. 补 Playwright/端到端验证：授权中心目标用户/授权对象、子集授权、历史接管、强制回收。
5. 将历史接管中的角色外权限转为“待治理”入口，串到风险治理模块。
6. 上线前做 secret scan 和配置分离治理。
