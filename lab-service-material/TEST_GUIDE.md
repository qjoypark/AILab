# 药品管理服务测试指南

## 属性测试说明

本模块包含基于属性的测试（Property-Based Testing），使用jqwik框架验证系统的正确性属性。

### 测试文件

- `MaterialPropertyTest.java` - 验证药品台账包含必需字段（属性4）

### 运行测试

#### 前提条件

1. 确保已安装Java 17或更高版本
2. 确保已安装Maven 3.6或更高版本
3. 确保数据库已启动（测试使用H2内存数据库，无需外部数据库）

#### 运行所有测试

```bash
cd lab-service-material
mvn test
```

#### 运行特定的属性测试

```bash
mvn test -Dtest=MaterialPropertyTest
```

#### 运行测试并生成报告

```bash
mvn test
# 查看测试报告
open target/surefire-reports/index.html
```

### 测试配置

**测试数据库**: H2内存数据库（自动创建和销毁）

**测试配置文件**: `src/test/resources/application-test.yml`

**数据库Schema**: `src/test/resources/schema.sql`

### 属性测试详情

#### 属性4：药品台账包含必需字段

**验证需求**: 5.1, 6.1, 6.2

**测试策略**:
- 生成100个随机药品记录
- 验证每个记录包含所有必需字段
- 对于危化品，额外验证危化品特有字段

**测试覆盖**:
- 耗材（materialType=1）
- 试剂（materialType=2）
- 危化品（materialType=3）

**验证字段**:

基本字段（所有药品类型）:
- materialName（药品名称）
- specification（规格）
- unit（单位）
- categoryId（分类ID）
- materialType（药品类型）

危化品额外字段（materialType=3）:
- casNumber（CAS号）
- dangerCategory（危险类别）
- isControlled（管控标识）

### 测试失败处理

如果属性测试失败，jqwik会提供：
1. 失败的具体输入（反例）
2. 失败的断言信息
3. 可重现的种子值

**示例失败输出**:
```
Property [materialRecordContainsRequiredFields] failed with sample:
  material = Material(materialCode=ABC123, materialName=Test, ...)
  
Assertion failed:
  Expected: not blank
  Actual: null
  
Seed: 1234567890
```

### 调试测试

#### 启用详细日志

在 `application-test.yml` 中设置：
```yaml
logging:
  level:
    com.lab: debug
    net.jqwik: debug
```

#### 固定随机种子

如果需要重现特定的测试失败，可以在测试类上添加：
```java
@Property(seed = "1234567890")
```

#### 减少迭代次数（快速调试）

```java
@Property(tries = 10)  // 默认是100
```

### 持续集成

在CI/CD流程中运行测试：

```bash
# 运行测试并生成报告
mvn clean test

# 检查测试结果
if [ $? -eq 0 ]; then
  echo "所有测试通过"
else
  echo "测试失败"
  exit 1
fi
```

### 测试最佳实践

1. **定期运行**: 每次代码变更后运行测试
2. **关注失败**: 属性测试失败通常表示业务逻辑问题
3. **保存反例**: 将失败的反例添加为单元测试
4. **更新测试**: 需求变更时及时更新属性测试

### 常见问题

**Q: 测试运行很慢？**
A: 属性测试默认运行100次迭代，可以临时减少tries参数进行快速验证。

**Q: H2数据库兼容性问题？**
A: H2配置为MySQL兼容模式，如果遇到SQL语法问题，检查schema.sql中的SQL语句。

**Q: 测试数据冲突？**
A: 每个测试方法都使用@Transactional注解，测试后会自动回滚，不会产生数据冲突。

**Q: 如何查看生成的测试数据？**
A: 在测试方法中添加日志输出，或者临时移除@Transactional注解并启用H2控制台。

### 下一步

完成测试后，可以：
1. 查看测试覆盖率报告
2. 继续实现其他模块的属性测试
3. 集成到CI/CD流程

### 相关文档

- [jqwik用户指南](https://jqwik.net/docs/current/user-guide.html)
- [属性测试最佳实践](https://jqwik.net/docs/current/user-guide.html#best-practices)
- [设计文档](../../.kiro/specs/smart-lab-management-system/design.md)
