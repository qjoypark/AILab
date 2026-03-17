# Nacos 部署与启动说明

## 1) 当前改动目标

- 微服务默认启用 Nacos（不再“真正关闭”）。
- Gateway 默认走服务发现（`lb://`）而不是固定端口。
- 支持本地/测试/生产通过环境变量切换 Nacos 地址和认证信息。
- Windows 启动脚本改为等待 Nacos 健康后再继续。

## 2) 环境变量（推荐）

所有服务统一支持以下变量：

- `NACOS_SERVER_ADDR`（默认：`localhost:8848`）
- `NACOS_NAMESPACE`（默认：空）
- `NACOS_GROUP`（默认：`DEFAULT_GROUP`）
- `NACOS_USERNAME` / `NACOS_PASSWORD`（默认：空）
- `NACOS_DISCOVERY_ENABLED`（默认：`true`）
- `NACOS_CONFIG_ENABLED`（默认：`true`）
- `NACOS_DISCOVERY_FAIL_FAST`（默认：`false`）

## 3) 启动方式

### 本地开发

1. 执行 `start-all.bat` 启动基础设施；
2. 脚本会轮询 `http://localhost:8848/nacos/v1/ns/operator/metrics`；
3. 当返回 `{"status":"UP"}` 后再启动各微服务。

### 生产/正式环境

建议在服务启动参数或环境变量中明确设置：

- `NACOS_SERVER_ADDR=<your-nacos-host>:8848`
- `NACOS_NAMESPACE=<your-namespace>`
- `NACOS_GROUP=<your-group>`
- `NACOS_USERNAME=<your-user>`
- `NACOS_PASSWORD=<your-password>`

## 4) 故障排查

- Nacos 健康检查：
  - `curl http://<nacos-host>:8848/nacos/v1/ns/operator/metrics`
- 查看 Nacos 容器日志：
  - `docker logs -f lab-nacos`
- 端口连通性检查：
  - `8848`（HTTP）
  - `9848`（gRPC）
