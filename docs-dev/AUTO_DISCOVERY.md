# Auto-Discovery Mode Guide

## 概述

`run-healer.sh` 脚本现在支持自动发现模式，可以：
- 自动发现项目中的所有模块
- 按 Maven 依赖顺序执行测试
- 自动发现每个模块中的所有测试类
- 批量执行并报告结果

## 使用方法

### 模式 1：自动模式（推荐）

测试整个项目的所有模块和测试类：

```bash
./bin/run-healer.sh /path/to/target-project
```

**示例**：
```bash
# 测试整个 Browser4-4.6 项目
./bin/run-healer.sh ~/workspace/Browser4-4.6

# 测试单个模块的所有测试
./bin/run-healer.sh ~/workspace/Browser4-4.6/pulsar-core/pulsar-dom
```

### 模式 2：手动模式

指定特定的测试类：

```bash
./bin/run-healer.sh /path/to/project com.example.Test1 com.example.Test2
```

## 自动模式工作流程

1. **发现模块** - 使用 Maven reactor 按依赖顺序列出所有模块
2. **编译模块** - 依次编译每个模块的测试代码
3. **发现测试** - 在 `src/test/` 下查找所有测试类文件
4. **批量执行** - 将模块的所有测试类一次性提交给 healer
5. **报告结果** - 汇总每个模块和整体的测试结果

## 依赖顺序

脚本使用 Maven 命令自动确定模块依赖顺序：

```bash
mvn -q exec:exec -Dexec.executable=pwd -Dexec.workingdir='${project.basedir}'
```

这确保了：
- 依赖模块先于被依赖模块测试
- 编译顺序正确
- classpath 构建正确

## 输出示例

```
=== Self-Healing Test Orchestrator ===
Target Project: /home/user/workspace/MyProject

🤖 Mode: Auto (discovering all modules and tests)

🔍 Discovering modules in dependency order...
✓ Found 5 module(s)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Processing Module 1 of 5
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📦 Module: common
   Path: /home/user/workspace/MyProject/common
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔨 Compiling module...
🔍 Discovering test classes...
✓ Found 10 test class(es)

  🧪 Testing 10 classes...
     ✅ All tests passed

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Module Summary: common
✅ All tests passed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[... 其他模块 ...]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 All modules processed!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total: 5 modules
Passed: 4 modules
Failed: 1 modules

Failed modules:
  - api-module
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## 测试类发现规则

脚本会在以下位置查找测试类：

1. `src/test/kotlin/` - Kotlin 测试
2. `src/test/java/` - Java 测试

文件名模式：
- `*Test*.kt`
- `*Test*.java`

例如会匹配：
- `MyTest.kt`
- `TestMyFeature.kt`
- `MyFeatureTests.kt`
- `MyTestCase.java`

## 性能考虑

### 批量执行
- 每个模块的所有测试类会一次性提交
- 减少 JVM 启动开销
- 共享 classpath 构建

### 编译策略
- 每个模块单独编译一次
- 使用 `mvn test-compile -q -DskipTests`
- 不执行实际测试，只编译

## 跳过条件

模块会在以下情况被跳过：

1. **无 pom.xml** - 不是 Maven 模块
2. **无 src/test** - 没有测试目录
3. **无测试类** - 目录为空或没有匹配的测试文件

## 故障处理

### 编译失败
- 显示警告但继续执行
- 可能导致后续测试失败

### 测试失败
- 模块标记为失败
- 继续处理下一个模块
- 最终报告中列出失败的模块

### 超时处理
- 每个测试类有独立的 AI 修复超时
- 模块级别没有硬性超时
- 可以使用 `timeout` 命令包装：
  ```bash
  timeout 1800 ./bin/run-healer.sh /path/to/project
  ```

## 实际测试结果

### 测试项目：Browser4-4.6

```bash
./bin/run-healer.sh ~/workspace/Browser4-4.6
```

发现的模块（按依赖顺序）：
1. pulsar-dependencies (无测试)
2. Browser4-4.6 (父 POM，无测试)
3. pulsar-browser (14 测试类)
4. pulsar-common (3 测试类)
5. pulsar-dom (2 测试类)
6. pulsar-persist (N 测试类)
7. pulsar-ql-common (N 测试类)
8. pulsar-third (N 测试类)
9. ... 等

## 配置选项

### 修改测试类模式

编辑脚本中的 `discover_test_classes` 函数：

```bash
find "$test_dir" -type f \( -name "*Test*.kt" -o -name "*Test*.java" \)
```

### 修改编译参数

在 `run_module_tests` 函数中：

```bash
mvn test-compile -q -DskipTests
```

可以添加：
- `-T 1C` - 多线程编译
- `-o` - 离线模式
- `-U` - 强制更新快照

## 最佳实践

1. **首次运行**
   ```bash
   cd /path/to/target-project
   mvn clean install -DskipTests  # 确保依赖已安装
   ```

2. **大型项目**
   - 先测试单个模块验证
   - 使用 `timeout` 防止无限等待
   - 考虑分批次执行

3. **CI 集成**
   - 不建议在 CI 中使用（API 延迟）
   - 作为本地开发辅助工具
   - 可以在 pre-commit hook 中使用

4. **调试**
   ```bash
   # 保留详细输出
   ./bin/run-healer.sh /path/to/project 2>&1 | tee healer-run.log
   
   # 只测试特定模块
   ./bin/run-healer.sh /path/to/project/specific-module
   ```

## 限制

1. **仅支持 Maven 项目**
   - 需要 `pom.xml`
   - 使用 Maven 标准目录结构

2. **Kotlin/Java + JUnit 5**
   - 测试框架限制
   - 不支持 TestNG、Spock 等

3. **单线程执行**
   - 按顺序处理模块
   - 未来可考虑并行化

4. **内存占用**
   - 每个模块启动独立 JVM
   - 大型项目可能需要较长时间

## 故障排查

### 问题：找不到测试类

**原因**：
- 文件名不匹配模式
- 目录结构不标准

**解决**：
```bash
# 手动检查
find /path/to/module/src/test -name "*.kt" -o -name "*.java"
```

### 问题：编译失败

**原因**：
- 依赖未安装
- 模块依赖顺序错误

**解决**：
```bash
cd /path/to/project
mvn clean install -DskipTests
```

### 问题：Maven reactor 失败

**原因**：
- Maven 版本太旧
- POM 格式错误

**解决**：
脚本会自动回退到解析 `<modules>` 标签

## 未来改进

- [ ] 并行模块执行
- [ ] Gradle 支持
- [ ] 自定义测试模式
- [ ] 增量测试（只测试变更的模块）
- [ ] HTML 报告生成
- [ ] 集成到 IDE

---

**更新日期**: 2026-02-16  
**版本**: 2.0
