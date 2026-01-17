# 日志功能说明 (Logging Documentation)

## 概述 (Overview)

本项目已添加完整的日志功能，将所有操作记录到文件中。日志系统使用 Logback 作为日志框架，提供了两种日志文件：

1. **通用日志** (`logs/kirakira.log`) - 记录所有应用程序日志
2. **操作日志** (`logs/operations.log`) - 专门记录用户操作和系统活动

## 日志文件位置 (Log File Locations)

所有日志文件存储在项目根目录的 `logs/` 文件夹中：

```
logs/
├── kirakira.log              # 当前通用日志文件
├── kirakira.2026-01-16.log   # 历史通用日志（按日期滚动）
├── operations.log            # 当前操作日志文件
└── operations.2026-01-16.log # 历史操作日志（按日期滚动）
```

## 日志配置 (Log Configuration)

### 日志滚动策略 (Rolling Policy)

#### 通用日志 (kirakira.log)
- 按天滚动 (Daily rollover)
- 保留 30 天历史记录
- 最大总大小: 1GB

#### 操作日志 (operations.log)
- 按天滚动 (Daily rollover)
- 保留 90 天历史记录
- 最大总大小: 2GB

### 日志格式 (Log Format)

**通用日志格式:**
```
2026-01-17 14:30:45 [main] INFO  c.k.service.BotService - 日志消息
```

**操作日志格式:**
```
2026-01-17 14:30:45 - BIND - Group: 123456, QQ: 789012, CF: tourist - SUCCESS
```

## 记录的操作 (Logged Operations)

### 1. 账号绑定 (BIND)
记录用户绑定 Codeforces 账号的操作

**日志示例:**
```
BIND - Group: 123456789, QQ: 987654321, CF: tourist - START
BIND - Group: 123456789, QQ: 987654321, CF: tourist - SUCCESS
BIND - Group: 123456789, QQ: 987654321, CF: invalid - FAILED: User not found
```

### 2. 账号解绑 (UNBIND)
记录用户解绑 Codeforces 账号的操作

**日志示例:**
```
UNBIND - Group: 123456789, QQ: 987654321, CF: tourist - START
UNBIND - Group: 123456789, QQ: 987654321, CF: tourist - SUCCESS
```

### 3. 列出个人绑定 (LIST)
记录用户查询自己绑定的账号

**日志示例:**
```
LIST - Group: 123456789, QQ: 987654321, Requesting bindings
LIST - Group: 123456789, QQ: 987654321, Result: 2 binding(s) - tourist, Petr
```

### 4. 列出所有绑定 (LISTALL)
记录管理员查询群内所有绑定的账号

**日志示例:**
```
LISTALL - Group: 123456789, Requesting user list
LISTALL - Group: 123456789, Result: 5 users with bindings
```

### 5. 命令接收 (COMMAND)
记录所有接收到的命令

**日志示例:**
```
COMMAND - Group: 123456789, User: 987654321, Command: bind cf tourist
COMMAND - Group: 123456789, User: 987654321, Command: list cf
```

### 6. 提交监控 (MONITOR)
记录定时任务检查提交的活动

**日志示例:**
```
MONITOR - Starting submission check
MONITOR - Checking 25 Codeforces accounts
MONITOR - New submission recorded: CF: tourist, Problem: 1234A, Submission ID: 98765432
MONITOR - Notifications sent to group: 123456789, 3 submission(s)
MONITOR - Submission check completed
```

## 配置文件 (Configuration File)

日志配置文件位于: `src/main/resources/logback-spring.xml`

如需修改日志配置，请编辑此文件。常见修改包括：
- 修改日志保留天数 (`<maxHistory>`)
- 修改最大文件大小 (`<totalSizeCap>`)
- 修改日志格式 (`<pattern>`)
- 修改日志级别 (`level="INFO"`)

## 注意事项 (Notes)

1. `logs/` 目录已添加到 `.gitignore`，不会提交到版本控制系统
2. 日志文件会自动创建，无需手动创建目录
3. 操作日志使用简化格式，便于解析和审计
4. 所有敏感操作（绑定、解绑）都会记录详细的成功/失败信息
5. 日志级别设置为 INFO，可根据需要调整为 DEBUG 以获取更详细的日志

## 查看日志 (Viewing Logs)

### 实时查看日志 (Tail Logs)
```bash
# 查看操作日志
tail -f logs/operations.log

# 查看通用日志
tail -f logs/kirakira.log
```

### 搜索日志 (Search Logs)
```bash
# 搜索特定用户的操作
grep "QQ: 987654321" logs/operations.log

# 搜索失败的绑定操作
grep "BIND.*FAILED" logs/operations.log

# 搜索今天的监控记录
grep "MONITOR" logs/operations.log | grep "2026-01-17"
```

### 统计操作 (Count Operations)
```bash
# 统计今天的绑定操作数
grep "BIND.*SUCCESS" logs/operations.log | grep "2026-01-17" | wc -l

# 统计失败的操作
grep "FAILED" logs/operations.log | wc -l
```
