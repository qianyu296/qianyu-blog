# 千语博客

一个前后端分离的个人博客项目，包含博客内容管理、分类与标签、图片资源库、音乐频道、AI 对话与生图、后台管理等能力。

项目由两个子应用组成：

- `qianyu_blog_backend`：Spring Boot 3 后端
- `qianyu_blog_frontend`：Vue 3 + TypeScript 前端

## 项目特性

### 前台站点

- 首页文章列表与文章详情页
- 分类筛选、标签列表、归档页
- 关于页、光影相册页、小游戏中心
- 音乐频道页，支持公共曲库播放
- AI 对话页，支持流式聊天与图片生成
- 站点信息动态读取，可从后台配置站点标题、副标题、Hero 文案等

### 后台管理

- 管理员登录，基于 JWT 认证
- 文章管理：新增、编辑、删除、发布状态、置顶
- 分类管理
- 图片资源管理：上传、查询、删除
- 音乐管理：上传音频、歌词文件、更新曲目信息、删除
- AI 设置管理：模型、接口地址、Key、最大输出长度
- AI 辅助润色文章
- 站点信息配置

### 其他能力

- 提供 Swagger / OpenAPI 文档
- 提供 `schema.sql` 初始化数据库脚本
- 后端包含基础测试，测试环境可使用 H2

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
   ├─ src/
   │  ├─ api/
   │  ├─ components/
   │  ├─ composables/
   │  ├─ constants/
   │  ├─ router/
   │  ├─ stores/
   │  ├─ types/
   │  ├─ utils/
   │  └─ views/
   └─ public/
```

## 主要页面

### 前台路由

- `/`：首页
- `/posts/:id`：文章详情
- `/archive`：归档页
- `/about`：关于页
- `/light`：光影相册
- `/games`：小游戏中心
- `/chat`：AI 对话 / AI 生图

### 后台路由

- `/admin/login`：后台登录
- `/admin/dashboard`：控制台
- `/admin/posts`：文章管理
- `/admin/categories`：分类管理
- `/admin/media`：图片资源管理
- `/admin/music`：音乐管理
- `/admin/ai-settings`：AI 配置

## 核心接口概览

### 公开接口

- `GET /api/public/site/settings`：站点配置
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
- `GET/POST /api/admin/site-settings`：站点设置
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

或者手动执行 `schema.sql` 中的建库建表语句。

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

## 配置说明

后端主要配置位于 `qianyu_blog_backend/src/main/resources/application.properties`。

常用配置项如下：

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/qianyu_blog...` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `SERVER_PORT` | 后端端口 | `8080` |
| `JWT_SECRET` | JWT 密钥 | 示例值 |
| `JWT_EXPIRATION_MINUTES` | Token 有效期 | `120` |
| `ADMIN_USERNAME` | 初始管理员账号 | `admin` |
| `ADMIN_PASSWORD` | 初始管理员密码 | `admin123` |
| `MUSIC_STORAGE_DIR` | 音乐文件目录 | `uploads/music` |
| `MEDIA_STORAGE_DIR` | 图片文件目录 | `uploads/media` |

前端通过环境变量控制后端地址：

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 默认管理员账号

后端启动时会自动检查管理员账号是否存在；如果不存在，会按配置自动创建。

默认值：

- 用户名：`admin`
- 密码：`admin123`

建议在首次部署前改成你自己的环境变量，不要直接用于公网环境。

## 文件上传与资源存储

本项目默认把上传文件存放在后端本地目录：

- 图片：`qianyu_blog_backend/uploads/media`
- 音乐：`qianyu_blog_backend/uploads/music`

这些目录通常不应提交到 Git 仓库，仓库里应只保留源码和配置。

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

基于仓库现状，这个版本已经包含：

- 博客文章、分类、标签的数据模型与接口
- 后台文章与分类管理
- 站点设置动态化
- 图片资源管理与公开访问
- 音乐上传、播放与频道式展示
- AI 对话、AI 生图、AI 润色
- 多个前端扩展页面与小游戏页面

## 已知注意事项

### 1. 关于页头像资源

`qianyu_blog_frontend/src/views/AboutView.vue` 当前直接引用了本地图片文件：

- `qianyu_blog_frontend/photo/my.jpg`

如果你没有把这个目录一并提交，克隆仓库后前端构建可能失败。更稳妥的做法有两种：

- 把该头像图放到可跟踪的 `src/assets` 或 `public` 目录
- 或者修改关于页为使用后台站点配置中的头像地址

### 2. 公开仓库中的敏感配置

`application.properties` 中现在保留了开发用默认值，包括数据库密码、JWT Secret 和管理员默认口令。公开到 GitHub 前，建议：

- 仅保留示例值
- 生产环境统一改为环境变量注入
- 不要把真实 API Key、数据库密码提交到仓库

### 3. AI 功能依赖外部模型服务

AI 对话、AI 生图和文章润色功能需要先在后台配置可用的模型接口地址和 API Key，否则相关接口会返回配置错误。

## 后续可完善方向

- 增加 Docker / Docker Compose 部署方案
- 补充完整的接口文档与示例请求
- 给媒体和音乐资源接入对象存储
- 完善权限模型与后台角色控制
- 补齐前后端自动化测试

## 许可证

当前仓库未声明许可证。如需开源发布，建议补充 `LICENSE` 文件后再公开。
