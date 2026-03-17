# 版本配置说明

## 已安装的开发环境

### Java
- **版本**: OpenJDK 17.0.18 (Homebrew)
- **路径**: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- **配置**: 已添加到 `~/.zshrc`，作为默认 Java 版本

### Maven
- **版本**: Apache Maven 3.9.14
- **Java 版本**: 自动使用 Java 17（通过 JAVA_HOME 环境变量）

## 项目依赖版本

### 核心框架
- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Cloud**: 2023.0.0
- **Spring Cloud Alibaba**: 2022.0.0.0

### 主要依赖
- **Lombok**: 1.18.32
- **MyBatis Plus**: 3.5.5
- **MySQL Connector**: 8.0.33
- **Druid**: 1.2.20
- **Knife4j**: 4.4.0
- **JWT**: 0.12.3
- **Hutool**: 5.8.23
- **jqwik** (测试): 1.8.2

## 重要变更

### 1. 包名变更
- **javax.servlet** → **jakarta.servlet**
- **javax.validation** → **jakarta.validation**

这是因为 Spring Boot 3.x 使用 Jakarta EE 9+ 规范。

### 2. 编译器配置
lab-common 模块的 pom.xml 中添加了 Lombok 注解处理器配置：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.32</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3. 缺失的导入
在多个服务实现类中添加了：
```java
import java.util.stream.Collectors;
```

### 4. DTO 字段补充
- **MaterialInfo**: 添加了 `materialCode` 字段
- **ApprovalContext**: 添加了 `applicationNo`, `applicationId`, `businessType` 字段

## 使用说明

### 编译项目
```bash
# 编译所有模块（跳过测试）
mvn clean install -DskipTests -Dmaven.test.skip=true

# 编译单个模块
cd lab-service-inventory
mvn clean compile
```

### 运行测试
```bash
# 运行特定测试
mvn test -Dtest=NotificationMapperTest

# 运行所有测试
mvn test
```

### 验证环境
```bash
# 检查 Java 版本
java -version
# 应该显示: openjdk version "17.0.18"

# 检查 Maven 使用的 Java 版本
mvn -version
# 应该显示: Java version: 17.0.18
```

## 故障排除

### 如果 Maven 仍使用旧版本 Java
1. 重新加载 shell 配置：
   ```bash
   source ~/.zshrc
   ```

2. 或者重启终端

3. 验证 JAVA_HOME：
   ```bash
   echo $JAVA_HOME
   # 应该显示: /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
   ```

### 如果遇到 Lombok 编译错误
确保：
1. Lombok 版本是 1.18.32
2. 编译器插件配置了 annotationProcessorPaths
3. 使用 Java 17 编译

## 下一步

现在所有模块都可以成功编译。接下来可以：
1. 修复测试代码中的编译错误
2. 运行单元测试验证功能
3. 启动服务进行集成测试
