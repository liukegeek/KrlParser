# KRLParser

> KRLParser 是一个面向 KUKA 机器人的程序调用关系分析工具。用于从机器人备份中提取模块信息、从程序文件中生成对应的 AST，并针对 `CELL 程序` -> `大车型程序` -> `车型程序` -> `轨迹程序` 这种层级调用模式（常见于柔性化、多车型的汽车自动生产线）来构建调用链图谱，并导出可归档的 Excel 结果。

## 1 项目简介

​	KUKA 是一种主流的六轴工业机器人，用户可以通过编写程序来实现对 KUKA 机器人的运动控制、数据运算、信号交互。编写这类可被 KUKA 执行的程序所使用的语言就是 KRL（KUKA Robot Language）。

​	由于柔性化生产的需要，在一个汽车部件焊接线中，一个流水线工位上往往承担多款车型制造。同品牌不同改款的车型往往大同小异，这就会导致对于机器人当中，有的程序模块可能会被多个车型所共用，有的程序模块又可能是仅仅单个车型所独占使用。

​	当前业内一种主流的车型管理方式是：`CELL 程序` -> `大车型程序` -> `车型程序` -> `轨迹程序` 模式。上游控制设备（如 PLC）会将一种车型映射成一个唯一的车型代码（比如 132、142、145 等），然后将车型代码拆分成两部分：大车型代码 + 小车型代码。比如 `车型代码:132` 转换成 `大车型代码:12` 与 `小车型代码:3`，`142` 转换成 `大车型代码:12` 与 `小车型代码:4`。这么做是为了更细粒度地控制程序共用，并尽可能直观地传递出**相似车型调用相同程序**这一信息。比如从 `132、142、145` 中可以看出，`132、142` 由于大车型代码一致，因此会有更多工位上的机器人能够共用程序。

​	这种模式下，KUKA 机器人的自动工作流程是：

1. 首先运行 `CELL` 程序，根据上游发送来的大车型代码，进入对应的 `大车型程序`（也称主车型程序、P 程序等）
2. 在 `大车型程序` 中，根据上游发送来的小车型代码，进入对应的 `车型程序`（此时大、小车型代码确定，故而车型代码能够确定）
3. `车型程序` 中，会控制机器人执行该车型的完整工作周期。根据工艺不同，会有焊接、搬运、空中补焊、涂胶等工作。车型程序会根据上游设备对应的时序与信号接收情况，按照预期依次进入具体的 `轨迹程序` 当中，并在最后进行 `所有工作完成` 信号的握手传递。
4. `轨迹程序` 用于控制该机器人在进行具体工作时的详细运动形式，例如运动轨迹、焊接通电、气缸动作等等。

​	该项目针对上述工作流程，从 KUKA 机器人备份程序中自动化分析各流程阶段的机器人共用模块、独占模块等，并可视化展现整条调用链路，且能将结果以 Excel 表格的形式导出。此外，也能够借助该程序查看机器人本体信息、内部运行模块的属性等细节。

- 程序自动解析备份，并生成反映 KUKA 机器人内部程序调用关系的树形图。

![image-20260517073658977](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F17%2F073700-423d254157c2809ced893465b2e46bad.webp)

- 当点击选中某节点时，可以高亮该节点所在的调用链路，从而更直观体现调用关系。

![image-20260517073727860](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F17%2F073729-3a0d29a0fd14296957c652e2dcda1ad3.webp)

它同时支持桌面本地分析和服务器部署后的异步任务分析，适合现场排查、程序结构梳理、批量备份审查和结果留档等场景。

## 2 核心能力

- 解析 `*.zip` 备份包，遍历程序模块（KUKA Module）并构建 KRL AST
- 支持多个备份包批量分析，按机器人汇总结果

![](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F19%2F000508-77e19f9a90cc16c9d0841e325f5e3408.webp)

- 提供线体视图与车型视图，支持图谱/列表双视图切换

![image-20260519000922275](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F19%2F000924-45598272c09b397378813e7defd27be4.webp)

- 支持节点链路高亮、文件属性查看和上下文信息查看

![image-20260519002132805](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F19%2F002134-774a34c321776d748d4edb99aa80ab1b.webp)

![image-20260529162820827](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F29%2F162824-998420fa77f4e0bc9d954dbc24479d15.webp)

- 支持在线读取与编辑本次分析使用的 `Config.yml`，根据需求过滤分析噪音。

![image-20260519000604191](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F19%2F000606-cbe35cd3f4ba682086f025e18ec7e227.webp)

- 支持导出 Excel，每个机器人一个 Sheet，包含调用树与关系矩阵

![image-20260519001316993](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F19%2F001319-0385990fbe74230832b3dab915f889bf.webp)

- 服务器模式支持异步任务、结果落盘与过期清理

## 3 技术栈

- `ANTLR4`：依据提供的 KRL 词法、语法规则，对机器人运行程序进行词法分析、语法分析。
- `Spring Boot`：将核心功能封装为可供前端界面调用的本地/服务器 API。
- `Cytoscape.js + Dagre`：实现调用关系可视化，调用链路的高亮展示。
- `Apache POI`：将调用数据导出为 Excel 表格并持久化保存。

## 4 运行模式

KRLParser 支持本地桌面模式与服务器模式，前端会通过 `GET /api/runtime/status` 自动识别当前模式。

| 模式 | 适用场景 | 优势 | 劣势 |
| --- | --- | --- | --- |
| `desktop` | 本机分析、现场快速排查、无网络环境 | 本机处理，适合现场即时分析 | 需要在桌面设备上安装或从源码运行 |
| `server` | 云服务器部署、多人共享使用 | 统一入口，便于多人共享 | 受服务器配置影响，长任务耗时可能较长 |

- 默认运行模式为 `desktop`
- 设置 `KRL_RUNTIME_MODE=server` 后，运行模式会切换为 `server`

## 5 快速开始

### (1) 下载软件直接运行

​	本项目提供开箱即用的使用体验，可在 GitHub Releases 页面根据操作系统下载对应版本后运行。

### (2) 通过代码编译运行

- 环境要求

  - `JDK 21`

  - `Maven 3.9+`


- 本地桌面模式

```bash
mvn -pl krl-web -am spring-boot:run
```

桌面模式会自动打开浏览器；若未自动打开，请访问启动日志中显示的本地地址。

- 本地验证服务器模式

```bash
KRL_RUNTIME_MODE=server mvn -pl krl-web -am spring-boot:run
```

如需自定义监听地址或端口，可通过 `SERVER_ADDRESS` 与 `SERVER_PORT` 覆盖默认值。

### (3) Docker / 服务器部署

支持两种方式：

- 从 GitHub Releases 下载 Linux Docker 镜像资产并直接部署
- 在本地从源码构建 Docker 镜像后部署

完整部署步骤见 [deploy/README-server.md](deploy/README-server.md)。

## 6 使用流程

1. 打开 `操作菜单`，点击 `上传备份 (.zip)`，可一次选择 1~N 个备份包
2. 点击 `Config` 查看或编辑本次分析所使用的配置
3. 点击 `开始分析` 发起解析
4. 在线体视图中查看机器人节点，并进入对应车型视图
5. 在图谱视图或列表视图中查看调用关系
6. 需要留档时点击 `下载Excel` 导出本次结果
7. 桌面模式使用完毕后，可点击 `退出程序` 关闭后台 Java 进程

![image-20260521000502128](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F21%2F000504-257f582ad5aadd00c312b12f33fe6320.webp)

交互补充：

- 单击节点可高亮链路
- 双击节点可查看文件属性
- 右键可查看相关内容
- 移动端长按节点也可查看文件属性

## 7 配置说明

### 默认路径

- 配置文件：`~/.KrlParser/Config.yml`
- 日志目录：`~/.KrlParser/logs/`
- 临时目录：`~/.KrlParser/tmp/`
- 结果目录：`~/.KrlParser/results/`

### 自动初始化

- 启动时会检查配置文件是否存在
- 若目标路径不存在配置文件，则自动从 `krl-core/src/main/resources/config.yml` 复制生成
- 桌面模式可在 Config 编辑器中点击 `初始化配置`，将磁盘配置恢复为应用内置默认内容；服务器模式不提供该操作

### 规则语义

- 规则数组包括 `prefix` / `suffix`
- `!xxx`：忽略该规则命中的内容
- `xxx`：保留该规则命中的内容
- 匹配时不区分大小写。

### 配置项介绍

- `robotInfo.filePath`：机器人信息 INI 文件路径
- `fileLoadSection`：备份中的文件加载过滤的规则
- `carInvokerParseSection`：调用关系解析时的过滤规则

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

## 8 Excel 导出说明

一次分析导出一个 `.xlsx` 文件，每个机器人对应一个 Sheet。

- 树形调用结构

  - 列顺序固定为 `Cell程序 | P程序 | 车型代码 | 车型程序 | 轨迹程序`

  - 同层相邻重复值纵向合并

  - 不同类型使用固定颜色填充


- 调用关系矩阵

  - 行表示调用方，列表示被调用方

  - 若存在直接调用，在交叉单元格中填入调用方名称

  - 同列顶部到首次直调单元格之间使用 `↑` 指示上溯关系

![image-20260521000736998](https://obj.waitforu.tech/imgs%2Fblog%2F2026%2F05%2F21%2F000738-dd63e5be538a2efe04a3b971f30d37f1.webp)


## 9 项目结构

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
│   ├── .env.example
│   ├── docker-compose.image.yml
│   ├── docker-compose.server.yml
│   ├── Caddyfile.krl.example
│   └── README-server.md
└── README.md
```

## 10 相关文档

- [服务器部署说明](deploy/README-server.md)
- [Release 镜像版 Compose](deploy/docker-compose.image.yml)
- [Docker Compose 部署文件](deploy/docker-compose.server.yml)
- [Caddy 反向代理示例](deploy/Caddyfile.krl.example)

## 11 常见问题

1. 为什么“下载Excel”按钮不可点击？

按钮启用依赖两个条件：

- 已选择至少一个 zip 文件
- 配置已成功加载

如果按钮仍为灰色，请依次检查：

- 后端服务是否正常启动
- `/api/config` 是否返回成功
- 浏览器是否还在使用旧版静态资源，必要时强制刷新缓存
