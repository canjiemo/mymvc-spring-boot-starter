# mymvc-spring-boot-starter

一个轻量级的 Spring Boot MVC 增强 Starter，提供统一响应封装、全局异常处理、增强参数校验、隐私字段脱敏等功能。

## JDK 25 本地开发版本

当前分支 `feature/jdk25` 使用 `2.0.0-jdk25-SNAPSHOT`，最低编译及运行版本为 JDK 25。历史 JDK 21 版本保留在原分支及原发布坐标中。

当前适配目标为 Eclipse Temurin 25、Spring Boot 4.1.x、Spring Framework 7 和 Lombok 1.18.46，构建固定 Spring Boot 4.1.1。

依赖由 Boot 4.1.1 BOM 管理，默认 JSON 使用 Jackson 3；保留 Jackson 2 的可选脱敏适配。当前分支服务 Servlet MVC，不承诺 Boot 3 或 WebFlux 兼容。原 JDK 21／Boot 3 发布坐标保留。

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25) # macOS；其他环境设置为实际 JDK 25 路径
mvn clean install -Dgpg.skip=true
```

本仓库显式配置 Lombok 与 Spring Boot 配置元数据处理器。使用方自行声明的 Lombok DTO 也需要配置注解处理器，否则 JDK 25 下可能缺少 getter，影响 JSON 序列化。正式版本为 `2.0.0-jdk25`，在适配和联调稳定后单独发布。

### Boot 4.1 联合验证

验证日期：2026-10-04；环境：Temurin 25.0.4.1+1／Maven 3.9.11。Boot 4.1.0 与 4.1.1 均运行组件测试及联合测试，未来补丁需按相同步骤重新验证后固定版本。

| 验证对象 | 测试数 | 主要覆盖 |
| --- | ---: | --- |
| MyJDBC | 196 | SQL 改写、逻辑删除、租户隔离、审计填充及查询 |
| MyMVC | 24 | 响应、异常、校验、Jackson 3 脱敏及 Jackson 可选依赖隔离 |
| MyDict | 38 | 缓存、JDK 25 注解处理、Lombok 联合生成及 Boot 自动配置 |
| 三组件联合项目 | 11 | 真实 Tomcat HTTP、自动装配、租户过滤、事务回滚、审计填充、生成字段输出、字符串大整数 ID、脱敏、AOP 和 Actuator 指标 |

联合消费项目位于 [integration-tests/boot41](integration-tests/boot41/pom.xml)。先在三个组件仓库分别使用 JDK 25 执行 `mvn clean install -Dgpg.skip=true`，然后在本仓库根目录执行：

```bash
mvn -f integration-tests/boot41/pom.xml clean verify
# 可选：复核已验证的另一个 4.1 补丁
mvn -f integration-tests/boot41/pom.xml clean verify -Dspring-boot.version=4.1.0
```

测试通过标准自动配置发现三个组件，使用 Boot 自动配置的数据源与随机本地端口，仅作为组件兼容测试。H2 测试表在 Java 测试配置中初始化，不附 SQL 或 Python 文件。真实 PostgreSQL、Redis、业务权限、密码设备以及医院部署环境仍需建项目时验证。

## 特性

- 🚀 **统一响应封装** - 提供 `MyResponseResult<T>` 统一返回格式
- 🛡️ **全局异常处理** - 默认自动注册 `@RestControllerAdvice`，也兼容 `MyBaseController`
- ✅ **增强参数校验** - 提供 5 种开箱即用的自定义校验器
- 🔒 **隐私字段脱敏** - `@Privacy` 注解自动遮蔽响应 DTO 中的敏感字段，支持 Jackson / Fastjson / Fastjson2
- 🎯 **零侵入集成** - 基于 Spring Boot 自动装配，引入即可使用
- ⚡ **轻量灵活** - optional 依赖设计，容器中立，支持配置化覆盖默认行为

## 快速开始

### Maven 依赖

```xml
<!-- 1. 添加本 Starter -->
<dependency>
    <groupId>io.github.canjiemo</groupId>
    <artifactId>mymvc-spring-boot-starter</artifactId>
    <version>2.0.0-jdk25-SNAPSHOT</version>
</dependency>

<!-- 2. 确保项目中有 spring-boot-starter-webmvc -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- 3. 如果需要 @Valid / @Validated 参数校验，补充以下依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

> **说明**：
> 1. 本 Starter 使用 `optional` 依赖，不会强制引入 Spring 相关依赖，避免版本冲突。
> 2. `MyBaseController` 的异常输出约定是 HTTP 层固定返回 `200`，业务成功与失败通过响应体中的 `code` 表达。
> 3. `@Valid` / `@Validated` 相关能力依赖 `spring-boot-starter-validation`；方法参数切面还需要 `spring-boot-starter-aspectj`。
> 4. 全局异常处理默认自动启用；如果你只需要异常兜底，不必继承 `MyBaseController`。

### 基本使用

#### 1. 统一响应格式

继承 `MyBaseController` 使用便捷的响应方法：

```java
@RestController
@RequestMapping("/api/users")
public class UserController extends MyBaseController {

    // 返回数据
    @GetMapping("/{id}")
    public MyResponseResult<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return doJsonOut(user);
    }

    // 返回分页数据
    @GetMapping("/page")
    public MyResponseResult<Page<User>> listUsers(@RequestParam int page) {
        Page<User> userPage = userService.findByPage(page);
        return doJsonPagerOut(userPage);
    }

    // 返回消息
    @PostMapping
    public MyResponseResult saveUser(@RequestBody User user) {
        userService.save(user);
        return doJsonMsg("用户创建成功");
    }

    // 自定义状态码和消息
    @DeleteMapping("/{id}")
    public MyResponseResult deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return doJsonMsg(200, "删除成功");
    }
}
```

**响应格式示例：**

```json
{
  "code": 200,
  "msg": "OK",
  "data": {
    "id": 1,
    "name": "张三"
  }
}
```

#### 2. 全局异常处理

Starter 默认注册全局 `@RestControllerAdvice` 处理常见异常；如果你还想使用 `doJsonOut` / `doJsonMsg` 这类快捷方法，再继承 `MyBaseController`。

业务代码里可以直接抛出自定义业务异常：

```java
import io.github.canjiemo.mycommon.exception.BusinessException;

@Service
public class UserService {

    public User findById(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            // 抛出业务异常，会被自动捕获并返回友好的错误信息
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
```

**错误响应示例：**

```json
{
  "code": 500,
  "msg": "用户不存在",
  "data": null
}
```

#### 3. 隐私字段脱敏

在响应 DTO 的字段上添加 `@Privacy` 注解，序列化时自动遮蔽敏感数据，无需改动业务代码。

```java
public class UserVO {

    @Privacy(type = PrivacyType.PHONE)
    private String phone;      // 138****8888

    @Privacy(type = PrivacyType.NAME)
    private String name;       // 张*明

    @Privacy(type = PrivacyType.EMAIL)
    private String email;      // zh***@qq.com

    @Privacy(type = PrivacyType.ID_CARD)
    private String idCard;     // 110101****2345

    @Privacy(type = PrivacyType.BANK_CARD)
    private String bankCard;   // ****1234

    @Privacy(type = PrivacyType.ADDRESS)
    private String address;    // 北京市朝阳区****

    @Privacy(left = 3, right = 4)
    private String custom1;    // 固定保留前3后4位

    @Privacy(percent = 0.6)
    private String custom2;    // 遮蔽中间60%

    @Privacy(maskChar = '#')
    private String custom3;    // 自定义遮蔽字符
}
```

**预设策略说明：**

| 类型 | 保留规则 | 示例 |
|------|---------|------|
| `PHONE` | 前3后4 | `138****8888` |
| `NAME` | 首尾各1 | `张*明` |
| `EMAIL` | 遮蔽本地部分中间 | `zh***@qq.com` |
| `ID_CARD` | 前6后4 | `110101****2345` |
| `BANK_CARD` | 仅保留后4 | `****1234` |
| `ADDRESS` | 保留前6 | `北京市朝阳区****` |
| `CUSTOM` | 由 `left`/`right`/`percent` 决定 | 自定义 |

**序列化框架支持：**

| 框架 | 检测条件 | 说明 |
|------|---------|------|
| Jackson 3 | classpath 含 `tools.jackson.core:jackson-databind` | Boot 4 默认 JsonMapper 自动注册 `PrivacyJackson3Module` |
| Jackson 2 | classpath 含 `com.fasterxml.jackson.core:jackson-databind` | 保留 `PrivacyJacksonModule`；应用需实际使用并注册到 Jackson 2 mapper |
| Fastjson | classpath 含 `com.alibaba:fastjson` | Starter 提供 `ValueFilter` Bean，需在应用侧注册到 Fastjson 配置 |
| Fastjson2 | classpath 含 `com.alibaba.fastjson2:fastjson2` | Starter 提供 `ValueFilter` Bean，需在应用侧注册到 Fastjson2 配置 |

这些 JSON 库可同时共存，各自处理自己的序列化路径。

> **提示**：Boot 4 默认自动配置的 JsonMapper 会拾取 `PrivacyJackson3Module`。自行创建 mapper 时，必须在 builder 中显式 `addModule`；Fastjson/Fastjson2 需要将对应 filter 挂到实际使用的序列化配置中。脱敏只改变输出，不修改 DTO 原始值，也不替代存储加密或权限检查。

#### 4. 增强参数校验

##### 4.1 实体类校验

```java
public class UserDTO {

    @IdCard(required = true, message = "请输入有效的身份证号")
    private String idCard;

    @Number(required = true, min = 0, max = 150, message = "年龄必须在0-150之间")
    private String age;

    @ValIn(value = {"male", "female"}, message = "性别只能是male或female")
    private String gender;

    @Date(format = "yyyy-MM-dd", message = "请输入正确的日期格式(yyyy-MM-dd)")
    private String birthday;

    @LimitLength(min = 2, max = 50, chineseLength = 2, message = "姓名长度必须在2-50字符之间")
    private String name;
}
```

```java
@RestController
public class UserController extends MyBaseController {

    // 使用 @Valid 触发校验
    @PostMapping("/users")
    public MyResponseResult createUser(@Valid @RequestBody UserDTO userDTO) {
        // 校验失败会自动返回错误信息
        userService.save(userDTO);
        return doJsonDefaultMsg();
    }
}
```

##### 4.2 方法参数校验

使用 `@Validated` 注解在方法或类上启用参数级别的校验：

```java
@RestController
@Validated
public class UserController extends MyBaseController {

    @GetMapping("/user/{id}")
    public MyResponseResult getUser(
        @Number(min = 1, message = "用户ID必须大于0") @RequestParam String id
    ) {
        User user = userService.findById(Long.parseLong(id));
        return doJsonOut(user);
    }
}
```

## 自定义校验器详解

### @IdCard - 身份证号校验

校验中国大陆 15 位或 18 位身份证号。

```java
@IdCard(required = true, message = "请输入有效的身份证号")
private String idCard;
```

**参数：**
- `required`: 是否必填，默认 `true`
- `message`: 自定义错误消息

**校验规则：**
- 支持 15 位和 18 位身份证
- 验证地区码、出生日期、校验码
- 年龄必须在 0-150 岁之间

---

### @Number - 数字范围校验

校验字符串是否为数字，并支持范围和整数限制。

```java
@Number(required = true, min = 0, max = 100, integer = true, message = "分数必须是0-100的整数")
private String score;
```

**参数：**
- `required`: 是否必填，默认 `true`
- `min`: 最小值，默认 `Long.MIN_VALUE`
- `max`: 最大值，默认 `Long.MAX_VALUE`
- `integer`: 是否必须为整数，默认 `false`
- `message`: 自定义错误消息

---

### @ValIn - 值域校验

校验字符串是否在指定的值域范围内，支持枚举类型。

```java
// 简单值域
@ValIn(value = {"PENDING", "APPROVED", "REJECTED"}, message = "状态值无效")
private String status;

// 枚举值域
@ValIn(
    enumType = @EnumType(type = UserStatus.class, valKey = "code"),
    message = "用户状态无效"
)
private String userStatus;

// 忽略大小写
@ValIn(value = {"true", "false"}, ignoreCase = true, message = "布尔值无效")
private String boolValue;
```

**参数：**
- `value`: 允许的字符串值数组
- `enumType`: 枚举类型配置（支持通过枚举字段值校验）
- `ignoreCase`: 是否忽略大小写，默认 `true`
- `required`: 是否必填，默认 `true`
- `message`: 自定义错误消息

**EnumType 参数：**
- `type`: 枚举类
- `valKey`: 枚举中用于匹配的字段名
- `include`: 包含的枚举值（默认全部）
- `exclude`: 排除的枚举值

**示例枚举：**

```java
public enum UserStatus {
    ACTIVE("1", "激活"),
    INACTIVE("0", "停用");

    private String code;
    private String desc;

    UserStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

---

### @Date - 日期格式校验

校验字符串是否符合指定的日期格式。

```java
@Date(format = "yyyy-MM-dd HH:mm:ss", message = "请输入正确的日期时间格式")
private String createTime;

@Date(format = "yyyy-MM-dd", required = true, message = "出生日期不能为空")
private String birthday;
```

**参数：**
- `format`: 日期格式（符合 `SimpleDateFormat` 规范）
- `required`: 是否必填，默认 `true`
- `message`: 自定义错误消息

**常用格式：**
- `yyyy-MM-dd`: 2024-01-01
- `yyyy-MM-dd HH:mm:ss`: 2024-01-01 12:30:45
- `yyyy/MM/dd`: 2024/01/01

---

### @LimitLength - 字符串长度校验

校验字符串长度（支持中英文混合，可自定义中文字符长度）。

```java
@LimitLength(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
private String password;

@LimitLength(min = 2, max = 50, chineseLength = 2, message = "姓名长度必须在2-50字符之间")
private String name;
```

**参数：**
- `min`: 最小长度，默认 `0`
- `max`: 最大长度，默认 `Long.MAX_VALUE`
- `chineseLength`: 一个中文字符计为几个长度，默认 `2`
- `required`: 是否必填，默认 `true`
- `message`: 自定义错误消息

**长度计算规则：**
- 英文、数字、符号：长度为 1
- 中文字符：长度为 `chineseLength` 设置的值

**示例：**

```java
// chineseLength = 2
"hello"    -> 长度 5
"你好"      -> 长度 4
"hello你好" -> 长度 9
```

---

## 编程式校验

除了注解式校验，还可以使用工具类进行编程式校验：

```java
import io.github.canjiemo.base.mymvc.validator.MyValidatorUtils;
import io.github.canjiemo.mycommon.exception.BusinessException;

public class UserService {

    public void saveUser(UserDTO userDTO) {
        // 手动触发校验，校验失败会抛出 BusinessException
        MyValidatorUtils.validateEntity(userDTO);

        // 业务逻辑...
    }
}
```

## 全局异常处理

启用 Starter 后，以下异常会被自动处理。HTTP 层统一返回 `200`，错误类型体现在响应体的 `code` 字段中：

| 异常类型 | 响应体 code | 说明 |
|---------|-----------|------|
| `BaseException` | 自定义 | 业务异常基类 |
| `BusinessException` | 500 | 业务异常 |
| `BindException` | 400 | 参数绑定异常（Bean Validation） |
| `MethodArgumentNotValidException` | 400 | `@RequestBody` Bean Validation 异常 |
| `HandlerMethodValidationException` | 400 | 方法级参数校验异常 |
| `HttpMessageNotReadableException` | 400 | JSON 解析异常 |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不支持 |
| `DuplicateKeyException` | 500 | 数据库唯一键冲突 |
| `AccessDeniedException` | 403 | 权限不足 |
| `AuthenticationException` | 403 | 认证失败 |
| `HttpMediaTypeNotSupportedException` | 422 | 媒体类型不支持 |
| 其他异常 | 500 | 未知异常 |

### 自定义异常错误码

```java
public static final String LOGIN_ERROR_MSG = "非法授权,请先登录";
public static final String PERMISSION_ERROR_MSG = "您没有权限，请联系管理员授权";
public static final String REQUEST_ERROR_MSG = "请求参数格式错误";
public static final String DUPLICATEKEY_ERROR_MSG = "系统已经存在该记录";
```

可以通过两种方式自定义：

1. 提供你自己的 `MyMvcExceptionHandler` Bean，覆盖默认自动装配。
2. 继承 `MyBaseController` 并重写对应的异常处理方法。

```java
public class CustomController extends MyBaseController {

    @Override
    protected MyResponseResult handleException(Exception e) {
        return doJsonMsg(500, "自定义错误提示");
    }
}
```

## 技术栈

- **Java 25**
- **Spring Boot 4.1.1**
- **Spring Framework 7.0.9**
- **Jackson 3.1.5（默认）；Jackson 2.21.5（可选适配）**
- **Jakarta Validation 3.1.1／Servlet 6.1**
- **Hibernate Validator 9.1.3.Final**
- **Apache Commons Lang3 3.20.0**
- **AspectJ 1.9.25.1**

## 兼容性

- ✅ Spring Boot 4.1.x；构建固定 4.1.1
- ✅ Spring Framework 7
- ✅ Eclipse Temurin JDK 25
- ✅ Servlet 6.1；联合验证使用 Tomcat 11

> **注意**：
> 1. 本分支以 Boot 4.1.x 为目标；Boot 3 使用历史制品。
> 2. 使用满足 Servlet 6.1 的容器；Boot 4 已移除 Undertow 支持。Jetty 按 Boot 4 官方版本选择，尚未在本仓库联合验证。

## 配置项

Starter 默认开箱即用，同时支持通过 `mymvc.*` 调整核心行为：

| 配置项 | 默认值 | 说明 |
|------|------|------|
| `mymvc.exception-handler.enabled` | `true` | 是否启用默认全局异常 `@RestControllerAdvice` |
| `mymvc.response.success-code` | `200` | 成功响应默认业务码 |
| `mymvc.response.success-message` | `OK` | `doJsonOut(data)` 的默认消息 |
| `mymvc.response.default-success-message` | `操作成功` | `doJsonDefaultMsg()` 的默认消息 |
| `mymvc.messages.login-error` | `非法授权,请先登录` | 认证失败提示 |
| `mymvc.messages.permission-error` | `您没有权限，请联系管理员授权` | 权限失败提示 |
| `mymvc.messages.request-error` | `请求参数格式错误` | 通用请求错误提示 |
| `mymvc.messages.duplicate-key-error` | `系统已经存在该记录` | 唯一键冲突提示 |
| `mymvc.messages.fallback-error` | `请求失败,请稍后再试` | 未知异常提示 |
| `mymvc.messages.json-parse-error` | `JSON格式错误，请检查请求参数` | JSON 解析失败提示 |
| `mymvc.messages.json-type-error` | `参数类型错误，请检查数据类型` | JSON 类型不匹配提示 |
| `mymvc.messages.missing-request-body-error` | `缺少请求体` | 请求体缺失提示 |

例如：

```yaml
mymvc:
  exception-handler:
    enabled: true
  response:
    default-success-message: 处理完成
  messages:
    fallback-error: 系统繁忙，请稍后再试
```

如需彻底关闭 Starter 自动配置，也可以直接排除自动配置类：

```java
@SpringBootApplication(exclude = {
    MyMvcAutoConfiguration.class  // 禁用参数校验切面
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 最佳实践

### 1. Controller 层

```java
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController extends MyBaseController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public MyResponseResult<List<Product>> list() {
        return doJsonOut(productService.findAll());
    }

    @PostMapping
    public MyResponseResult create(@Valid @RequestBody ProductDTO dto) {
        productService.create(dto);
        return doJsonDefaultMsg();
    }

    @GetMapping("/{id}")
    public MyResponseResult<Product> get(
        @Number(min = 1, message = "ID必须大于0") @PathVariable String id
    ) {
        return doJsonOut(productService.findById(Long.parseLong(id)));
    }
}
```

### 2. Service 层

```java
@Service
public class ProductService {

    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public void create(ProductDTO dto) {
        // 编程式校验
        MyValidatorUtils.validateEntity(dto);

        // 业务逻辑
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        productRepository.save(product);
    }
}
```

### 3. DTO 层

```java
@Data
public class ProductDTO {

    @LimitLength(min = 2, max = 100, message = "商品名称长度必须在2-100字符之间")
    private String name;

    @Number(required = true, min = 0, message = "价格必须大于等于0")
    private String price;

    @ValIn(value = {"NEW", "HOT", "SALE"}, message = "标签值无效")
    private String tag;

    @Date(format = "yyyy-MM-dd", message = "上架日期格式错误")
    private String publishDate;
}
```

## 常见问题

### Q: 为什么校验不生效？

**A:** 请确保：
1. 如果你要使用 `doJsonOut` / `doJsonMsg` 等快捷方法，Controller 需要继承 `MyBaseController`
2. 请求体参数上添加了 `@Valid`，方法参数校验请在类或方法上添加 `@Validated`
3. Maven 依赖正确引入，尤其是 `spring-boot-starter-validation`
4. 如果要启用方法参数切面，请额外引入 `spring-boot-starter-aspectj`

仅使用全局异常处理时，不继承 `MyBaseController` 也可以。

### Q: 如何自定义错误消息格式？

**A:** 重写 `MyBaseController` 中的 `handleException` 方法：

```java
@RestController
public class CustomController extends MyBaseController {

    @Override
    @ExceptionHandler(BaseException.class)
    protected MyResponseResult handleException(BaseException e) {
        // 自定义错误处理逻辑
        return doJsonMsg(e.getCode(), "【错误】" + e.getMessage());
    }
}
```

### Q: 校验失败的错误消息是什么格式？

**A:** 参数名 + 空格 + 错误消息。例如：

```
age 年龄必须在0-150之间
idCard 请输入有效的身份证号
```

如需稳定输出真实参数名而不是 `arg0`、`arg1`，请确保应用在编译时开启了 `-parameters`。

如果有多个字段校验失败，会按字母顺序排列并用逗号连接：

```
age 年龄必须在0-150之间,name 姓名长度必须在2-50字符之间
```

### Q: 如何禁用某个校验器？

**A:** 校验器是按需使用的，不使用对应注解即不会触发校验。如需禁用整个参数校验功能，可排除 `MyMvcAutoConfiguration`。

### Q: 如何自定义 Jackson 配置？

**A:** Boot 4 推荐通过 builder customizer 调整默认 JsonMapper，这样仍保留 Boot 自动注册的脱敏模块：

```java
@Configuration
public class JacksonConfig {
    @Bean
    public org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
```

### Q: 为什么依赖标记为 optional？

**A:** 这是 Spring Boot Starter 的最佳实践：
- **避免版本冲突**：不会覆盖你项目中已有的 Spring 版本
- **减小体积**：只在需要时才引入依赖
- **灵活性**：你可以选择不使用某些功能（如校验）

`spring-boot-starter-webmvc` 提供 MVC 与默认 Jackson 3；参数校验额外引入 `spring-boot-starter-validation`，方法校验切面额外引入 `spring-boot-starter-aspectj`。Fastjson 适配按需添加。

### Q: 如何选择 Web 容器？

**A:** 本 Starter 不强制容器，你可以自由选择：

```xml
<!-- 使用 Tomcat（默认） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- 使用 Jetty -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

## 依赖说明

本 Starter 采用**最少依赖原则**设计，核心依赖均标记为 `optional`，不会污染你的项目依赖树。

### 必需依赖（会传递到你的项目）

```xml
<!-- 业务异常基类 -->
<dependency>
    <groupId>io.github.canjiemo</groupId>
    <artifactId>mycommon</artifactId>
</dependency>

<!-- 工具类 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
```

### 可选依赖（需要你的项目提供）

以下依赖标记为 `optional`，由应用选用的 WebMVC、Validation、AspectJ 或 JSON 依赖提供：

- `spring-boot-autoconfigure` - 自动配置
- `spring-webmvc` - MVC 支持
- `jakarta.servlet-api` - Servlet 规范
- `spring-aop` - AOP 支持
- `aspectjweaver` - 切面编织
- `jakarta.validation-api` - 校验 API
- `hibernate-validator` - 校验实现
- `slf4j-api` - 日志接口
- `tools.jackson.core:jackson-databind` - 默认 Jackson 3 脱敏适配（有则激活）
- `com.fasterxml.jackson.core:jackson-databind` - 可选 Jackson 2 脱敏适配
- `fastjson` - 隐私脱敏 Fastjson v1 适配（有则激活）
- `fastjson2` - 隐私脱敏 Fastjson2 适配（有则激活）

### 为什么这样设计？

1. **避免版本冲突** - 不强制依赖版本，由你的项目统一管理
2. **减小体积** - 不引入冗余的 starter 和传递依赖
3. **容器选择** - 由应用选择 Boot 4 支持的 Servlet 6.1 容器，例如 Tomcat 或 Jetty
4. **灵活性高** - 可以精确控制每个依赖的版本

## 更新日志

### 2.0.0-jdk25-SNAPSHOT

- 编译及运行基线升级为 JDK 25。
- 适配 Boot 4.1／Spring 7，导入 Boot BOM；迁移方法参数校验 API，保留跨参数错误信息。
- 新增 Jackson 3 脱敏模块及可选依赖隔离测试，覆盖数组形态、展开字段、空值与字段重命名。
- Web／AOP 依赖分别使用 `spring-boot-starter-webmvc`／`spring-boot-starter-aspectj`；测试使用对应 webmvc-test starter。
- 更新 Spring 依赖以读取 Java 25 字节码，显式配置 Lombok 及配置元数据处理器。
- 修复 JDK 25 构建时响应 DTO 缺少生成的 getter、异常响应无法正确序列化的问题。

### 1.0-jdk21 版本（历史）
- ✅ **隐私脱敏**：新增 `@Privacy` 注解，支持 Jackson / Fastjson / Fastjson2 自动脱敏
- ✅ 升级 JDK 至 21

### spring3 版本
- ✅ 升级支持 Spring Boot 3.x 和 Jakarta EE
- ✅ **架构优化**：移除所有 Hibernate Validator 内部 API 反射调用
- ✅ **依赖优化**：采用最少依赖原则，核心依赖标记为 optional
- ✅ **性能优化**：ValidatorFactory 改为 Spring Bean 注入
- ✅ **Bug 修复**：修复 ValInValidator 的校验逻辑错误
- ✅ **代码质量**：移除 Hashtable、修复空指针风险、添加 final 修饰符
- ✅ **容器中立**：移除强制 Undertow 依赖，支持任意容器
- ✅ **配置简化**：移除 Jackson 自动配置，减少干预

## 许可证

本项目采用 [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) 许可证。

## 联系方式

- **作者**: mocanjie
- **邮箱**: canjiemo@gmail.com
- **GitHub**: [https://github.com/canjiemo/mymvc-spring-boot-starter](https://github.com/canjiemo/mymvc-spring-boot-starter)

## 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## Claude Code Skill

本项目提供了一个 [Claude Code](https://claude.ai/code) Skill，让 AI 在帮你写代码时自动理解本 Starter 的 API、注解用法和最佳实践，避免生成不符合本库约定的代码。

### 安装方式

**方式一：一行命令（推荐）**

```bash
mkdir -p ~/.claude/skills/mymvc-dev && curl -fsSL https://raw.githubusercontent.com/canjiemo/mymvc-spring-boot-starter/master/claude-skills/mymvc-dev/SKILL.md -o ~/.claude/skills/mymvc-dev/SKILL.md
```

**方式二：克隆后复制**

```bash
mkdir -p ~/.claude/skills/mymvc-dev && cp claude-skills/mymvc-dev/SKILL.md ~/.claude/skills/mymvc-dev/SKILL.md
```

安装完成后，Claude Code 会在你的项目中涉及以下场景时自动加载此 Skill：

- 使用 `MyResponseResult`、`MyBaseController`、`@Privacy` 等类/注解
- 配置 `mymvc.*` 属性
- 使用 `@Date`、`@IdCard`、`@Number`、`@ValIn`、`@LimitLength` 自定义校验器
- 需要统一响应格式、全局异常处理、隐私脱敏等功能

---

**如果这个项目对你有帮助，请给个 Star ⭐️ 支持一下！**
