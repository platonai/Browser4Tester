# Quick Start Guide - Browser4Tester

## 一分钟上手

### 前提条件
```bash
# 检查依赖
java -version    # 需要 JDK 17+
mvn -version     # 需要 Maven 3.6+
gh copilot       # 需要 GitHub CLI + Copilot
```

### 安装
```bash
cd /path/to/Browser4Tester
mvn clean package -DskipTests
```

### 运行示例
```bash
# 测试单个类
./bin/run-healer.sh /path/to/target-project com.example.MyTest

# 实际示例（已验证）
./bin/run-healer.sh ~/workspace/Browser4-4.6/pulsar-core/pulsar-dom \
    ai.platon.pulsar.dom.select.TestQueryParser
```

### 预期输出
```
=== Self-Healing Test Orchestrator ===
Target Project: /path/to/project
Test Classes: com.example.MyTest

✅ All classes passed.
```

或

```
⚠️ Unstable classes:
- com.example.FailingTest
```

## 工作原理

1. **执行** → 运行测试类
2. **失败** → 收集错误详情
3. **修复** → 调用 Copilot AI
4. **验证** → 检查完整性
5. **重试** → 最多 3 次
6. **回滚** → 失败则恢复

## 常见问题

### Q: Copilot 返回错误
```bash
# 确保已登录
gh auth status

# 确保 Copilot 可用
gh copilot -- -p "test"
```

### Q: 找不到测试类
确保：
- 类名是完全限定名（FQDN）
- 目标项目已编译（`mvn compile test-compile`）
- 测试文件位于标准位置（`src/test/kotlin/`）

### Q: 修复后仍然失败
- 检查 `config.maxRetryPerClass`（默认 3）
- 查看 Copilot 修复的代码是否合理
- 可能需要手动修复或调整提示词

## 配置选项

编辑 `Main.kt`:
```kotlin
OrchestratorConfig(
    maxRetryPerClass = 3,         // 最大重试次数
    allowMainSourceEdits = false, // 只修改测试代码
    testRoot = Path.of(".")       // 测试根目录
)
```

## 提示

💡 **最佳实践**
- 在本地开发时使用，不建议用于 CI
- 先在小项目上测试
- 审查 AI 修复的代码
- 使用版本控制（Git）

⚠️ **限制**
- 仅支持 Kotlin + JUnit 5
- 需要 GitHub Copilot 订阅
- API 调用有延迟（~10-60秒）
- 修复质量取决于错误信息的清晰度

🔒 **安全**
- 默认不修改生产代码
- 自动 Git 快照
- 完整性校验防护
- 失败自动回滚

## 下一步

- 查看 [README.md](README.md) 了解完整文档
- 查看 [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) 了解技术细节
- 查看 [browser4-test-healer/src](browser4-test-healer/src) 了解源码

## 支持

遇到问题？
1. 检查 `~/.copilot/logs/` 日志
2. 运行 `mvn -X` 查看详细输出
3. 查看项目 Issues

---

**Created**: 2026-02-15  
**Tested on**: Browser4-4.6/pulsar-core/pulsar-dom  
**Status**: ✅ Working
