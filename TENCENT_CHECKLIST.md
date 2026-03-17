# 腾讯云部署检查清单

## 部署前准备

### 1. 购买服务器
- [ ] 选择地域（建议：广州/上海/北京）
- [ ] 选择配置（推荐：8核16GB，最低：4核8GB）
- [ ] 选择系统：Ubuntu 22.04 LTS
- [ ] 配置数据盘：50-100GB
- [ ] 配置带宽：3-5Mbps

### 2. 安全组配置
在腾讯云控制台配置安全组规则：

| 端口 | 协议 | 说明 | 是否必须 |
|------|------|------|----------|
| 22 | TCP | SSH 登录 | 必须 |
| 8080 | TCP | 应用网关 | 必须 |
| 8848 | TCP | Nacos 控制台 | 可选 |
| 15672 | TCP | RabbitMQ 管理界面 | 可选 |
| 9001 | TCP | MinIO 控制台 | 可选 |

### 3. 域名配置（可选）
- [ ] 购买域名
- [ ] 配置 DNS 解析到服务器 IP
- [ ] 备案（如需要）

## 部署步骤

### 快速部署（推荐）

```bash
# 1. SSH 登录服务器
ssh ubuntu@your-server-ip

# 2. 上传项目代码
# 方式1：使用 Git
git clone <your-repo-url> /opt/lab-system
cd /opt/lab-system

# 方式2：使用 SCP 上传
# 在本地执行：scp -r ./smart-lab-management-system ubuntu@your-server-ip:/opt/lab-system

# 3. 运行一键部署脚本
cd /opt/lab-system
sudo bash deploy-tencent.sh
```

### 手动部署

参考 `TENCENT_CLOUD_DEPLOYMENT.md` 文档。

## 部署后检查

### 1. 检查 Docker 服务
```bash
docker-compose ps
```
应该看到 5 个服务都是 Up 状态。

### 2. 检查应用服务
```bash
/opt/lab-deploy/status.sh
```
应该看到 5 个服务都在运行。

### 3. 测试访问
```bash
# 测试网关
curl http://localhost:8080/actuator/health

# 测试 Nacos
curl http://localhost:8848/nacos
```

### 4. 浏览器访问
- [ ] 网关：http://your-server-ip:8080
- [ ] Nacos：http://your-server-ip:8848/nacos
- [ ] RabbitMQ：http://your-server-ip:15672
- [ ] MinIO：http://your-server-ip:9001

## 常见问题

### 服务启动失败
```bash
# 查看日志
tail -f /opt/lab-deploy/gateway.log

# 检查端口占用
netstat -tlnp | grep 8080

# 检查 Nacos 连接
curl http://localhost:8848/nacos/v1/ns/operator/metrics
```

### 内存不足
```bash
# 查看内存使用
free -h

# 减少 JVM 内存（编辑启动脚本）
nano /opt/lab-deploy/start-all.sh
# 修改：JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 重启服务
/opt/lab-deploy/stop-all.sh
/opt/lab-deploy/start-all.sh
```

### 无法访问
1. 检查安全组是否开放端口
2. 检查防火墙：`sudo ufw status`
3. 检查服务是否运行：`/opt/lab-deploy/status.sh`

## 日常维护

### 查看日志
```bash
# 应用日志
tail -f /opt/lab-deploy/gateway.log
tail -f /opt/lab-deploy/user.log

# Docker 日志
docker logs -f lab-mysql
docker logs -f lab-nacos
```

### 重启服务
```bash
# 重启所有服务
/opt/lab-deploy/stop-all.sh
/opt/lab-deploy/start-all.sh

# 重启 Docker 服务
docker-compose restart
```

### 数据备份
```bash
# 创建备份目录
mkdir -p /opt/backup

# 备份数据库
docker exec lab-mysql mysqldump -uroot -proot lab_management > /opt/backup/lab_$(date +%Y%m%d).sql

# 设置定时备份
crontab -e
# 添加：0 2 * * * docker exec lab-mysql mysqldump -uroot -proot lab_management > /opt/backup/lab_$(date +\%Y\%m\%d).sql
```

### 更新代码
```bash
# 1. 停止服务
/opt/lab-deploy/stop-all.sh

# 2. 更新代码
cd /opt/lab-system
git pull

# 3. 重新编译
mvn clean package -DskipTests

# 4. 启动服务
/opt/lab-deploy/start-all.sh
```

## 成本优化建议

### 测试阶段
- 使用按量付费，测试完关机
- 选择 4核8GB 配置
- 带宽选择 3Mbps
- 预计：¥10-20/天

### 长期运行
- 购买包年包月，有折扣
- 根据实际负载调整配置
- 使用 CDN 减少带宽成本
- 预计：¥250-500/月

## 安全加固（生产环境必做）

- [ ] 修改 MySQL root 密码
- [ ] 修改 Redis 密码（如需要）
- [ ] 修改 RabbitMQ 密码
- [ ] 修改 MinIO 密码
- [ ] 修改 Nacos 密码
- [ ] 配置 HTTPS（使用 Let's Encrypt）
- [ ] 限制 Nacos/RabbitMQ/MinIO 仅内网访问
- [ ] 配置防火墙规则
- [ ] 定期更新系统和依赖
