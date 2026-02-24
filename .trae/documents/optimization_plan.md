# 功能优化计划

## 1. 元数据列表分页 (Metadata Pagination)
**目标**：解决全量加载导致的性能问题，实现前后端分页。
- **后端**：
    - 修改 `MetadataController.getTables` 接口，支持 `page` 和 `size` 参数。
    - 使用 `TableMetadataRepository.findAll(Pageable)` 进行分页查询。
- **前端**：
    - 修改 `Metadata.vue`，在 `a-table` 中启用服务端分页 (`pagination` 配置)。
    - 监听表格翻页事件，调用后端接口获取对应页数据。

## 2. 数据地图统计完善 (DataMap Statistics)
**目标**：替换 Mock 数据，展示真实的系统运行状态。
- **后端**：
    - 修改 `DataMapController.getStats`。
    - `collectorCount`: 统计 `DataSourceConfig` 表中活跃的数据源数量。
    - `storage`: 统计 `TableMetadata` 表中 `totalSize` 的总和。
    - `apiCount`: 统计 Controller 方法数量（可选，或暂时保留为 0）。
- **前端**：
    - 确认 `DataMap.vue` 正确展示后端返回的统计数据。

## 3. 元数据同步状态反馈 (Sync Status Feedback)
**目标**：提供真实的同步进度反馈，避免用户盲目等待。
- **后端**：
    - 引入 `SyncStatusService`，记录同步任务状态 (RUNNING, COMPLETED, FAILED)。
    - 修改 `MetadataController.syncMetadata`，在异步任务开始/结束时更新状态。
    - 新增接口 `GET /api/metadata/sync/status` 获取当前同步状态。
- **前端**：
    - 修改 `Metadata.vue` 的同步按钮逻辑。
    - 点击后进入 Loading 状态，并轮询 `/api/metadata/sync/status`。
    - 根据返回状态更新 UI（成功提示或错误提示）。

## 4. 全局异常处理 (Global Exception Handling)
**目标**：统一后端错误返回格式，提升前端错误处理能力。
- **后端**：
    - 创建 `GlobalExceptionHandler` (@ControllerAdvice)。
    - 定义统一的 `ApiResponse` 结构。
