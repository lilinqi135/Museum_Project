# 展品知识库闭环 Postman 完整测试案例

## 一、说明

本文档用于在 Postman 中完整验证“展品录入 -> 资料上传 -> 文档绑定 -> 游客提问 -> 精准回答”闭环。

以下 URL 全部按本地默认启动地址编写：

- `http://localhost:8091`

如果你实际启动地址不是本机 `8091` 端口，请把下文所有 URL 中的域名和端口替换成你自己的实际地址。

## 二、建议的 Postman 环境变量

建议先在 Postman 新建一个环境，配置以下变量：

- `baseUrl` = `http://localhost:8091`
- `token` = 登录成功后填写
- `museumId` = `1`
- `exhibitId` = 新增展品成功后填写
- `documentId` = 上传文档成功后填写
- `deviceId` = `demo-esp32-001` (或你真实的设备ID)

## 三、公共请求头

### 1. 需要登录的接口

请求头统一带：

- `Authorization: Bearer {{token}}`
- `Content-Type: application/json`

### 2. 不需要登录的接口

以下接口当前可直接调：

- `POST http://localhost:8091/api/museum/chat`

## 四、演示资料文件

仓库里已经准备好了 3 份可直接上传的样例文件：

- `f:\xiaozhi-esp32-server-java-main\demo_upload_docs\bronze_zun_demo.md`
- `f:\xiaozhi-esp32-server-java-main\demo_upload_docs\tang_tricolor_horse_demo.md`
- `f:\xiaozhi-esp32-server-java-main\demo_upload_docs\lanting_preface_copy_demo.md`

建议先用第一份：

- `f:\xiaozhi-esp32-server-java-main\demo_upload_docs\bronze_zun_demo.md`

## 五、测试顺序总览

建议按以下顺序执行：

1. 登录获取 token
2. 新增展品
3. 查询展品列表
4. 查询展品详情
5. 上传展品资料
6. 查询文档列表
7. 查询文档状态
8. 绑定文档到展品
9. 查询展品关联文档
10. 游客问答 `visitor`
11. 游客问答 `edu`
12. 游客问答 `kids`
13. 非法模式问答
14. 非法展品问答
15. 删除文档
16. 删除展品
17. 管理员侧：设置设备归属 (Phase 3)
18. 硬件侧：获取设备上下文 (Phase 3)
19. 硬件侧：一键语音对话 (Phase 3)

## 六、详细测试案例

### 1. 登录获取 token

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/user/login`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "username": "admin",
  "password": "123456"
}
```

#### 说明

- 如果你本地 `admin` 密码不是 `123456`，请替换成你自己的实际账号密码。
- 登录成功后，把返回里的 `data.token` 填到 Postman 环境变量 `token`。

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "操作成功"`
  - `data.token` 非空
  - eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOjEsInJuU3RyIjoiSVVXUms5MzJtTVo1dFVMVDkwekNjSnczTnRQczZNaDkifQ.bSYfDM334374u63CrDmkesn7dCvBLpjm1lSqOH5Nz10

---

### 2. 新增展品

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/exhibit`
- 请求头：
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "name": "青铜尊",
  "description": "商代晚期青铜礼器，用于盛酒与礼仪活动。",
  "era": "商代",
  "hall": "青铜器馆",
  "categoryId": 1,
  "imageUrl": "https://example.com/images/bronze-zun.jpg",
  "tags": "青铜器,商代,礼器",
  "status": "ENABLED",
  "sort": 100
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "新增成功"`
  - `data.id` 非空
  - `data.museumId = 1`
  - `data.name = "青铜尊"`

#### 后续操作

- 将返回的 `data.id` 保存为 `exhibitId`
- exhibitId：9

---

### 3. 查询展品列表

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/exhibit?museumId=1&name=青铜&status=ENABLED&start=1&limit=10`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.list` 或分页数据中包含刚创建的展品

---

### 4. 查询展品详情

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/exhibit/{{exhibitId}}`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.id = {{exhibitId}}`
  - `data.name = "青铜尊"`

---

### 5. 上传展品资料

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/knowledge/document/upload`
- 请求头：
  - `Authorization: Bearer {{token}}`
- Body 类型：
  - `form-data`

#### 重要说明

- 这一条请求不要手动写 `Content-Type: application/json`
- 也不要手动写固定的 `multipart/form-data`
- 让 Postman 在你选择 `form-data` 之后自动生成 `Content-Type` 和 boundary
- `file` 这一项的类型必须切换为 `File`
- `file` 字段名必须严格叫 `file`

#### form-data 字段

- `file`：选择文件  
  文件路径建议使用：  
  `f:\xiaozhi-esp32-server-java-main\demo_upload_docs\bronze_zun_demo.md`
- `museumId`：`1`
- `exhibitId`：`{{exhibitId}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "上传成功，已提交索引任务"`
  - `data.id` 非空
  - `data.museumId = 1`
  - `data.exhibitId = {{exhibitId}}`
  - `data.status = "PENDING"` 或接近该状态

#### 后续操作

- 将返回的 `data.id` 保存为 `documentId`

#### 如果返回 400 或 500

优先检查这几项：

- 你是否把 Body 选成了 `form-data`
- `file` 的类型是否为 `File`
- `file` 字段名是否写成了别的，比如 `files`、`uploadFile`
- 你是否手动写了错误的 `Content-Type`
- 你是否真的选择了本地文件

---

### 6. 查询文档列表

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/knowledge/document/list?museumId=1&page=1&pageSize=10`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.records` 中包含刚上传的文档

---

### 7. 按状态查询文档列表

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/knowledge/document/list?museumId=1&status=COMPLETED&page=1&pageSize=10`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 若文档已索引完成，返回结果中包含该文档
- 若尚未完成，可稍后重复请求

---

### 8. 查询文档状态

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/knowledge/document/status/{{documentId}}?museumId=1`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.id = {{documentId}}`
  - `data.status` 最终应变为 `COMPLETED`
  - `data.chunkCount > 0`

#### 说明

- 如果刚上传后还没完成索引，可每隔几秒重复请求一次

---

### 9. 绑定文档到展品

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/knowledge/document/{{documentId}}/bind-exhibit?museumId=1&exhibitId={{exhibitId}}`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "绑定成功，已提交索引更新任务"`

#### 说明

- 即使上传时已经带了 `exhibitId`，这里仍建议手动测一次，验证绑定接口本身无问题

---

### 10. 查询展品关联文档

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/knowledge/document/by-exhibit?museumId=1&exhibitId={{exhibitId}}&page=1&pageSize=10`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.records` 中能看到该文档

---

### 11. 游客问答 `visitor`

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "exhibitId": {{exhibitId}},
  "roleId": 1,
  "mode": "visitor",
  "debug": true,
  "question": "这件青铜尊是做什么用的？"
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "操作成功"`
  - `data` 为模型回答内容字符串
  - `debug.contextText` 非空
  - `debug.contextSources` 包含上传文档来源

---

### 12. 游客问答 `edu`

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "exhibitId": {{exhibitId}},
  "roleId": 1,
  "mode": "edu",
  "debug": true,
  "question": "这件青铜尊反映了商代怎样的礼制文化？"
}
```

#### 预期

- HTTP 状态：`200`
- 回答比 `visitor` 更偏知识讲解
- `debug.hasKnowledge = true`

---

### 13. 游客问答 `kids`

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "exhibitId": {{exhibitId}},
  "roleId": 1,
  "mode": "kids",
  "debug": true,
  "question": "这个青铜尊是干嘛的呀？"
}
```

#### 预期

- HTTP 状态：`200`
- 回答更通俗、更像面向小朋友

---

### 14. 非法模式测试

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "exhibitId": {{exhibitId}},
  "roleId": 1,
  "mode": "expert",
  "debug": true,
  "question": "这件展品有什么特点？"
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 400`
  - `message = "mode 仅支持 visitor、edu、kids"`

---

### 15. 非法展品测试

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": 1,
  "exhibitId": 999999,
  "roleId": 1,
  "mode": "visitor",
  "debug": true,
  "question": "这件展品是什么？"
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 400`
  - `message = "展品不存在或不属于当前场馆"`

---

### 16. 非法文档状态筛选测试

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/knowledge/document/list?museumId=1&status=DONE&page=1&pageSize=10`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 400`
  - `message = "status 仅支持 PENDING、PROCESSING、COMPLETED、FAILED"`

---

### 17. 删除文档

#### 请求

- 方法：`DELETE`
- URL：`http://localhost:8091/api/knowledge/document/{{documentId}}?museumId=1`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "删除成功"`

#### 后续验证

- 再次调用文档状态查询，预期提示文档不存在或不属于当前场馆

---

### 18. 删除展品

#### 请求

- 方法：`DELETE`
- URL：`http://localhost:8091/api/exhibit/{{exhibitId}}?museumId=1`
- 请求头：
  - `Authorization: Bearer {{token}}`

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "删除成功"`

## 七、推荐的 Postman 变量提取

### 1. 从登录接口提取 token

在 Tests 中可写：

```javascript
pm.environment.set("token", pm.response.json().data.token);
```

### 2. 从新增展品接口提取 exhibitId

```javascript
pm.environment.set("exhibitId", pm.response.json().data.id);
```

### 3. 从上传文档接口提取 documentId

```javascript
pm.environment.set("documentId", pm.response.json().data.id);
```

## 八、建议的完整演示路径

建议现场演示按以下 6 步走：

1. `POST http://localhost:8091/api/exhibit` 新增一个展品
2. `POST http://localhost:8091/api/knowledge/document/upload` 上传对应资料
3. `GET http://localhost:8091/api/knowledge/document/status/{documentId}?museumId=1` 等待索引完成
4. `GET http://localhost:8091/api/knowledge/document/by-exhibit?museumId=1&exhibitId={exhibitId}&page=1&pageSize=10` 展示绑定结果
5. `POST http://localhost:8091/api/museum/chat` 用 `visitor` 或 `edu` 模式提问
6. 再用一个未命中问题测试系统边界感

## 九、补充说明

- 当前 `ExhibitController`、`KnowledgeController` 默认需要登录。
- 当前 `MuseumChatController` 测试接口可直接调用，不需要登录。
- 如果你的本地角色 `roleId=1` 不可用，请改成你数据库里真实存在的角色 ID。
- 如果你更希望通过设备推断角色，可以把 `roleId` 去掉，改传 `deviceId`，但前提是该设备已存在且已配置角色。

---

## 十、集成与硬件模拟测试 (Phase 3)

这一部分模拟真实的硬件接入流程，验证“设备绑定 -> 上下文获取 -> 自动推断问答”的闭环。

### 17. 管理员侧：设置设备归属

模拟管理员在后台将设备分配给某个博物馆和展品。

#### 请求

- 方法：`PUT`
- URL：`http://localhost:8091/api/device/{{deviceId}}`
- 请求头：
  - `Authorization: Bearer {{token}}`
  - `Content-Type: application/json`

#### Body

```json
{
  "museumId": {{museumId}},
  "exhibitId": {{exhibitId}},
  "deviceName": "大创路演 1 号机"
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "操作成功"`

---

### 18. 硬件侧：获取设备上下文

模拟硬件（如 ESP32）开机时，通过自己的设备 ID 获取当前应该处于哪个馆、讲哪个展品。

#### 请求

- 方法：`GET`
- URL：`http://localhost:8091/api/device/context/{{deviceId}}`
- 请求头：无需 (SaIgnore)

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `data.museumId = {{museumId}}`
  - `data.exhibitId = {{exhibitId}}`

---

### 19. 硬件侧：一键语音对话 (DeviceId 驱动)

模拟硬件端直接发起对话。注意：请求体中**不传** `museumId` 和 `exhibitId`，全靠后端根据 `deviceId` 自动推断。

#### 请求

- 方法：`POST`
- URL：`http://localhost:8091/api/museum/chat`
- 请求头：
  - `Content-Type: application/json`

#### Body

```json
{
  "deviceId": "{{deviceId}}",
  "question": "这个东西的历史背景是什么？",
  "mode": "visitor",
  "debug": true
}
```

#### 预期

- HTTP 状态：`200`
- 返回 JSON 中：
  - `code = 200`
  - `message = "操作成功"`
  - `data` 成功返回基于文档的回答
  - `debug.requestedMuseumId` 自动匹配为你在步骤 17 设置的值
  - `debug.requestedExhibitId` 自动匹配为你在步骤 17 设置的值

