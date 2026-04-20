# 智博导览系统 - 前端接入详细指南 (Admin Web)

## 1. 文档目的

面向管理后台前端同学，给出可直接开发与联调的后端接口说明。

本次重点围绕博物馆业务闭环：

1. 展品管理（Exhibit）
2. 文档知识库管理（Knowledge）
3. 设备绑定展品后的上下文验证（Device Context）
4. 基于设备/展品的问答联调（Museum Chat）

---

## 2. 项目现状结论（先看）

基于当前仓库 `web` 前端与后端控制器对照，结论如下：

### 2.1 已有前端页面（可直接用）

1. 登录相关页面已完成：`/login`、`/register`、`/forget`
2. 设备管理页面已完成：`/device`
3. 用户、角色、模板、配置、消息、记忆等通用管理页已存在

### 2.2 仍需补充前端页面（本次重点）

1. **展品管理页**（对接 `/api/exhibit`）
2. **知识库管理页**（对接 `/api/knowledge/**`）
3. **博物馆联调面板**（对接 `/api/museum/chat`、`/api/museum/refresh`，可选集成 `/api/device/context/{deviceId}`）

> 备注：当前 `web/src/services` 中尚未发现 `exhibit`、`knowledge`、`museum` 相关 service 封装，建议新增。

---

## 3. 全局调用约定

### 3.1 Base URL

- 接口统一挂在 `/api`
- 示例：`POST /api/exhibit`

### 3.2 鉴权

除标注 `@SaIgnore` 的开放接口外，其余接口都需要登录态。

- Header：`Authorization: Bearer <token>`
- 登录接口：`POST /api/user/login`（已有前端）

### 3.3 统一响应结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

建议前端统一按 `code` 处理：

- `200`: 成功
- `400`: 业务参数/校验错误（直接透传 `message`）
- `401/403`: 未登录或登录失效（跳转登录）
- `500`: 服务异常（提示“系统繁忙，请稍后重试”）

### 3.4 分页参数有两套（易踩坑）

1. 展品接口使用：`start` + `limit`
2. 知识库接口使用：`page` + `pageSize`

---

## 4. 接口文档：展品管理（必须新增前端）

Controller：`/api/exhibit`

### 4.1 展品字段说明

- `id`: Long，展品ID
- `museumId`: Long，馆ID（必填）
- `name`: String，展品名称（新增时必填）
- `description`: String，讲解文案
- `era`: String，年代
- `hall`: String，展厅
- `categoryId`: Long，分类ID
- `imageUrl`: String，图片地址
- `tags`: String，标签（逗号分隔）
- `status`: String，`ENABLED` / `DISABLED`
- `sort`: Integer，排序值

### 4.2 查询展品列表

- Method: `GET`
- URL: `/api/exhibit`
- Query:
  - `museumId` 必填
  - `name` 可选
  - `status` 可选
  - `start` 可选，默认 1
  - `limit` 可选，默认 10，最大 1000

示例：

```http
GET /api/exhibit?museumId=1&name=青铜&status=ENABLED&start=1&limit=10
```

### 4.3 查询展品详情

- Method: `GET`
- URL: `/api/exhibit/{id}`

### 4.4 新增展品

- Method: `POST`
- URL: `/api/exhibit`
- Body(JSON):

```json
{
  "museumId": 1,
  "name": "西周青铜尊",
  "description": "用于祭祀礼仪的青铜器",
  "era": "西周",
  "hall": "第一展厅",
  "categoryId": 2,
  "imageUrl": "https://example.com/zun.jpg",
  "tags": "青铜器,礼器",
  "status": "ENABLED",
  "sort": 10
}
```

### 4.5 更新展品

- Method: `PUT`
- URL: `/api/exhibit/{id}`
- Body(JSON):

```json
{
  "museumId": 1,
  "name": "西周青铜尊（更新）",
  "description": "更新后的讲解文案",
  "status": "ENABLED",
  "sort": 20
}
```

> 注意：更新接口要求 `museumId` 必传，用于校验展品归属。

### 4.6 删除展品（逻辑删除）

- Method: `DELETE`
- URL: `/api/exhibit/{id}`
- Query: `museumId` 必填

示例：

```http
DELETE /api/exhibit/10?museumId=1
```

### 4.7 常见错误

- `museumId 不能为空`
- `展品ID非法`
- `展品不存在`
- `展品不存在或不属于该场馆`

---

## 5. 接口文档：知识库管理（必须新增前端）

Controller：`/api/knowledge`

### 5.1 文档字段说明

- `id`: Long，文档ID
- `museumId`: Long，馆ID
- `exhibitId`: Long，关联展品ID，可空
- `fileName`: String，原文件名
- `fileUrl`: String，存储路径/URL
- `fileHash`: String，SHA-256
- `fileSize`: Long，文件大小
- `status`: String，`PENDING` / `PROCESSING` / `COMPLETED` / `FAILED`
- `chunkCount`: Integer，切片数
- `errorMsg`: String，失败原因

### 5.2 上传文档（异步索引）

- Method: `POST`
- URL: `/api/knowledge/document/upload`
- Body: `form-data`
- 字段：
  - `file` 必填
  - `museumId` 必填
  - `exhibitId` 可选

支持格式：`txt` / `pdf` / `docx` / `md`

前端注意：

1. 必须用 `form-data`
2. `file` 字段名必须是 `file`
3. 不要手动设置 `multipart/form-data` 的 boundary

### 5.3 文档列表

- Method: `GET`
- URL: `/api/knowledge/document/list`
- Query:
  - `museumId` 必填
  - `status` 可选（仅支持 `PENDING/PROCESSING/COMPLETED/FAILED`）
  - `page` 可选，默认 1
  - `pageSize` 可选，默认 10，最大 100

### 5.4 查询文档状态

- Method: `GET`
- URL: `/api/knowledge/document/status/{documentId}`
- Query: `museumId` 必填

### 5.5 按展品查询文档

- Method: `GET`
- URL: `/api/knowledge/document/by-exhibit`
- Query:
  - `museumId` 必填
  - `exhibitId` 必填
  - `page` 可选
  - `pageSize` 可选

### 5.6 绑定文档到展品

- Method: `POST`
- URL: `/api/knowledge/document/{documentId}/bind-exhibit`
- Query:
  - `museumId` 必填
  - `exhibitId` 必填

> 绑定后会触发重新索引，状态会回到 `PENDING -> PROCESSING -> COMPLETED`。

### 5.7 解绑文档与展品

- Method: `POST`
- URL: `/api/knowledge/document/{documentId}/unbind-exhibit`
- Query:
  - `museumId` 必填

### 5.8 删除文档

- Method: `DELETE`
- URL: `/api/knowledge/document/{documentId}`
- Query: `museumId` 必填

### 5.9 按馆重建索引（管理操作）

- Method: `POST`
- URL: `/api/knowledge/rebuild`
- Query: `museumId` 必填

### 5.10 常见错误

- `上传文件不能为空`
- `museumId 非法`
- `仅支持 txt/pdf/docx/md 文件`
- `status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED`
- `文档不属于当前场馆`
- `展品不存在或不属于当前场馆`
- `相同文档已绑定到其他展品，请先解绑后再调整归属`

---

## 6. 接口文档：联调面板（建议新增前端）

### 6.1 设备上下文查询（开放接口）

- Method: `GET`
- URL: `/api/device/context/{deviceId}`
- 鉴权：不需要登录（`@SaIgnore`）

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "deviceId": "demo-esp32-001",
    "museumId": 1,
    "exhibitId": 10,
    "deviceName": "一号厅导览机"
  }
}
```

### 6.2 博物馆问答测试（开放接口）

- Method: `POST`
- URL: `/api/museum/chat`
- 鉴权：不需要登录（`@SaIgnore`）
- Body(JSON):

```json
{
  "deviceId": "demo-esp32-001",
  "museumId": 1,
  "exhibitId": 10,
  "question": "这件展品的历史背景是什么？",
  "mode": "visitor",
  "debug": true
}
```

参数说明：

- `question` 必填
- `museumId` 可选（传 `deviceId` 时可自动推断）
- `exhibitId` 可选
- `roleId` 可选（不传可由 `deviceId` 推断）
- `mode` 可选，支持 `visitor` / `edu` / `kids`
- `debug` 可选，`true` 时返回调试信息

### 6.3 刷新知识库（开放接口）

- Method: `POST`
- URL: `/api/museum/refresh`
- 作用：手动触发知识库刷新

---

## 7. 前端开发任务拆分（建议）

### P0（必须）

1. 新增 `services/exhibit.ts`
2. 新增 `services/knowledge.ts`
3. 新增 `services/museum.ts`
4. 新增页面：`ExhibitView`（列表 + 新增 + 编辑 + 删除）
5. 新增页面：`KnowledgeView`（上传 + 列表 + 状态 + 绑定/解绑 + 删除）
6. 在侧边栏增加菜单入口（建议放在“设备管理”附近）

### P1（推荐）

1. 新增联调页：输入 `deviceId/question` 调 `/api/museum/chat`
2. 展示 `debug` 信息（`requestedMuseumId`、`requestedExhibitId`、`contextSources`）
3. 提供 `/api/device/context/{deviceId}` 快速校验按钮

### P2（可选）

1. 加入 `/api/knowledge/rebuild` 管理操作（权限控制 + 二次确认）
2. 上传后自动轮询状态直到 `COMPLETED/FAILED`

---

## 8. 推荐调用时序（上线闭环）

### 8.1 资料上线

1. 创建展品，拿到 `exhibitId`
2. 上传文档，拿到 `documentId`
3. 轮询状态到 `COMPLETED`
4. 若未绑定，执行绑定接口
5. 在“按展品查文档”确认可见
6. 在联调页发起 `/api/museum/chat` 验证问答质量

### 8.2 设备配置联动

1. 在现有设备页更新 `museumId/exhibitId`
2. 调 `/api/device/context/{deviceId}` 校验生效
3. 调 `/api/museum/chat`（仅传 `deviceId`）验证自动推断是否正确

---

## 9. 联调验收清单

1. 展品增删改查全部可用
2. 文档上传后状态可见且可达 `COMPLETED`
3. 文档绑定/解绑/删除行为正确
4. 设备切换展品后，上下文查询立即反映新值
5. 问答接口可按设备上下文返回对应展品内容
6. 常见错误文案可在前端准确展示

---

## 10. 后端串联审核结论（项目现状）

### 10.1 已串联完成（前端可直接开发）

1. **设备 -> 场馆/展品上下文**：`PUT /api/device/{deviceId}` 可写入 `museumId/exhibitId`，`GET /api/device/context/{deviceId}` 可读取。
2. **知识库 -> 展品绑定**：文档支持上传时携带 `exhibitId`，也支持后续绑定/解绑；绑定变更会触发重新索引。
3. **问答 -> 设备自动推断**：`POST /api/museum/chat` 在未传 `museumId/exhibitId` 时，会根据 `deviceId` 自动推断并检索对应上下文。
4. **RAG 检索过滤**：检索阶段会先按 `museumId` 过滤，再按 `exhibitId` 过滤，优先命中展品级结果。

### 10.2 当前限制与注意事项（建议前端知晓）

1. **设备解绑展品能力缺口**：当前设备更新接口对 `exhibitId` 是“非空才更新”，前端无法通过传 `null` 清空展品绑定。  
   现阶段建议：前端只做“切换展品”，不做“清空展品”。
2. **开放联调接口仅建议测试环境使用**：`/api/museum/chat`、`/api/museum/refresh`、`/api/device/context/*` 为开放接口，生产环境建议增加鉴权或网关限制。
3. **知识库重建接口是管理操作**：`/api/knowledge/rebuild` 可能触发大量重建，前端必须做二次确认与权限控制。

### 10.3 对前端同学的最终结论

1. **登录页与设备管理页**：已存在，可继续沿用当前实现。
2. **展品管理与知识库管理页**：后端接口已具备，前端可以立即开发，不需要等待后端再开新接口。
3. **联调面板**：可并行开发，优先用于测试环境联调硬件问答链路。
