# Implementation Notes - Browser4Tester

## 完成日期
2026-02-15

## 实现目标
创建一个自愈测试编排器 (Self-Healing Test Orchestrator)，能够自动修复失败的 JUnit 5 Kotlin 测试。

## 解决的问题

### 1. JUnit Platform API 集成
**问题**: 初始代码中 `LauncherDiscoveryRequestBuilder` 导入错误
```kotlin
// 错误
import org.junit.platform.launcher.LauncherDiscoveryRequestBuilder

// 正确
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
```

**解决**: 使用完全限定名 `org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()`

### 2. Maven 依赖配置
**问题**: 缺少 JUnit Jupiter Engine，导致运行时错误:
```
Cannot create Launcher without at least one TestEngine
```

**解决**: 在 `pom.xml` 中添加:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <version>${junit.jupiter.version}</version>
</dependency>
```

### 3. Fat JAR 打包
**问题**: 原始 jar 不包含 Kotlin runtime，导致 `NoClassDefFoundError: kotlin.jvm.internal.Intrinsics`

**解决**: 添加 maven-assembly-plugin 创建包含所有依赖的 fat jar:
```xml
<plugin>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
</plugin>
```

### 4. Git Snapshot 管理
**问题**: 当工作目录干净时，`git commit` 失败

**解决**: 在提交前检查是否有变更:
```kotlin
val status = runCaptureOutput("git", "status", "--porcelain")
if (status.isNotBlank()) {
    run("git", "commit", "-m", message)
}
```

### 5. GitHub Copilot CLI 集成
**问题**: Copilot CLI API 变更，`gh copilot suggest` 已被弃用

**解决**: 使用新的命令格式:
```bash
gh copilot -- -p "<prompt>" --allow-all-tools
```

### 6. Copilot 输出解析
**问题**: Copilot 返回的不仅是代码，还包含 markdown 格式和统计信息

**解决**: 实现智能解析器:
```kotlin
private fun extractCodeFromOutput(output: String): String {
    // 1. 尝试提取 markdown 代码块
    val codeBlockRegex = Regex("```(?:kotlin)?\\s*\\n([\\s\\S]*?)```")
    
    // 2. 尝试提取 package 到统计信息之间的内容
    val packageStart = output.indexOf("package ")
    val statsStart = output.indexOf("\nTotal usage est:")
    
    // 3. 回退方案：移除统计信息
}
```

## 测试结果

### 目标项目
- **项目**: Browser4-4.6
- **模块**: pulsar-core/pulsar-dom
- **测试类**: ai.platon.pulsar.dom.select.TestQueryParser

### 执行结果
```
=== Self-Healing Test Orchestrator ===
Target Project: /home/vincent/workspace/Browser4-4.6/pulsar-core/pulsar-dom
Test Classes: ai.platon.pulsar.dom.select.TestQueryParser

All classes passed.
```

✅ 所有测试通过
✅ 无需修复（测试已经正常）

## 架构亮点

1. **类级隔离**: 使用 JUnit Platform Launcher API 精确执行单个测试类
2. **失败收集**: 自定义 `TestExecutionListener` 捕获方法级失败详情
3. **AI 集成**: 通过 GitHub Copilot CLI 进行智能修复
4. **完整性保护**: `TestIntegrityGuard` 防止测试质量退化
5. **Git 安全**: 自动快照和回滚机制

## 待改进项

1. **Copilot 输出解析**: 当前的正则表达式可能不够健壮
2. **日志输出**: 应该记录修复过程的详细信息
3. **并行执行**: 当前是串行执行，可以考虑并行
4. **框架支持**: 目前只支持 JUnit 5，可扩展到 TestNG、Spock
5. **语言支持**: 当前专注于 Kotlin，可扩展到 Java
6. **进度报告**: 应该提供更详细的进度反馈

## 关键文件

- `ClassExecutor.kt` - JUnit 测试执行器
- `CopilotAgent.kt` - Copilot CLI 集成
- `TestOrchestrator.kt` - 主编排逻辑
- `GitSnapshotManager.kt` - Git 快照管理
- `PatchApplier.kt` - 文件修补应用
- `TestIntegrityGuard.kt` - 测试完整性验证
- `run-healer.sh` - 运行脚本

## 性能考虑

- Copilot API 调用延迟: ~10-60秒
- 每次修复重试成本: 测试执行 + API 调用
- 建议最大重试次数: 3次（已配置）

## 安全考虑

1. **只修改测试代码**: 默认 `allowMainSourceEdits = false`
2. **完整性校验**: 防止删除断言或测试方法
3. **Git 回滚**: 失败时自动恢复
4. **无限制提示词**: 堆栈跟踪截断为 500 字符防止过大

## 使用建议

1. **在 CI 前使用**: 作为本地开发辅助工具
2. **不建议用于 CI**: API 延迟和成本问题
3. **监控修复质量**: 人工审查 AI 修复的代码
4. **逐步迭代**: 从小范围测试集开始

## 许可证考虑

- 需要 GitHub Copilot 订阅
- API 调用计入使用配额
- 确保符合 GitHub 服务条款
