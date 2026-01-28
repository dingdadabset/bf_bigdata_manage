# 数据地图 (Data Map) - 实现文档

## 概述

数据地图是数据治理平台的核心功能模块，提供统一的数据资产检索、浏览和管理入口。用户可以通过数据地图快速定位和访问各类数据资源。

## 功能特性

### 1. 智能搜索
- **关键字搜索**: 支持按表名、字段名、描述等多维度搜索
- **AI搜索**: 集成智能搜索引擎，支持自然语言查询
- **数据源筛选**: 可按数据源类型（EMR Hive、StarRocks等）过滤结果
- **大小写敏感**: 可切换搜索时是否区分大小写

### 2. 最近浏览
- 记录用户最近访问的数据资源
- 支持按类型（表、数据源）筛选
- 一键快速访问历史浏览记录

### 3. 数据统计
- **类目导航**: 显示总采集表数量、管理表数量、纳管比例
- **元数据采集**: 按数据源展示实例、数据库、表、API、采集器数量
- **实时刷新**: 支持手动刷新统计数据

### 4. 数据管理
- 我的数据：个人关注的数据资源
- 权限管理：查看和管理数据访问权限

## 前端实现

### 文件结构
```
dga-frontend/src/
├── views/
│   └── DataMap.vue         # 数据地图主页面
└── router/
    └── index.js            # 路由配置
```

### 主要组件

#### DataMap.vue
**功能模块:**
1. **搜索区域** (`search-header`)
   - 渐变背景头部
   - 数据源选择下拉框
   - 搜索输入框
   - AI搜索和大小写切换按钮

2. **数据使用卡片** (`content-card` 左侧)
   - 类型和数据源筛选器
   - 最近浏览列表
   - 空状态提示

3. **数据管理卡片** (`content-card` 右侧)
   - 选项卡切换（我的数据/权限管理）
   - 类目导航统计
   - 元数据采集统计

**关键方法:**
- `fetchStats()`: 获取统计数据
- `fetchRecentViews()`: 获取最近浏览记录
- `handleSearch()`: 执行搜索并跳转
- `toggleAISearch()`: 切换AI搜索模式
- `formatViewTime()`: 格式化浏览时间显示

### 样式设计
- 使用 CSS Modules 实现样式隔离
- 渐变色头部 (`linear-gradient(135deg, #667eea 0%, #764ba2 100%)`)
- 卡片阴影和圆角设计
- 响应式布局支持

## 后端实现

### 文件结构
```
dga-backend/src/main/java/com/dga/
├── datamap/
│   ├── controller/
│   │   └── DataMapController.java      # REST API控制器
│   ├── entity/
│   │   └── UserRecentView.java         # 用户浏览记录实体
│   └── repository/
│       └── UserRecentViewRepository.java  # 数据访问层
└── resources/
    ├── schema.sql                      # 主数据库结构
    └── schema-datamap.sql              # 数据地图扩展表结构
```

### API接口

#### 1. 获取统计信息
```
GET /api/datamap/stats
```
**响应示例:**
```json
{
  "instanceCount": 5,
  "databaseCount": 120,
  "tableCount": 3500,
  "apiCount": 0,
  "collectorCount": 0,
  "managedCount": 3500,
  "coverage": "100%"
}
```

#### 2. 获取最近浏览
```
GET /api/datamap/recent?username={username}
```
**响应示例:**
```json
[
  {
    "id": 1,
    "username": "user001",
    "viewType": "TABLE",
    "viewContent": "ods.user_info",
    "datasourceId": 1,
    "viewedAt": "2026-01-28T10:30:00"
  }
]
```

#### 3. 添加浏览记录
```
POST /api/datamap/recent
Content-Type: application/json

{
  "username": "user001",
  "viewType": "TABLE",
  "viewContent": "ods.user_info",
  "datasourceId": 1
}
```

## 数据库设计

### 核心表结构

#### 1. dga_user_recent_views (用户最近浏览)
```sql
CREATE TABLE `dga_user_recent_views` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `view_type` VARCHAR(50) NOT NULL COMMENT 'TABLE, DATASOURCE, DATABASE, COLUMN',
  `view_content` VARCHAR(500) NOT NULL,
  `datasource_id` BIGINT,
  `viewed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_view` (`username`, `viewed_at` DESC)
);
```

#### 2. dga_search_history (搜索历史)
```sql
CREATE TABLE `dga_search_history` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `search_query` VARCHAR(500) NOT NULL,
  `search_type` VARCHAR(50) NOT NULL COMMENT 'KEYWORD, AI, ADVANCED',
  `datasource_id` BIGINT,
  `result_count` INT DEFAULT 0,
  `searched_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_search` (`username`, `searched_at` DESC)
);
```

#### 3. dga_user_favorites (用户收藏)
```sql
CREATE TABLE `dga_user_favorites` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `resource_type` VARCHAR(50) NOT NULL,
  `resource_id` BIGINT NOT NULL,
  `resource_name` VARCHAR(500) NOT NULL,
  `datasource_id` BIGINT,
  `tags` VARCHAR(500),
  `notes` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_resource` (`username`, `resource_type`, `resource_id`)
);
```

#### 4. dga_datamap_stats_cache (统计缓存)
```sql
CREATE TABLE `dga_datamap_stats_cache` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `datasource_id` BIGINT,
  `stat_type` VARCHAR(50) NOT NULL,
  `stat_name` VARCHAR(100) NOT NULL,
  `stat_value` BIGINT NOT NULL,
  `calculated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_datasource_stat` (`datasource_id`, `stat_type`, `stat_name`)
);
```

### 扩展表（在 schema-datamap.sql 中）

#### 5. dga_data_categories (数据分类)
- 支持层级化的数据目录分类
- 用于组织和管理数据资产

#### 6. dga_table_category_mapping (表分类映射)
- 将表映射到分类目录
- 支持多对多关系

#### 7. dga_datamap_permissions (数据地图权限)
- 细粒度的数据访问控制
- 支持权限过期时间

#### 8. dga_data_lineage (数据血缘)
- 追踪数据流转关系
- 记录ETL、视图等转换逻辑

#### 9. dga_search_recommendations (搜索推荐)
- AI搜索推荐记录
- 基于用户行为的智能推荐

## 技术栈

### 前端
- **框架**: Vue 2.7.14
- **UI组件库**: Ant Design Vue 1.7.8
- **构建工具**: Vite 5.0.0
- **路由**: Vue Router 3.6.5
- **HTTP客户端**: Axios 1.6.0

### 后端
- **框架**: Spring Boot 2.7.18
- **语言**: Java 8
- **ORM**: Spring Data JPA
- **数据库**: MySQL 8.0

## 部署说明

### 前端部署
```bash
cd dga-frontend
npm install
npm run dev      # 开发环境
npm run build    # 生产构建
```

### 后端部署
```bash
cd dga-backend
mvn clean install
mvn spring-boot:run
```

### 数据库初始化
```bash
# 1. 创建数据库
CREATE DATABASE dga_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 执行主表结构
mysql -u root -p dga_platform < src/main/resources/schema.sql

# 3. 执行数据地图扩展表结构
mysql -u root -p dga_platform < src/main/resources/schema-datamap.sql
```

## 使用流程

### 用户搜索流程
1. 用户在搜索框输入关键字
2. 选择数据源类型（可选）
3. 切换AI搜索模式（可选）
4. 点击搜索或按Enter键
5. 系统记录搜索历史
6. 跳转到元数据页面展示结果

### 浏览记录流程
1. 用户访问某个数据表详情页
2. 系统自动记录到 `dga_user_recent_views`
3. 数据地图页面展示最近10条浏览记录
4. 用户可快速点击访问历史记录

### 统计数据更新流程
1. 后台定时任务计算各项统计指标
2. 将结果缓存到 `dga_datamap_stats_cache`
3. API接口从缓存表读取数据返回
4. 用户点击刷新按钮重新计算

## 未来增强

### 短期规划
1. **高级搜索**: 支持多条件组合查询、正则表达式搜索
2. **收藏功能**: 允许用户收藏常用数据表和数据源
3. **搜索推荐**: 基于历史行为的智能推荐
4. **搜索热词**: 展示平台热门搜索关键词

### 长期规划
1. **知识图谱**: 构建数据资产知识图谱，可视化展示数据关系
2. **血缘分析**: 完整的数据血缘追踪和影响分析
3. **质量评分**: 为数据表打分，标识数据质量等级
4. **使用分析**: 数据表的访问频率、热度分析
5. **智能标签**: 自动为数据表打标签并分类

## 相关文档
- [数据源管理](./docs/datasource-management.md)
- [元数据管理](./docs/metadata-management.md)
- [权限管理](./docs/access-management.md)
- [数据质量](./docs/data-quality.md)

## 联系方式
如有问题请联系开发团队或提交Issue。
