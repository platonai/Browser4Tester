# Self-Healing Test Orchestrator For Browser4

目标明确：

> ✅ 每个测试类单独执行
> ❌ 若失败 → 立即调用 GitHub Copilot CLI 自动修复
> 🔁 修复后重新执行该类
> ➜ 成功后再执行下一个类

这不是 CI，而是一个“开发期 AI 修复管线”。

---

# 一、总体架构

```
Test Class Iterator
        │
        ▼
Class Executor (JUnit Platform Launcher)
        │
   ┌────┴────┐
   │ PASS     │→ 下一个类
   │ FAIL     │
   └────┬────┘
        ▼
Failure Collector
        ▼
Copilot CLI Repair Agent
        ▼
Patch Apply + Git Stage
        ▼
Re-run Same Class
        │
   (max retry N)
```

核心原则：

* 严格类级隔离
* 严格失败上下文收集
* 可回滚
* 限制重试次数（避免 AI 死循环）

---

# 二、执行模型选择

你现在是：

* Kotlin + Spring Boot 3
* Maven multi-module
* JUnit 5
* GitHub Actions
* CDP 驱动

所以推荐方案：

> 用 JUnit Platform Launcher API + 独立 Orchestrator 程序

不要用 `mvn test`。

原因：

* Maven 级别太粗
* 无法精准控制单类
* 不利于逐类 AI 修复

---

# 三、类级执行器

## 方式 1（推荐）

使用 JUnit Platform Launcher：

```kotlin
val request = LauncherDiscoveryRequestBuilder.request()
    .selectors(selectClass("com.example.MyTest"))
    .build()

val launcher = LauncherFactory.create()
launcher.execute(request, listener)
```

监听器收集：

* 失败测试方法
* stacktrace
* assertion message

输出 JSON：

```json
{
  "class": "MyTest",
  "failures": [
    {
      "method": "shouldLogin",
      "message": "...",
      "stacktrace": "..."
    }
  ]
}
```

---

# 四、Copilot CLI 调用策略

## 核心问题

Copilot CLI 不是专门做“自动修复”的，需要明确 prompt。

### 调用方式

```bash
gh copilot suggest -p "<PROMPT>" --output json
```

或

```bash
gh copilot explain
```

（你需要封装一个 CLI adapter）

---

# 五、修复 Prompt 设计（关键）

必须提供：

1. 测试类源码
2. 失败方法源码
3. 报错堆栈
4. 相关业务类源码（可选）
5. 修复要求

示例 Prompt：

```
You are fixing a failing JUnit 5 test.

Test class:
<full class code>

Failure:
<stacktrace>

Constraints:
- Do not change production code unless necessary.
- Prefer fixing test logic.
- Keep style consistent.
- Return FULL updated file.
```

一定要要求：

> 返回完整文件，而不是 diff

否则 patch 应用复杂度很高。

---

# 六、Patch 应用策略

建议：

* Copilot 输出完整文件
* 覆盖原文件
* 自动格式化（ktlint）
* git add

### 安全措施

* 在修复前 git commit snapshot
* 若修复 N 次失败 → 回滚

```bash
git commit -am "pre-ai-fix snapshot"
```

---

# 七、控制循环

必须防止无限修复：

```text
maxRetryPerClass = 3
```

流程：

```
run class
if pass → next
if fail:
    for i in 1..3:
        aiFix()
        rerun
        if pass break
    if still fail:
        mark unstable
        continue next class
```

---

# 八、执行顺序策略

默认顺序：

```
按类名排序
```

更高级策略：

* 先跑快测试
* 再跑 integration
* 排除 @MustRunExplicitly

你之前的 Test Taxonomy 可以直接集成。

---

# 九、工程化实现模块

建议新建模块：

```
browser4-test-healer
```

内部结构：

```
TestOrchestrator.kt
CopilotAgent.kt
PatchApplier.kt
GitSnapshotManager.kt
FailureCollector.kt
```

---

# 十、风险分析（必须严肃）

### 1️⃣ AI 修改生产代码风险

建议默认策略：

```
只允许修改 test 目录
```

生产代码必须加白名单开关。

---

### 2️⃣ 语义退化风险

AI 可能：

* 删除断言
* 放宽判断
* 改成 assertTrue(true)

解决：

增加 “测试完整性校验器”

例如：

* 断言数量不可减少
* 不允许删除测试方法
* 不允许 empty test

---

### 3️⃣ 隐性行为改变

如果测试是 CDP 驱动：

AI 可能改等待逻辑，导致 flaky。

建议：

* 强制使用统一 Wait DSL
* 在 prompt 中声明

---

# 十一、增强版：分层修复策略

更专业的做法：

## 第一阶段：Test-Only 修复

只允许修改：

```
src/test
```

## 第二阶段（可选）：

允许修改：

```
src/main
```

但必须人工审批。

---

# 十二、是否值得做？

### 优点

* 加速 TDD
* 自动消除低级错误
* 可做为“开发辅助模式”

### 缺点

* 可能污染代码
* 需要强约束
* 可能引入隐藏 bug

---

# 十三、进阶版本（企业级）

可以升级为：

> AI Gatekeeper Pipeline

结合：

* 静态分析（detekt / spotless）
* 覆盖率校验
* mutation test

只有满足质量门禁才允许进入下一类。

---

# 十四、极简 MVP 版本

如果你要快速落地：

1. Kotlin 主程序
2. 读取 test classes 列表
3. 逐个 `Launcher` 执行
4. 失败 → 调用 shell `gh copilot`
5. 覆盖文件
6. 重跑

一周内可完成。

---

# 结论

这是一个：

> Class-Level Self-Healing Test Execution Engine

从工程架构角度看：

* 技术上完全可行
* 关键在约束和质量防护
* 必须防止 AI 破坏测试语义
