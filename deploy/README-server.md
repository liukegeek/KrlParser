# KRL Parser 部署说明

KRL Parser 支持以服务器模式运行，推荐两种 Docker 部署方式：

- 方式 A：从 GitHub Release 下载 Docker 镜像资产后直接部署
- 方式 B：从仓库源码本地构建镜像后部署

当前服务默认不启用登录认证。若需要公网访问，请务必通过反向代理、网关、内网或访问控制策略保护入口。

## 1. 准备环境变量

在 `deploy/` 目录下复制一份环境变量模板：

```bash
cp deploy/.env.example deploy/.env
```

如需调整镜像名、对外端口、任务保留时长、上传体积或并发度，可直接修改 `deploy/.env` 中对应字段。

## 2. 方式 A：从 Release 资产部署

### 2.1 下载镜像资产

在 GitHub Releases 页面下载对应版本的 Linux 镜像资产：

- `KRLParser-<version>-linux-amd64-image.tar.gz`

### 2.2 导入镜像

在目标机器执行：

```bash
gunzip -c KRLParser-<version>-linux-amd64-image.tar.gz | docker load
```

导入完成后，镜像默认包含以下标签：

- `krlparser:<version>`
- `krlparser:latest`

若希望固定使用某个版本，可在 `deploy/.env` 中显式设置：

```bash
KRL_IMAGE=krlparser:<version>
```

### 2.3 启动服务

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.image.yml up -d
```

## 3. 方式 B：从源码构建镜像后部署

如果你不想依赖 Release 资产，也可以直接在仓库根目录构建并启动：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

## 4. 访问地址

容器内监听端口由应用配置决定，宿主机映射端口由 `KRL_PUBLIC_PORT` 控制。

默认访问地址：

```text
http://localhost:<KRL_PUBLIC_PORT>
```

## 5. 数据目录说明

`deploy/data/` 将映射到容器内 `/app/data/`，其中包含：

- `Config.yml`
- `logs/`
- `tmp/`
- `results/`

## 6. 可选：接入 Caddy

如需对外提供域名入口，可参考 `deploy/Caddyfile.krl.example`，将请求反向代理到本机实际监听端口。

示例：

```caddy
<your-domain> {
    reverse_proxy 127.0.0.1:<your-port>
}
```

## 7. 健康检查

应用提供标准健康检查地址：

```text
/actuator/health
```

若服务已映射到本机端口 `<KRL_PUBLIC_PORT>`，则完整地址为：

```text
http://localhost:<KRL_PUBLIC_PORT>/actuator/health
```
