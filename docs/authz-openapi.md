# DGA 开放授权接口文档

本文档用于对接 DGA 授权能力的外部系统，覆盖集群查询、用户查询、库表查询、用户权限查询、批量授权和批量回收。

## 1. 基础信息

- 接口前缀：`/openapi/v1/authz`
- 鉴权方式：`Bearer JWT`
- 返回格式：`application/json`
- Swagger 地址：
  - `http://{host}:8081/swagger-ui/index.html`
  - 过滤 `开放授权接口`

测试环境：10.0.21.191
生产环境：172.20.85.101

推荐流程：

1. 登录获取 JWT
2. 查询可授权集群
3. 查询用户 / 库 / 表
4. 查询用户现有权限
5. 执行批量授权或批量回收

## 2. 获取 Token

export DGA_BASE_URL="http://{host}:8081"
export DGA_USERNAME="openapi_auth"
export DGA_PASSWORD="jfiae;jgie123"

请求：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "openapi_auth",
  "password": "jfiae;jgie123"
}
```

成功响应示例：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "user": {
    "username": "openapi_auth",
    "isAdmin": true
  }
}
```

后续请求头：

```http
Authorization: Bearer <token>
```

## 3. 接口说明

### 3.1 查询可授权集群

请求：

```http
GET /openapi/v1/authz/clusters
```

响应示例：

```json
[
  {
    "clusterCode": "CDH",
    "clusterName": "CDH",
    "engineType": "HIVE",
    "authBackend": "SENTRY",
    "status": "READY"
  },
  {
    "clusterCode": "HDP",
    "clusterName": "HDP",
    "engineType": "HIVE",
    "authBackend": "RANGER",
    "status": "READY"
  }
]
```

字段说明：

- `clusterCode`：对外调用统一使用的集群编码
- `clusterName`：平台展示名称
- `engineType`：当前授权引擎，例如 `HIVE` / `STARROCKS` / `DORIS`
- `authBackend`：实际授权后端，例如 `SENTRY` / `RANGER` / `STARROCKS_SQL`
- `status`：当前授权能力状态，`READY` 才建议开放给外部调用

### 3.2 查询用户列表

请求：

```http
GET /openapi/v1/authz/principals?clusterCode=CDH&keyword=ding&page=1&pageSize=20
```

响应示例：

```json
{
  "page": 1,
  "pageSize": 20,
  "total": 2,
  "items": [
    { "name": "dingquan" },
    { "name": "dingquan_test" }
  ]
}
```

参数说明：

- `clusterCode`：必填
- `keyword`：可选，按用户名模糊过滤
- `page`：页码，从 `1` 开始
- `pageSize`：每页条数，最大 `200`

### 3.3 查询数据库列表

请求：

```http
GET /openapi/v1/authz/resources/databases?clusterCode=CDH&keyword=ods&page=1&pageSize=50
```

响应示例：

```json
{
  "page": 1,
  "pageSize": 50,
  "total": 3,
  "items": [
    { "name": "ods" },
    { "name": "ods_trade" },
    { "name": "ods_user" }
  ]
}
```

说明：

- 默认会过滤内部系统库，如 `information_schema`、`mysql`、`sys`、`performance_schema`

### 3.4 查询表列表

请求：

```http
GET /openapi/v1/authz/resources/tables?clusterCode=CDH&database=ods&keyword=user&page=1&pageSize=50
```

响应示例：

```json
{
  "page": 1,
  "pageSize": 50,
  "total": 2,
  "items": [
    { "name": "user_info" },
    { "name": "user_login_log" }
  ]
}
```

参数说明：

- `clusterCode`：必填
- `database`：必填
- `keyword`：可选

### 3.5 查询用户当前权限

请求：

```http
GET /openapi/v1/authz/permissions?clusterCode=CDH&username=dingquan
```

响应示例：

```json
{
  "clusterCode": "CDH",
  "username": "dingquan",
  "engineType": "HIVE",
  "authBackend": "SENTRY",
  "grants": [
    {
      "resourceType": "DATABASE",
      "databaseName": "ods",
      "tableName": null,
      "permission": "SELECT",
      "grantText": "GRANT SELECT ON DATABASE ods TO USER dingquan"
    },
    {
      "resourceType": "TABLE",
      "databaseName": "dwd",
      "tableName": "user_info",
      "permission": "SELECT",
      "grantText": "GRANT SELECT ON TABLE dwd.user_info TO USER dingquan"
    }
  ]
}
```

字段说明：

- `resourceType`：当前版本主要返回 `DATABASE` 或 `TABLE`
- `grantText`：底层授权后端返回的原始授权语句或授权描述

### 3.6 批量授权

请求：

```http
POST /openapi/v1/authz/grants
Content-Type: application/json

{
  "clusterCode": "CDH",
  "username": "dingquan",
  "grants": [
    {
      "resourceType": "DATABASE",
      "databaseName": "ods",
      "permission": "SELECT"
    },
    {
      "resourceType": "TABLE",
      "databaseName": "dwd",
      "tableName": "user_info",
      "permission": "SELECT"
    }
  ]
}
```

成功响应示例：

```json
{
  "success": true,
  "clusterCode": "CDH",
  "username": "dingquan",
  "processed": 2
}
```

规则说明：

- `resourceType` 支持 `DATABASE`、`TABLE`
- `permission` 当前复用平台已有权限校验，例如 `SELECT`、`INSERT`、`CREATE`、`ALL`
- `TABLE` 级授权必须传 `tableName`
- 写接口仅允许 `admin` 或平台超级用户调用

### 3.7 批量回收

请求：

```http
POST /openapi/v1/authz/revokes
Content-Type: application/json

{
  "clusterCode": "CDH",
  "username": "dingquan",
  "grants": [
    {
      "resourceType": "DATABASE",
      "databaseName": "ods",
      "permission": "SELECT"
    },
    {
      "resourceType": "TABLE",
      "databaseName": "dwd",
      "tableName": "user_info",
      "permission": "SELECT"
    }
  ]
}
```

成功响应示例：

```json
{
  "success": true,
  "clusterCode": "CDH",
  "username": "dingquan",
  "processed": 2
}
```

## 4. 错误响应

统一错误结构示例：

```json
{
  "status": 400,
  "error": "Bad Request",
  "path": "/openapi/v1/authz/grants",
  "message": "授权端点账号权限不足: Access denied ..."
}
```

常见错误：

- `400 Bad Request`
  - 参数缺失
  - `resourceType` 非法
  - `permission` 非法
  - 授权端点本身执行失败
- `401 Unauthorized`
  - 未登录或 JWT 无效
- `403 Forbidden`
  - 非 `admin` / 非超级用户调用写接口
- `500 Internal Server Error`
  - 服务端未预期异常

## 5. curl 测试案例

建议先准备环境变量：

```bash
export DGA_BASE_URL="http://localhost:8081"
export DGA_USERNAME="openapi_auth"
export DGA_PASSWORD="jfiae;jgie123"
```

### 5.1 登录并取 token

```bash
TOKEN=$(
  curl -sS -X POST "${DGA_BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${DGA_USERNAME}\",\"password\":\"${DGA_PASSWORD}\"}" \
  | jq -r '.token'
)
```

### 5.2 查询集群

```bash
curl -sS "${DGA_BASE_URL}/openapi/v1/authz/clusters" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5.3 查询用户

```bash
curl -sS "${DGA_BASE_URL}/openapi/v1/authz/principals?clusterCode=CDH&keyword=ding&page=1&pageSize=20" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5.4 查询数据库

```bash
curl -sS "${DGA_BASE_URL}/openapi/v1/authz/resources/databases?clusterCode=CDH&page=1&pageSize=20" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5.5 查询表

```bash
curl -sS "${DGA_BASE_URL}/openapi/v1/authz/resources/tables?clusterCode=CDH&database=ods&page=1&pageSize=20" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5.6 查询用户权限

```bash
curl -sS "${DGA_BASE_URL}/openapi/v1/authz/permissions?clusterCode=CDH&username=dingquan" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5.7 执行数据库级授权

```bash
curl -sS -X POST "${DGA_BASE_URL}/openapi/v1/authz/grants" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clusterCode": "CDH",
    "username": "dingquan",
    "grants": [
      {
        "resourceType": "DATABASE",
        "databaseName": "ods",
        "permission": "SELECT"
      }
    ]
  }'
```

### 5.8 执行表级授权

```bash
curl -sS -X POST "${DGA_BASE_URL}/openapi/v1/authz/grants" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clusterCode": "CDH",
    "username": "dingquan",
    "grants": [
      {
        "resourceType": "TABLE",
        "databaseName": "dwd",
        "tableName": "user_info",
        "permission": "SELECT"
      }
    ]
  }'
```

### 5.9 执行权限回收

```bash
curl -sS -X POST "${DGA_BASE_URL}/openapi/v1/authz/revokes" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clusterCode": "CDH",
    "username": "dingquan",
    "grants": [
      {
        "resourceType": "TABLE",
        "databaseName": "dwd",
        "tableName": "user_info",
        "permission": "SELECT"
      }
    ]
  }'
```

## 6. 联调测试清单

### 用例 1：查询类接口可用

目标：

- 能成功查询集群、用户、数据库、表、用户权限

检查点：

- 返回 `200`
- 集群状态为 `READY`
- 列表结构包含 `page/pageSize/total/items`

### 用例 2：数据库级授权成功

步骤：

1. 先查询用户当前权限
2. 调用 `/grants`
3. 再查询 `/permissions`

预期：

- 授权响应 `success=true`
- `processed` 与请求条数一致
- 查询权限时能看到对应 `DATABASE` 权限

### 用例 3：表级授权成功

步骤：

1. 查询表列表确认目标表存在
2. 调用 `/grants`
3. 再查 `/permissions`

预期：

- 能看到 `TABLE` 级权限
- `databaseName + tableName + permission` 正确

### 用例 4：权限回收成功

步骤：

1. 先授权
2. 再调用 `/revokes`
3. 再查 `/permissions`

预期：

- 被回收的权限不再出现在实时权限快照里

### 用例 5：参数校验失败

示例请求：

```json
{
  "clusterCode": "CDH",
  "username": "dingquan",
  "grants": [
    {
      "resourceType": "TABLE",
      "databaseName": "dwd",
      "permission": "SELECT"
    }
  ]
}
```

预期：

- 返回 `400`
- `message` 明确提示 `TABLE 级授权必须提供 tableName`

### 用例 6：权限不足

目标：

- 用普通用户 token 调用 `/grants` 或 `/revokes`

预期：

- 返回 `403`
- `message` 提示仅 `admin` 或超级用户可调用写接口

## 7. 接入建议

- 外部系统只使用 `clusterCode`，不要混用 `clusterName`
- 写接口调用前，先查一遍库表和现有权限，避免重复授权
- 建议调用方记录请求体、响应体和时间，用于审计追溯
- 如果后续需要审批流，可以继续在这一层外部接口前增加工单编排，不必改底层授权实现
