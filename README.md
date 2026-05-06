# 千语博客

一个前后端分离的个人博客项目，包含文章管理、分类与标签、图片资源库、音乐频道、AI 对话与生图、后台管理等能力。

项目由两个子应用组成：

- `qianyu_blog_backend`：Spring Boot 3 后端
- `qianyu_blog_frontend`：Vue 3 + TypeScript 前端

## 当前功能

### 前台站点

- 首页文章列表
- 文章详情页
- 分类筛选与标签列表
- 归档页
- 关于页
- 光影相册页
- 音乐频道页
- AI 对话与图片生成页

### 后台管理

- 管理员登录，基于 JWT 认证
- 文章管理：新增、编辑、删除、发布状态、置顶
- 分类管理
- 图片资源管理：上传、查询、删除
- 音乐管理：上传音频、更新信息、删除
- AI 设置管理
- AI 辅助润色文章

### 其他能力

- Swagger / OpenAPI 文档
- `schema.sql` 数据库初始化脚本
- 后端基础集成测试

## 已移除模块

当前版本已经移除以下模块：

- 小游戏中心
- 站点配置管理

对应的前端路由、页面、组件，以及后端接口、服务、实体、测试引用都已删除。

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios / Fetch
- Playwright

### 后端

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- MySQL
- H2（测试）
- springdoc-openapi

## 目录结构

```text
qianyu_blog/
├─ README.md
├─ qianyu_blog_backend/
│  ├─ pom.xml
│  ├─ src/main/java/com/study/qianyu_blog/
│  │  ├─ config/
│  │  ├─ controller/
│  │  ├─ dto/
│  │  ├─ entity/
│  │  ├─ exception/
│  │  ├─ repository/
│  │  ├─ security/
│  │  └─ service/
│  ├─ src/main/resources/
│  │  ├─ application.properties
│  │  └─ schema.sql
│  └─ src/test/
└─ qianyu_blog_frontend/
   ├─ package.json
   ├─ public/
   └─ src/
      ├─ api/
      ├─ components/
      ├─ composables/
      ├─ constants/
      ├─ router/
      ├─ stores/
      ├─ types/
      ├─ utils/
      └─ views/
```

## 主要路由

### 前台

- `/`：首页
- `/posts/:id`：文章详情
- `/archive`：归档页
- `/about`：关于页
- `/light`：光影相册
- `/chat`：AI 对话 / AI 生图

### 后台

- `/admin/login`：后台登录
- `/admin/dashboard`：控制台
- `/admin/posts`：文章管理
- `/admin/categories`：分类管理
- `/admin/media`：图片资源管理
- `/admin/music`：音乐管理
- `/admin/ai-settings`：AI 配置

## 核心接口概览

### 公开接口

- `GET /api/public/categories`：分类列表
- `GET /api/public/tags`：标签列表
- `GET /api/public/posts`：文章列表
- `GET /api/public/posts/{id}`：文章详情
- `GET /api/public/media`：媒体列表
- `GET /api/public/media/{id}/file`：媒体文件
- `GET /api/public/music`：音乐列表
- `GET /api/public/music/{id}/file`：音频文件

### 管理接口

- `POST /api/admin/auth/login`：管理员登录
- `GET/POST/PUT/DELETE /api/admin/posts`：文章管理
- `GET/POST/PUT/DELETE /api/admin/categories`：分类管理
- `GET/POST/DELETE /api/admin/media`：图片资源管理
- `POST/PUT/DELETE /api/admin/music`：音乐管理
- `GET/POST /api/admin/ai/settings`：AI 设置
- `POST /api/admin/ai/polish`：AI 润色文章

### AI 接口

- `POST /api/ai/chat/stream`：流式对话
- `POST /api/ai/image`：图片生成

## 运行环境

### 后端

- JDK 17
- Maven 3.9+
- MySQL 8+

### 前端

- Node.js 20+
- npm 10+

## 本地启动

### 1. 初始化数据库

创建 MySQL 数据库并导入脚本：

```sql
SOURCE qianyu_blog_backend/src/main/resources/schema.sql;
```

### 2. 启动后端

```powershell
cd qianyu_blog_backend
mvn spring-boot:run
```

默认地址：

- 应用：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui.html`

### 3. 启动前端

```powershell
cd qianyu_blog_frontend
npm install
npm run dev
```

默认地址：

- 前端：`http://localhost:5173`

## Docker Compose 一键部署

项目已包含以下容器化部署文件：

- `docker-compose.yml`
- `qianyu_blog_backend/Dockerfile`
- `qianyu_blog_frontend/Dockerfile`
- `qianyu_blog_frontend/nginx.conf`

### 启动命令

在项目根目录执行：

```powershell
docker compose up -d --build
```

## Jenkins 部署

如果你的 Jenkins 就跑在目标服务器上，最省事的方式是直接用仓库根目录的 `docker-compose.yml` 部署。

### 服务器前置条件

- 已安装 Docker
- 已安装 Docker Compose Plugin（即 `docker compose` 命令可用）
- Jenkins 运行用户有权限执行 Docker
- 服务器 80 端口未被其他服务占用

### Jenkins 凭据

在 Jenkins 中创建以下 `Secret text` 凭据：

- `qianyu-blog-mysql-root-password`
- `qianyu-blog-db-password`
- `qianyu-blog-jwt-secret`
- `qianyu-blog-admin-username`
- `qianyu-blog-admin-password`

### Jenkins 任务配置

1. 新建一个 Pipeline 任务
2. 在代码仓库地址中填入你的 GitHub 仓库
3. 让 Jenkins 使用仓库根目录的 `Jenkinsfile`
4. 触发构建

### 流水线做的事

- 拉取最新代码
- 在工作区生成 `.env`
- 执行 `docker compose up -d --build --remove-orphans`
- 通过 `http://127.0.0.1:${APP_PORT}/api/public/categories` 做健康检查

### 本地环境文件示例

仓库已提供示例文件：

```env
.env.production.example
```

如果你暂时不想把敏感值放进 Jenkins，也可以先在服务器仓库根目录手动创建 `.env`，内容参考该示例文件。

启动后默认访问：

- 站点首页：`http://localhost`
- Swagger：`http://localhost/swagger-ui.html`

### 服务组成

- `mysql`：MySQL 8.4
- `backend`：Spring Boot 后端
- `frontend`：Nginx 托管前端静态文件，并反向代理 `/api`

### 可配置环境变量

可通过系统环境变量或根目录 `.env` 文件覆盖：

```env
APP_PORT=80
QIANYU_BLOG_MYSQL_ROOT_PASSWORD=change-me-qianyu-blog-mysql-root-password
QIANYU_BLOG_DB_URL=jdbc:mysql://localhost:3306/qianyu_blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
QIANYU_BLOG_DB_USERNAME=root
QIANYU_BLOG_DB_PASSWORD=change-me-db-password
QIANYU_BLOG_JWT_SECRET=change-me-qianyu-blog-jwt-secret-at-least-32-chars
QIANYU_BLOG_ADMIN_USERNAME=change-me-admin-username
QIANYU_BLOG_ADMIN_PASSWORD=change-me-admin-password
```

### 数据持久化

`docker compose` 会创建以下卷：

- `mysql_data`
- `backend_music`
- `backend_media`

分别用于：

- MySQL 数据库持久化
- 音乐上传文件持久化
- 图片上传文件持久化

### 重新初始化

如果你需要清空数据库和卷后重新初始化：

```powershell
docker compose down -v
docker compose up -d --build
```

## 配置说明

后端主要配置位于 `qianyu_blog_backend/src/main/resources/application.properties`。

常用配置项：

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| `QIANYU_BLOG_DB_URL` | MySQL 连接串 | 无默认值 |
| `QIANYU_BLOG_DB_USERNAME` | 数据库用户名 | 无默认值 |
| `QIANYU_BLOG_DB_PASSWORD` | 数据库密码 | 无默认值 |
| `QIANYU_BLOG_SERVER_PORT` | 后端端口 | `8080` |
| `QIANYU_BLOG_JWT_SECRET` | JWT 密钥，至少 32 个字符 | 无默认值 |
| `QIANYU_BLOG_JWT_EXPIRATION_MINUTES` | Token 有效期 | `120` |
| `QIANYU_BLOG_ADMIN_USERNAME` | 初始管理员账号 | 无默认值 |
| `QIANYU_BLOG_ADMIN_PASSWORD` | 初始管理员密码 | 无默认值 |
| `QIANYU_BLOG_MUSIC_STORAGE_DIR` | 音乐文件目录 | `uploads/music` |
| `QIANYU_BLOG_MEDIA_STORAGE_DIR` | 图片文件目录 | `uploads/media` |

前端环境变量：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 默认管理员账号

后端启动时会自动检查管理员账号是否存在；如果不存在，会按 `QIANYU_BLOG_ADMIN_USERNAME` 和 `QIANYU_BLOG_ADMIN_PASSWORD` 自动创建。

建议部署前改成你自己的环境变量，不要直接用于公网环境。`QIANYU_BLOG_JWT_SECRET` 需要至少 32 个字符，否则后端会因 JWT 密钥强度不足而启动失败。

## 文件上传与资源存储

默认上传目录：

- 图片：`qianyu_blog_backend/uploads/media`
- 音乐：`qianyu_blog_backend/uploads/music`

这些目录通常不应提交真实资源文件到 Git 仓库。

## 测试与构建

### 后端测试

```powershell
cd qianyu_blog_backend
mvn test
```

### 前端构建

```powershell
cd qianyu_blog_frontend
npm run build
```

### 前端 E2E 测试

```powershell
cd qianyu_blog_frontend
npm run test:e2e
```

## 当前实现包含的内容

- 博客文章、分类、标签的数据模型与接口
- 后台文章与分类管理
- 图片资源管理与公开访问
- 音乐上传、播放与频道展示
- AI 对话、AI 生图、AI 润色
- 关于页、相册页、归档页等前台页面

## 注意事项

### 1. 敏感配置

`application.properties` 已经改为通过环境变量读取敏感配置。公开到 GitHub 前，建议：

- 仅保留示例值
- 生产环境改为环境变量注入
- 不要把真实 API Key、数据库密码提交到仓库

### 2. AI 功能依赖外部模型服务

AI 对话、AI 生图和文章润色功能需要先在后台配置可用的模型接口地址和 API Key，否则相关接口会返回配置错误。

## 后续可完善方向

- 补充更完整的接口文档与示例请求
- 给媒体和音乐资源接入对象存储
- 完善权限模型与后台角色控制
- 补齐前后端自动化测试

## 许可证

当前仓库未声明许可证。如需开源发布，建议补充 `LICENSE` 文件后再公开。
