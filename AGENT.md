# 项目协作指南

## 沟通与执行原则

- 所有对话、说明和交付汇报使用中文。
- 开始实现前说明假设；需求存在影响实现的歧义时，先指出并确认，不默默猜测。
- 多步任务先给简要计划，每一步写明验证方式；小改动直接处理。
- 使用满足需求的最少代码，不新增需求之外的接口、抽象、配置或依赖。
- 仅修改与当前任务直接相关的文件，沿用现有风格，不顺手重构、格式化或删除原有死代码。
- 删除本次修改产生的无用 import、变量和方法。
- 修改完成后统一汇总改动、验证结果及未完成事项，不逐处汇报。
- 代码审查只输出最终结论，按“必须修复 / 建议清理 / 记录但暂不改”分组，注明文件位置和原因，不逐条播报审查过程。

## 项目与事实来源

本项目为苍穹外卖 Java 后端，采用 Maven 多模块结构，包名前缀为 `com.sky`。

- 修改前阅读相关源码、模块 POM 和配置，以当前文件判断实现状态。
- 根目录的《苍穹外卖-开发计划方案.md》用于了解目标、阶段和接口约束；其中的基线描述可能落后于源码，不可直接作为当前完成情况。
- 计划文件不是一次性实施全部功能的授权，只完成当前明确要求的范围。
- 对具体接口进行开发时，查阅原始接口文档并核对请求与响应；文档缺失或与需求冲突时，明确指出。
- 当前主要业务实现集中在员工模块，包含登录、退出、新增、分页及启停用代码。其他领域存在 DTO、VO 和实体，不代表对应接口已完成或验证通过。

## 目录与职责

| 路径 | 职责 |
| --- | --- |
| `pom.xml` | 聚合三个模块，集中管理依赖版本 |
| `sky-common/src/main/java/com/sky` | 常量、异常、上下文、配置属性、工具、JSON 与统一返回类型 |
| `sky-pojo/src/main/java/com/sky/entity` | 数据库实体 |
| `sky-pojo/src/main/java/com/sky/dto` | 请求参数对象 |
| `sky-pojo/src/main/java/com/sky/vo` | 响应对象 |
| `sky-server/src/main/java/com/sky/controller` | 接口入口，当前员工接口位于 `admin` 子包 |
| `sky-server/src/main/java/com/sky/service` | 业务接口，实现在 `impl` 子包 |
| `sky-server/src/main/java/com/sky/mapper` | MyBatis Mapper 接口 |
| `sky-server/src/main/java/com/sky/config` | Spring 与 Web MVC 配置 |
| `sky-server/src/main/java/com/sky/interceptor` | 请求鉴权拦截器 |
| `sky-server/src/main/java/com/sky/handler` | 全局异常处理 |
| `sky-server/src/main/resources/mapper` | MyBatis XML 映射 |
| `sky-server/src/main/resources/application.yml` | 公共配置及环境配置引用 |
| `sky-server/src/main/resources/application-dev.yml` | 开发环境配置 |
| `苍穹外卖-开发计划方案.md` | 后续开发规划、接口约束和阶段验收参考 |

`sky-server` 依赖 `sky-common` 和 `sky-pojo`。不要将 Controller、Service 或数据库访问代码放入公共模块。

## 技术边界

- 沿用 Spring Boot `2.7.3`、MyBatis、MySQL、Druid、PageHelper、Lombok、JWT 与 Knife4j 的现有组合。
- 依赖版本以 POM 为准；没有明确需求不升级框架、不替换持久层、不引入新依赖。
- 当前代码使用 `javax.servlet`；不要直接套用 Spring Boot 3 的 `jakarta.servlet` 写法。
- POM 中存在 Redis、缓存、WebSocket、POI 等依赖，公共模块中存在 OSS、微信支付工具；使用前确认相应配置和业务接线是否已完成。
- Java 版本以实际构建配置及本地环境为准，未确认前避免引入高版本专属语法。

## 编码约定

- 保持 Controller → Service → Mapper 分层。Controller 负责参数接收和结果包装，Service 处理业务规则与事务，Mapper 负责数据库访问。
- 优先复用已有 DTO、VO、实体、常量、异常和工具类，先检查现有字段与行为是否满足需求。
- 沿用现有 Lombok、依赖注入和命名习惯，不为风格统一改写无关代码。
- 业务失败复用 `BaseException` 体系及 `MessageConstant`，由 `GlobalExceptionHandler` 统一处理；不要在每个接口重复捕获所有异常。
- 涉及多条关联写入且需要原子性的业务在 Service 层使用事务。
- 简单 SQL 可沿用 Mapper 注解；动态 SQL 放在 XML，确保 `namespace`、语句 `id` 与接口方法对应。
- SQL 参数使用 `#{...}` 绑定，不拼接来自请求的 SQL 内容。
- 已开启数据库下划线字段到 Java 驼峰字段映射，不重复添加无必要的映射配置。
- 分页沿用 `PageHelper.startPage(...)`、Mapper 查询与 `PageResult`，分页设置应紧邻目标查询。
- 涉及审计字段时使用当前登录上下文及现有时间类型；公共字段自动填充仍属于计划能力，使用前确认实际实现。

## 接口与鉴权

- 返回值统一使用 `Result<T>`：`code=1` 表示成功，`code=0` 表示业务失败，字段为 `code`、`msg`、`data`。
- 分页数据使用 `PageResult`，字段为 `total`、`records`。
- 保持既定接口路径、HTTP 方法、参数名称、参数位置及响应字段，与接口文档和现有前端契约一致。
- 当前员工接口前缀为 `/admin/employee`。
- 当前管理端拦截 `/admin/**`，放行 `/admin/employee/login`，令牌请求头由 `sky.jwt.admin-token-name` 配置，当前为 `token`；鉴权失败返回 HTTP 401。
- `JwtTokenAdminInterceptor` 已把员工 ID 写入 `BaseContext`。涉及请求上下文生命周期的修改时，同时检查 ThreadLocal 的清理，避免线程复用导致身份残留。
- 用户端 JWT 是计划能力；不要把属性类中存在用户端字段当作用户端鉴权已经接入。
- 时间格式按接口契约使用 `yyyy-MM-dd HH:mm:ss`。已有 `JacksonObjectMapper` 可复用，但修改序列化前应确认它是否已注册到消息转换器。
- 不将密码、完整令牌、数据库凭据、支付密钥等敏感信息写入新增日志、示例或交付文档。

## 构建与运行

以下命令均在项目根目录执行，使用本机 Maven 与 JDK；当前未发现 Maven Wrapper。

```powershell
# 编译服务及其依赖模块
mvn -pl sky-server -am compile

# 执行测试
mvn -pl sky-server -am test

# 打包服务及其依赖模块
mvn -pl sky-server -am package

# 打包成功后启动服务
java -jar sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

- 入口类为 `com.sky.SkyApplication`。
- 当前默认环境为 `dev`，端口为 `8080`；启动前确认开发环境的 MySQL 连接及数据库结构可用。
- Knife4j 文档入口为 `http://localhost:8080/doc.html`，访问需以服务实际启动成功为前提。
- 涉及 Redis、OSS、微信等功能时，先确认对应服务与配置。不要为了通过启动或测试擅自替换真实配置、关闭鉴权。
- 当前源码资源目录未发现数据库初始化脚本；计划中的 `resources/db/sky.sql` 不应被描述成现有文件。

## 验证与交付

- 仅修改文档时，核对事实、路径、命令和范围，无需运行完整业务测试。
- 修改 Java 或 Mapper 时，至少执行相关模块的编译；修复业务问题时，优先增加或运行能复现问题的针对性测试。
- 当前未发现测试源码；`mvn test` 成功但没有执行测试，不代表业务验证通过。
- `.gitignore` 当前忽略 `*Test.java` 和 `**/test/`。新增测试需检查交付是否包含测试文件；若需调整忽略规则，只做与本次测试相关的最小修改。
- SQL、分页、事务和鉴权改动不能只依靠编译验证；根据改动检查实际查询、边界条件、非法输入及未登录请求。
- 集成验证依赖数据库或外部服务时，说明实际使用的环境、执行结果和未覆盖项。不要将“未执行”写成“通过”。
- 不手工修改或交付 `target/`、`.class`、IDE 配置等构建产物。
- 交付汇报包括改动文件与行为、验证方式与结果，以及仍需处理的阻碍；不附带无关清理。
