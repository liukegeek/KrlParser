# KRLParser

> KUKA 机器人备份调用关系分析工具

## 项目简介

KRLParser 是一个面向 KUKA 机器人备份包的调用关系分析工具，用于从 KRL 程序中提取模块关系、构建调用链图谱，并导出可归档的 Excel 结果。

它同时支持桌面本地分析和服务器部署后的异步任务分析，适合现场排查、程序结构梳理、批量备份审查和结果留档等场景。

## 核心能力

- 解析 `*.zip` 备份包，遍历模块并构建 KRL AST
- 支持 1~N 个备份包批量分析，按机器人汇总结果
- 提供线体视图与车型视图，支持图谱/列表双视图切换
- 支持节点高亮、文件属性查看和上下文信息查看
- 支持在线读取与编辑本次分析使用的 `Config.yml`
- 支持导出 Excel，每个机器人一个 Sheet，包含调用树与关系矩阵
- 服务器模式支持异步任务、结果落盘与过期清理

## 技术栈

- `ANTLR4`：KRL 词法与语法解析
- `Spring Boot`：本地/服务器运行与 API 提供
- `Cytoscape.js + Dagre`：调用关系可视化
- `Apache POI`：Excel 导出

## 运行模式

KRLParser 支持同一套代码在两种运行形态下工作，前端会通过 `GET /api/runtime/status` 自动识别当前模式。

| 模式 | 适用场景 | 分析链路 | 结果特征 |
| --- | --- | --- | --- |
| `desktop` | 本机单人分析、现场快速排查 | 同步分析 | 请求完成后直接返回结果 |
| `server` | 云服务器部署、多人共享使用 | 异步任务分析 | 结果落盘，可轮询状态并下载 Excel |

- 默认运行模式为 `desktop`
- 设置 `KRL_RUNTIME_MODE=server` 后，前端会切换到任务化接口
- 服务器模式默认不启用登录认证，对公网开放时请通过网关、内网或反向代理策略控制入口

## 快速开始

### 环境要求

- `JDK 21`
- `Maven 3.9+`

### 本地桌面模式

```bash
mvn -pl krl-web spring-boot:run
```

默认访问地址：`http://localhost:2026`

### 本地验证服务器模式

```bash
KRL_RUNTIME_MODE=server mvn -pl krl-web spring-boot:run
```

### 打包

```bash
mvn clean package
```

### Docker / 服务器部署

完整部署步骤见 [deploy/README-server.md](deploy/README-server.md)。

## 使用流程

1. 点击 `上传备份 (.zip)`，可一次选择 1~N 个备份包
2. 点击 `Config` 查看或编辑本次分析所使用的配置
3. 点击 `开始分析` 发起解析
4. 在线体视图中查看机器人节点，并进入对应车型视图
5. 在图谱视图或列表视图中查看调用关系
6. 需要留档时点击 `下载Excel` 导出本次结果

交互补充：

- 单击节点可高亮链路
- 双击节点可查看文件属性
- 右键可查看相关上下文
- 移动端长按节点也可查看文件属性

## 配置说明

### 默认路径

- 配置文件：`~/.KrlParser/Config.yml`
- 日志目录：`~/.KrlParser/logs/`
- 临时目录：`~/.KrlParser/tmp/`
- 结果目录：`~/.KrlParser/results/`

### 自动初始化

- 启动时会检查配置文件是否存在
- 若目标路径不存在配置文件，则自动从 `krl-core/src/main/resources/config.yml` 复制生成

### 规则语义

- 规则数组包括 `prefix` / `suffix`
- `!xxx`：忽略该规则命中的内容
- `xxx`：保留该规则命中的内容
- `@SKIP@`：跳过该条规则
- 匹配大小写不敏感

### 常用配置项

- `robotInfo.filePath`：机器人信息 INI 文件路径
- `fileLoadSection`：备份文件加载过滤规则
- `carInvokerParseSection`：调用解析过滤规则

### 常用环境变量

- `KRL_RUNTIME_MODE`：运行模式，`desktop` 或 `server`
- `KRL_CONFIG_PATH`：自定义配置文件路径
- `KRL_LOG_DIR`：自定义日志目录
- `KRL_STORAGE_TEMP_DIR`：服务器模式临时文件目录
- `KRL_STORAGE_RESULT_DIR`：服务器模式结果目录
- `KRL_TASK_RETENTION`：任务结果保留时长
- `KRL_CLEANUP_INTERVAL`：过期任务清理间隔
- `KRL_MAX_CONCURRENT_TASKS`：服务器模式最大并发任务数
- `KRL_MAX_ACTIVE_TASKS`：服务器模式最大活动任务数
- `KRL_MAX_FILE_SIZE` / `KRL_MAX_REQUEST_SIZE`：上传体积限制
- `SERVER_ADDRESS` / `SERVER_PORT`：服务监听地址与端口

## API 概览

所有分析接口均使用 `multipart/form-data`，上传字段支持：

- `files`：多个 zip 文件，推荐使用
- `file`：单个 zip 文件，兼容旧前端
- `configText`：前端在线编辑后的 YAML 文本

### 运行状态

- `GET /api/runtime/status`
- 返回当前 `runtimeMode` 与 `analysisMode`

### 配置读取

- `GET /api/config`
- 返回配置文件路径与当前内容

### 同步分析接口

适用于 `desktop` 模式：

- `POST /api/analysis`
- `POST /api/analysis/excel`

### 异步任务接口

适用于 `server` 模式：

- `POST /api/analysis/tasks`：提交任务
- `GET /api/analysis/tasks/{taskId}`：查询任务状态
- `GET /api/analysis/tasks/{taskId}/result`：获取 JSON 结果
- `GET /api/analysis/tasks/{taskId}/excel`：下载 Excel 结果

### 健康检查

- `GET /actuator/health`

## Excel 导出说明

一次分析导出一个 `.xlsx` 文件，每个机器人对应一个 Sheet。

### 树形调用结构

- 列顺序固定为 `Cell程序 | P程序 | 车型代码 | 车型程序 | 轨迹程序`
- 同层相邻重复值纵向合并
- 不同类型使用固定颜色填充

### 调用关系矩阵

- 行表示调用方，列表示被调用方
- 若存在直接调用，在交叉单元格中填入调用方名称
- 同列顶部到首次直调单元格之间使用 `↑` 指示上溯关系

## 项目结构

```text
KRLParser/
├── krl-core/
│   ├── src/main/java/tech/waitforu/
│   │   ├── loader/      # zip/yaml 读取
│   │   ├── parser/      # AST 构建与调用关系分析
│   │   ├── rule/        # 过滤规则
│   │   └── service/     # 分析与 Excel 导出核心服务
│   └── src/main/resources/
│       ├── krl.g4
│       └── config.yml
├── krl-web/
│   └── src/main/
│       ├── java/tech/waitforu/krlweb/
│       │   ├── controller/  # runtime / analysis / task APIs
│       │   ├── config/      # 运行模式与存储配置
│       │   └── service/     # 同步执行与异步任务服务
│       └── resources/
│           ├── static/      # index.html / js / css / vendor
│           └── application.yml
├── deploy/
│   ├── docker-compose.server.yml
│   ├── Caddyfile.krl.example
│   └── README-server.md
└── README.md
```

## 相关文档

- [服务器部署说明](deploy/README-server.md)
- [Docker Compose 部署文件](deploy/docker-compose.server.yml)
- [Caddy 反向代理示例](deploy/Caddyfile.krl.example)

## 常见问题

### 为什么“下载Excel”按钮不可点击？

按钮启用依赖两个条件：

- 已选择至少一个 zip 文件
- 配置已成功加载

如果按钮仍为灰色，请依次检查：

- 后端服务是否正常启动
- `/api/config` 是否返回成功
- 浏览器是否还在使用旧版静态资源，必要时强制刷新缓存

### 为什么界面上会出现任务状态标签？

当应用运行在 `server` 模式时，前端会自动切换到异步任务链路，并显示运行模式与任务状态标签。
