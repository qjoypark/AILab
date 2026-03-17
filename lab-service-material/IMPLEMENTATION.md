# 药品基础信息管理模块实现文档

## 概述

本文档描述了任务4（药品基础信息管理模块）的完整实现，包括数据库表创建、实体类、服务层、控制器层以及属性测试。

## 实现的子任务

### 4.1 创建药品管理数据表 ✅

**文件**: `sql/04_material_info.sql`

创建了以下数据表：
- `material_category`: 药品分类表（支持树形结构）
- `material`: 药品信息表（包含耗材、试剂、危化品）
- `supplier`: 供应商表

**初始数据**:
- 插入了三个一级分类：耗材、试剂、危化品
- 为每个一级分类插入了相应的二级分类

### 4.2 实现药品分类管理 ✅

**实现文件**:
- Entity: `MaterialCategory.java`
- DTO: `MaterialCategoryDTO.java`
- Mapper: `MaterialCategoryMapper.java`
- Service: `MaterialCategoryService.java`, `MaterialCategoryServiceImpl.java`
- Controller: `MaterialCategoryController.java`

**功能**:
- 查询分类树（支持递归构建树形结构）
- 创建分类（验证编码唯一性）
- 更新分类（验证编码唯一性）
- 删除分类（检查是否有子分类）

**API接口**:
- `GET /api/v1/material-categories/tree` - 查询分类树
- `POST /api/v1/material-categories` - 创建分类
- `PUT /api/v1/material-categories/{id}` - 更新分类
- `DELETE /api/v1/material-categories/{id}` - 删除分类

### 4.3 实现药品信息管理接口（CRUD） ✅

**实现文件**:
- Entity: `Material.java`
- DTO: `MaterialDTO.java`
- Mapper: `MaterialMapper.java`
- Service: `MaterialService.java`, `MaterialServiceImpl.java`
- Controller: `MaterialController.java`

**功能**:
- 分页查询药品列表（支持按类型、分类、关键词筛选）
- 查询药品详情（包含分类名称、供应商名称）
- 创建药品（验证编码唯一性、分类存在性、供应商存在性）
- 更新药品（同创建验证）
- 删除药品（逻辑删除）

**危化品特殊验证**:
- 药品类型为3（危化品）时，必须填写：
  - CAS号（casNumber）
  - 危险类别（dangerCategory）
  - 管控标识（isControlled）

**API接口**:
- `GET /api/v1/materials` - 分页查询药品列表
- `GET /api/v1/materials/{id}` - 查询药品详情
- `POST /api/v1/materials` - 创建药品
- `PUT /api/v1/materials/{id}` - 更新药品
- `DELETE /api/v1/materials/{id}` - 删除药品

### 4.4 编写药品信息属性测试 ✅

**测试文件**: `MaterialPropertyTest.java`

**测试属性**: 属性4 - 药品台账包含必需字段

**验证需求**: 5.1, 6.1, 6.2

**测试内容**:
- 对于任何新创建的药品记录，验证包含所有必需字段：
  - 名称（materialName）
  - 规格（specification）
  - 单位（unit）
  - 分类（categoryId）
  - 药品类型（materialType）
- 对于危化品（materialType=3），额外验证：
  - CAS号（casNumber）
  - 危险类别（dangerCategory）
  - 管控标识（isControlled）

**测试配置**:
- 使用jqwik框架
- 运行100次迭代
- 使用H2内存数据库
- 支持事务回滚

**数据生成器**:
- 生成1-3之间的药品类型
- 为危化品生成额外的必需字段
- 使用合理的字符串长度和字符集

### 4.5 实现供应商管理接口 ✅

**实现文件**:
- Entity: `Supplier.java`
- DTO: `SupplierDTO.java`
- Mapper: `SupplierMapper.java`
- Service: `SupplierService.java`, `SupplierServiceImpl.java`
- Controller: `SupplierController.java`

**功能**:
- 分页查询供应商列表（支持关键词搜索）
- 查询供应商详情
- 创建供应商（验证编码唯一性）
- 更新供应商（验证编码唯一性）
- 删除供应商（逻辑删除）

**API接口**:
- `GET /api/v1/suppliers` - 分页查询供应商列表
- `GET /api/v1/suppliers/{id}` - 查询供应商详情
- `POST /api/v1/suppliers` - 创建供应商
- `PUT /api/v1/suppliers/{id}` - 更新供应商
- `DELETE /api/v1/suppliers/{id}` - 删除供应商

### 4.6 实现文件上传功能（MinIO） ✅

**实现文件**:
- Config: `MinioConfig.java`
- Service: `FileUploadService.java`, `FileUploadServiceImpl.java`
- Controller: `FileUploadController.java`

**功能**:
- 文件上传（支持图片和文档）
- 文件类型检查（jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx）
- 文件大小限制（最大10MB）
- 自动创建bucket
- 设置bucket为公开读
- 生成唯一文件名（UUID）
- 返回文件访问URL

**API接口**:
- `POST /api/v1/files/upload` - 上传文件

**配置**:
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: lab-files
```

## 技术实现细节

### 数据库设计

**药品类型枚举**:
- 1: 耗材
- 2: 试剂
- 3: 危化品

**管控标识枚举**:
- 0: 非管控
- 1: 易制毒
- 2: 易制爆

**状态枚举**:
- 0: 停用
- 1: 启用

### 业务逻辑

**药品创建/更新验证流程**:
1. 验证药品编码唯一性
2. 验证分类是否存在
3. 验证供应商是否存在（如果提供）
4. 如果是危化品，验证危化品必填字段
5. 执行插入/更新操作

**分类删除验证**:
- 检查是否有子分类
- 如果有子分类，拒绝删除

**文件上传流程**:
1. 验证文件不为空
2. 验证文件大小（≤10MB）
3. 验证文件类型
4. 确保bucket存在
5. 生成唯一文件名
6. 上传到MinIO
7. 返回文件URL

### 依赖配置

**核心依赖**:
- Spring Boot 3.x
- MyBatis-Plus（数据访问）
- MinIO（对象存储）
- Knife4j（API文档）
- jqwik（属性测试）
- H2（测试数据库）

### 配置文件

**application.yml**:
- 服务端口：8082
- 数据库连接
- Redis配置
- MinIO配置
- MyBatis-Plus配置
- Knife4j配置

**application-test.yml**:
- H2内存数据库配置
- 测试日志配置

## API文档

启动服务后，访问 Knife4j API文档：
```
http://localhost:8082/doc.html
```

## 测试

### 运行属性测试

```bash
mvn test -Dtest=MaterialPropertyTest
```

### 测试覆盖

- ✅ 药品记录必需字段验证
- ✅ 危化品特殊字段验证
- ✅ 数据生成器覆盖所有药品类型

## 数据库初始化

执行SQL脚本：
```bash
mysql -u root -p lab_management < sql/04_material_info.sql
```

## 后续任务

本模块已完成所有子任务（4.1-4.6），可以继续进行：
- 任务5：库存管理模块
- 任务6：库存预警模块
- 任务8：领用申请与审批模块

## 注意事项

1. **危化品管理**：危化品必须填写CAS号、危险类别和管控标识，系统会自动验证
2. **文件上传**：支持的文件类型有限，最大10MB
3. **逻辑删除**：所有删除操作都是逻辑删除，数据不会真正从数据库中删除
4. **分类树**：支持多级分类，删除父分类前需要先删除子分类
5. **编码唯一性**：药品编码、分类编码、供应商编码都必须唯一

## 验收标准

根据需求5.1, 6.1, 6.2，本模块实现了：

✅ **需求5.1**：维护耗材和试剂台账
- 包含名称、规格、单位、库存数量、存放位置、供应商、单价
- 支持分类管理
- 支持供应商管理

✅ **需求6.1**：维护危化品台账
- 包含化学品名称、CAS号、危险类别、存储位置、数量、责任人
- 标识易制毒易制爆物品（isControlled字段）

✅ **需求6.2**：危化品申请必需字段
- 系统强制验证危化品的CAS号、危险类别、管控标识
- 创建和更新时自动验证

## 总结

任务4（药品基础信息管理模块）已全部完成，包括：
- 3个数据表（material_category, material, supplier）
- 3个实体类和对应的DTO
- 3个Mapper接口
- 6个Service接口和实现
- 4个Controller
- 1个MinIO配置和文件上传服务
- 1个属性测试（100次迭代）
- 完整的API文档

所有功能已实现并通过验证，可以进入下一阶段开发。
