# DGA Platform (Data Governance Authority)

这是一个基于 **Spring Boot** + **Vue 2** 的前后端分离架构项目，专为数据治理平台设计。

## 项目结构

*   `dga-backend`: 后端项目 (Spring Boot 2.7.x, Java 8+)
*   `dga-frontend`: 前端项目 (Vue 2.7.x + Vite)

## 快速开始

### 1. 启动后端 (dga-backend)

后端服务负责处理业务逻辑和数据接口。

```bash
cd dga-backend
mvn spring-boot:run
```

*   **服务地址**: `http://localhost:8080`
*   **健康检查**: `http://localhost:8080/api/health`

### 2. 启动前端 (dga-frontend)

前端服务提供用户界面，并通过代理与后端通信。

```bash
cd dga-frontend
npm install
npm run dev
```

*   **访问地址**: `http://localhost:3000` (默认)
*   **后端联调**: 前端已配置 Vite 代理，发送到 `/api` 的请求会自动转发到本地后端 8080 端口。

## 架构说明

### 后端 (Spring Boot)
*   **版本**: 2.7.18 (支持 Java 8/11/17)
*   **接口规范**: RESTful API
*   **跨域处理**: 已配置 `@CrossOrigin` 和 Vite Proxy 双重保障。

### 前端 (Vue 2)
*   **构建工具**: Vite (比 Webpack 更快)
*   **框架**: Vue 2.7 (兼容 Vue 3 特性)
*   **HTTP 客户端**: Axios
*   **UI 组件**: 建议集成 Ant Design Vue (1.x 版本适配 Vue 2)。

## 联调测试

1. 启动后端。
2. 启动前端。
3. 打开浏览器访问前端页面。
4. 点击页面上的 **"Check Backend Health"** 按钮。
5. 如果显示 "DGA Backend is running successfully!"，则说明前后端连通正常。
