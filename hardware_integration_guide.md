# 智博导览系统 - 硬件接入详细指南 (ESP32/终端)

## 1. 目标与原则

硬件侧最小目标：

1. 设备上线后可识别自身归属（馆 + 展品）
2. 用户提问可命中对应展品资料
3. 网络抖动下可恢复

接入原则：

- 设备端尽量少传业务参数，优先只传 `deviceId`
- 展品上下文由后端统一维护，硬件只拉取和使用
- 失败时必须可重试，不可静默失败

## 2. 最小接口清单

### 2.1 开机拉取上下文 (必调)

- 方法：`GET`
- URL：`/api/device/context/{deviceId}`
- 鉴权：免鉴权

成功响应：

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

失败处理：

- `code != 200`：进入“离线待配置”状态
- 每 10 秒重试，最多 6 次
- 超过 6 次后每 60 秒重试一次

### 2.2 发起问答 (核心链路)

- 方法：`POST`
- URL：`/api/museum/chat`
- 鉴权：免鉴权

请求建议：

```json
{
  "deviceId": "demo-esp32-001",
  "question": "这个展品是什么年代的？",
  "mode": "visitor",
  "debug": false
}
```

说明：

- 不必传 `museumId`、`exhibitId`
- 后端会按设备配置自动推断
- 如需强制切换展品，可临时传 `exhibitId`

## 3. 设备状态机建议

建议状态：

1. `BOOTING`：启动中
2. `SYNC_CONTEXT`：拉取上下文
3. `IDLE_READY`：待唤醒
4. `LISTENING`：采集语音
5. `ASKING`：请求问答
6. `PLAYING`：播报答案
7. `OFFLINE_RETRY`：网络异常重试

状态迁移建议：

- `BOOTING -> SYNC_CONTEXT -> IDLE_READY`
- `IDLE_READY -> LISTENING -> ASKING -> PLAYING -> IDLE_READY`
- 任一网络失败：进入 `OFFLINE_RETRY`

## 4. 重试与超时策略

### 4.1 HTTP 超时

- 连接超时：3 秒
- 读超时：15 秒（LLM 可能稍慢）

### 4.2 重试策略

- `GET /context`：指数退避重试（2s/4s/8s/16s/...）
- `POST /chat`：同一问题最多重试 1 次，避免重复播报

### 4.3 幂等保护

- 建议本地保存最近一次 `question` 指纹（hash）
- 连续 5 秒内相同问题不重复上送

## 5. 本地缓存建议

缓存字段：

- `deviceId`（固化）
- `museumId`（可刷新）
- `exhibitId`（可刷新）
- `deviceName`（展示可选）

刷新时机：

1. 开机后首次拉取
2. 每 5 分钟后台刷新一次
3. 收到“配置变更”事件时立即刷新（若后续加推送）

## 6. 异常场景处理

### 6.1 设备未激活

现象：

- `/api/device/context/{deviceId}` 返回错误

处理：

- 播报“设备未激活，请联系管理员”
- 保持周期性重试

### 6.2 问答业务错误

现象：

- `/api/museum/chat` 返回 `code=400`

处理：

- 直接播报 `message`（可加简化文案）
- 不进入长时间离线状态

### 6.3 服务端异常

现象：

- `/api/museum/chat` 返回 `code=500` 或网络超时

处理：

- 播报“当前网络繁忙，请稍后再试”
- 回到 `IDLE_READY`

## 7. 硬件联调检查表

1. 使用真实 `deviceId` 可成功获取上下文
2. 管理员修改设备展品后，设备下次刷新可感知变更
3. 只传 `deviceId + question` 可得到基于知识库的答案
4. 断网后可自动重试并恢复
5. 高频提问时不会重复提交同一句
6. 错误码 400/500 的播报分流正确

## 8. 建议联调顺序

1. 固定一个 `deviceId`，先打通 `/api/device/context`
2. 再打通 `/api/museum/chat` 的文本问答
3. 最后接入 STT/TTS 形成完整语音闭环

## 9. 参考请求示例

### 9.1 PowerShell

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8091/api/device/context/demo-esp32-001"
```

```powershell
$body = @{
  deviceId = "demo-esp32-001"
  question = "这件展品有什么特点？"
  mode = "visitor"
  debug = $false
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8091/api/museum/chat" `
  -ContentType "application/json" `
  -Body $body
```
