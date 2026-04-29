# API 接口设计文档

## 1. 基本信息
- 服务地址：`http://localhost:8080`
- API 前缀：`/api`
- 文档格式：OpenAPI 3.0
- Apifox 导入文件：`docs/openapi.json`

## 2. 鉴权方式
后台接口使用 Bearer JWT：

```http
Authorization: Bearer <token>
```

登录接口和公开博客接口无需鉴权。

## 3. 通用响应结构
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 4. 错误码
| code | HTTP | 说明 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40000 | 400 | 参数校验失败 |
| 40100 | 401 | 未登录、登录过期或账号密码错误 |
| 40400 | 404 | 资源不存在 |
| 40900 | 409 | 资源冲突 |
| 50000 | 500 | 服务器内部错误 |

## 5. 公开接口
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/public/categories` | 分类列表 |
| GET | `/api/public/posts` | 已发布文章分页列表，支持 `categoryId/page/size` |
| GET | `/api/public/posts/{id}` | 已发布文章详情 |

## 6. 后台接口
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/auth/login` | 管理员登录 |
| GET | `/api/admin/categories` | 后台分类列表 |
| POST | `/api/admin/categories` | 新增分类 |
| PUT | `/api/admin/categories/{id}` | 编辑分类 |
| DELETE | `/api/admin/categories/{id}` | 删除分类 |
| GET | `/api/admin/posts` | 后台文章分页列表 |
| GET | `/api/admin/posts/{id}` | 后台文章详情 |
| POST | `/api/admin/posts` | 新增文章 |
| PUT | `/api/admin/posts/{id}` | 编辑文章 |
| DELETE | `/api/admin/posts/{id}` | 删除文章 |

## 7. Apifox 导入方式
1. 打开 Apifox。
2. 选择导入项目或导入接口。
3. 选择 OpenAPI/Swagger 格式。
4. 导入 `docs/openapi.json`。
5. 在环境变量中配置服务地址 `http://localhost:8080`。
6. 调用登录接口后，将返回的 token 设置到 Bearer Token 鉴权中。
