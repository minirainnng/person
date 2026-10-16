# 部署指南：把项目发布到公网

本文档记录从本地项目到「任何人都能通过链接打开」需要做的所有事情。
按顺序执行，每一步都不依赖下一步。

## 0. 当前项目结构

```
青哥带小白做毕设2026项目脚手架/
├── .gitignore                  ← 已创建（忽略敏感配置、依赖、产物）
├── DEPLOY.md                   ← 本文档
└── code2026/
    ├── vue/                    ← 前端 Vue 3 + Vite
    │   ├── .env.development    ← 本地开发用
    │   ├── .env.production     ← 生产用（部署前改成真实后端 URL）
    │   └── .env.production.example  ← 模板
    └── springboot/             ← 后端 Spring Boot
        └── src/main/resources/
            ├── application.yml        ← 默认（开发用）
            └── application-prod.yml.example  ← 生产模板
```

## 1. 准备：本地能跑起来

> 这一步不是必须的，但强烈建议在推送前先确认代码能跑通。

### 1.1 后端

要求：JDK 17+、Maven 3.8+、MySQL 8.0+、本地有 3305 端口

```powershell
# 进入后端目录
cd code2026/springboot

# 启动 MySQL（如果用 XAMPP / phpStudy / Navicat 启动都行）
# 创建数据库
mysql -u root -p
> CREATE DATABASE IF NOT EXISTS person DEFAULT CHARSET utf8mb4;

# 跑起来
mvn spring-boot:run
```

跑通后访问 `http://localhost:9090/` 看到 `{"code":"200"}` 就 OK。

### 1.2 前端

要求：Node 18+

```powershell
cd code2026/vue
npm install
npm run dev
```

浏览器打开提示的端口（一般是 5173），看到登录页就 OK。

## 2. 把代码推送到 GitHub

### 2.1 注册 GitHub 账号

去 https://github.com 注册（已有就跳过）。

### 2.2 创建仓库

- 登录后点右上角 `+` → `New repository`
- Repository name 填 `memories-app`（或任何名字）
- 选 **Public**（Private 部署要收费）
- **不要**勾选 `Add a README`（我们要推本地代码）
- 点 `Create repository`

### 2.3 在本地初始化 git 并推送

在 PowerShell 里执行（把 `你的用户名` 换成你的 GitHub 用户名）：

```powershell
cd f:\easy\青哥带小白做毕设2026项目脚手架
git init
git add .
git status                              # 检查 .gitignore 是否生效（不要看到 node_modules / target）
git commit -m "chore: initial commit"
git branch -M main
git remote add origin https://github.com/你的用户名/memories-app.git
git push -u origin main
```

## 3. 部署前端（Vercel，免费）

### 3.1 注册 Vercel

- 打开 https://vercel.com
- 用 GitHub 账号登录
- 授权 Vercel 访问你的 GitHub 仓库

### 3.2 导入项目

- 主页点 `Add New...` → `Project`
- 选 `memories-app` 仓库 → `Import`
- **关键配置**：
  - **Root Directory**: 点 `Edit` 改成 `code2026/vue`
  - **Framework Preset**: 自动识别为 Vite
  - **Build Command**: `npm run build`（默认）
  - **Output Directory**: `dist`（默认）
- 点 `Deploy`

### 3.3 配置环境变量

部署完成后：

- 进入项目 → `Settings` → `Environment Variables`
- 添加：
  - `VITE_BASE_URL` = `https://你的后端URL`（先填占位，等后端部署完再改）
- 保存后 `Deployments` → 最新一次部署右侧三点 → `Redeploy`

部署成功后 Vercel 会给一个 `xxx.vercel.app` 链接，**任何人都能打开**。

## 4. 部署后端（Railway，$5/月含 MySQL）

### 4.1 注册 Railway

- 打开 https://railway.app
- 用 GitHub 账号登录
- 新用户有 $5 免费额度（够跑 1 个月）

### 4.2 创建 MySQL

- 主页 `New Project` → `Provision MySQL`
- 等几十秒创建完成
- 点 MySQL 服务 → `Variables` 标签，记下：
  - `MYSQL_HOST`
  - `MYSQL_PORT`
  - `MYSQL_DATABASE`
  - `MYSQL_USER`
  - `MYSQL_PASSWORD`（点 `Reveal` 才看得到）

### 4.3 部署后端服务

- 同一个 Project → `New` → `GitHub Repo`
- 选你的 `memories-app` 仓库
- **关键配置**：
  - **Root Directory**: 改成 `code2026/springboot`
  - 部署会自动跑 `mvn package`，生成 jar
- 等构建完成，Railway 会给一个 `xxx.up.railway.app` 链接

### 4.4 配置环境变量

进入后端服务 → `Variables`，添加：

| 变量名 | 值 | 说明 |
|---|---|---|
| `DB_HOST` | (从 MySQL 复制) | 例如 `containers-us-west-xxx.railway.app` |
| `DB_PORT` | (从 MySQL 复制) | Railway 的 MySQL 端口是 `6541` 之类 |
| `DB_NAME` | (从 MySQL 复制) | 数据库名 |
| `DB_USERNAME` | (从 MySQL 复制) | 用户名 |
| `DB_PASSWORD` | (从 MySQL 复制) | 密码 |
| `DEEPSEEK_ENABLED` | `false` | 暂不开启 AI 标签 |
| `DEEPSEEK_API_KEY` | 留空 | 暂不开启 |
| `FILE_BASE_URL` | 你的后端 URL | 例如 `https://xxx.up.railway.app` |

点 MySQL 服务，把库初始化（上传建表 SQL）：

- 进入 MySQL 服务 → `Data` 标签
- 找到 `Connect` 按钮，复制连接命令
- 在本地用 `mysql -h ... -u ... -p` 跑一下，把你建表 SQL 灌进去

### 4.5 验证后端

浏览器打开 `https://你的后端URL.up.railway.app/`
看到 `{"code":"200"}` 就 OK。

## 5. 把前后端连起来

回到 Vercel：

- `Settings` → `Environment Variables`
- 把 `VITE_BASE_URL` 改成 `https://你的后端URL.up.railway.app`
- `Deployments` → 最新一次 → `Redeploy`

## 6. 打开链接

现在 Vercel 给你的 `xxx.vercel.app` 链接就是你的应用公网地址。

发给你朋友试试：他们能直接打开 → 注册 → 上传记忆 → 看到自己存的内容。

## 7. 常见问题

### 7.1 CORS 跨域报错

打开 `code2026/springboot/src/main/java/com/example/common/CorsConfig.java`，
确认允许的 origin 包含你的 Vercel 域名（`*.vercel.app`）。

### 7.2 上传的文件丢失

Railway 重启会清空本地文件系统。
**生产环境必须用对象存储**（OSS / S3 / Railway Volume），
**最小成本方案**：改用 Cloudflare R2（10GB 免费）。

### 7.3 Railway 免费额度用完

可以用 [Render](https://render.com) 替代，免费层更宽松（但会休眠）。

## 8. 部署后的下一步（功能完善）

部署成功后，再考虑加：

1. **多人隔离**：数据库加 `user_id` 字段
2. **Token 鉴权**：登录后返回 token，拦截器校验
3. **文件存储**：换成 R2 / OSS
4. **HTTPS**：Vercel 和 Railway 都自动给，不需要管

具体每个改动的实施步骤都会单独出文档。
