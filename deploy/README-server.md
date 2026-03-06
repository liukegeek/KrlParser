# KRL Parser 服务器部署说明

当前仓库已按云服务器模式提供 Docker 部署产物，适用于当前服务器环境：
- Ubuntu 22.04.5
- Docker / Docker Compose 已安装
- Caddy 已运行
- Caddy 当前所在 Docker network: `blog-backend-net`
- Caddy 配置文件路径: `/home/ubuntu/dockerProjects/reverse-proxy/Caddyfile`
- 当前部署默认不启用登录认证，公网访问请自行通过网关、内网或反向代理策略控制入口

## 1. 准备环境变量

在 `deploy/` 目录下复制一份环境变量模板：

```bash
cp .env.example .env
```

如需调整任务保留时长、上传体积或并发度，可直接修改 `.env` 中对应字段。

## 2. 构建并启动服务

在仓库根目录执行：

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.server.yml up -d --build
```

服务启动后，容器名为 `krl-parser`，应用监听容器内端口 `2026`。

## 3. 接入现有 Caddy

将 `deploy/Caddyfile.krl.example` 中的站点配置追加到服务器 Caddyfile：

```caddy
krl.waitforu.tech {
    reverse_proxy krl-parser:2026
}
```

然后重载 Caddy 容器配置。

## 4. 数据目录说明

`deploy/data/` 将映射到容器内 `/app/data/`，其中包含：
- `Config.yml`
- `logs/`
- `tmp/`
- `results/`

## 5. 健康检查

应用提供标准健康检查地址：

```text
/actuator/health
```

如果子域名为 `krl.waitforu.tech`，则完整地址为：

```text
https://krl.waitforu.tech/actuator/health
```
