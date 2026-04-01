---
name: mymvc-dev
description: |
  mymvc-spring-boot-starter 开发指南。当遇到以下任何场景时必须使用此 skill：
  - 项目中引入了 mymvc-spring-boot-starter 依赖
  - 代码中出现 MyResponseResult、MyBaseController、MyMvcExceptionHandler、MyMvcProperties、
    @Privacy、PrivacyType、@Date/@IdCard/@Number/@ValIn/@LimitLength、MyValidatorUtils 任何一个标识
  - 用户需要统一 HTTP 响应格式（code/msg/data 结构）
  - 用户需要全局异常处理、自定义错误消息
  - 用户需要中文身份证、日期格式、数值范围、值域枚举等自定义参数校验
  - 用户需要对接口响应字段做脱敏处理（手机号、姓名、身份证、银行卡等）
  - 配置 mymvc.* 相关属性
  这是一个 Spring Boot 3.2+ MVC 增强 starter，提供响应封装/异常处理/参数校验/隐私脱敏四大能力，
  请务必使用此 skill 确保用法、配置和扩展方式的正确性。
---

# mymvc-spring-boot-starter 开发指南

本 starter 为 Spring Boot 3.2+ Web 项目提供四大开箱即用能力：**统一响应封装**、**全局异常处理**、**自定义参数校验**、**隐私字段脱敏**。引入依赖后自动装配，零样板代码。

---

## 一、Maven 依赖引入

```xml
<!-- 核心 Starter（必须） -->
<dependency>
    <groupId>io.github.canjiemo</groupId>
    <artifactId>mymvc-spring-boot-starter</artifactId>
    <version>1.0.2-jdk21</version>
</dependency>

<!-- Spring Web（必须，需显式引入） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- 参数校验（可选，使用 @Valid/@Validated 及自定义验证器时需要） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- AOP 支持（可选，使用方法参数校验切面时需要） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**运行环境要求**：Java 21+、Spring Boot 3.2.0+

**注意**：所有 Spring 依赖在 starter 中标记为 `<optional>true</optional>`，需在引入方项目中显式声明。

---

## 二、统一响应封装

### 响应结构

所有接口响应使用 `MyResponseResult<T>`：

```json
{
  "code": 200,
  "msg": "OK",
  "data": { }
}
```

### 推荐：继承 MyBaseController

```java
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController extends MyBaseController {

    // 返回带数据的成功响应 → {"code":200,"msg":"OK","data":{...}}
    @GetMapping("/{id}")
    public MyResponseResult<User> getUser(@PathVariable Long id) {
        return doJsonOut(userService.findById(id));
    }

    // 返回默认成功消息 → {"code":200,"msg":"操作成功","data":null}
    @DeleteMapping("/{id}")
    public MyResponseResult<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return doJsonDefaultMsg();
    }

    // 返回自定义消息
    @PostMapping
    public MyResponseResult<Void> createUser(@Valid @RequestBody UserDTO dto) {
        userService.save(dto);
        return doJsonMsg(200, "用户创建成功");
    }
}
```

### 不继承时的用法

```java
// 直接构造
return new MyResponseResult<>(200, "成功", data);
// 或注入工厂
@Autowired MyResponseFactory responseFactory;
return responseFactory.success(data);
return responseFactory.error(500, "业务异常");
```

---

## 三、全局异常处理

自动装配 `MyMvcExceptionHandler`（`@RestControllerAdvice`），无需任何额外配置。

### 内置异常映射

| 异常类型 | 业务码 | 说明 |
|---------|--------|------|
| `BaseException` / `BusinessException` | 自定义 / 500 | 业务异常，直接透传 code 和 message |
| `MethodArgumentNotValidException` | 400 | `@RequestBody` 校验失败 |
| `BindException` | 400 | 表单参数绑定失败 |
| `HandlerMethodValidationException` | 400 | 方法参数校验失败（Spring 6.1+） |
| `HttpMessageNotReadableException` | 400 | JSON 解析错误 / 类型不匹配 / 缺少请求体 |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不支持 |
| `HttpMediaTypeNotSupportedException` | 422 | 媒体类型不支持 |
| `DuplicateKeyException` | 500 | 唯一键冲突 |
| `AccessDeniedException` | 403 | Spring Security 权限不足 |
| `AuthenticationException` | 403 | Spring Security 认证失败 |
| 其他所有异常 | 500 | 未知异常，返回 fallback 消息 |

**HTTP 状态码始终为 200**，错误信息通过 `code` 字段体现。

### 多字段校验失败的消息格式

多个字段同时校验失败时，消息按字母排序后以逗号拼接：

```
age 年龄必须在0-150之间,idCard 身份证格式错误,name 姓名长度超限
```

### 关闭全局异常处理

```yaml
mymvc:
  exception-handler:
    enabled: false
```

### 自定义异常处理

**方案 1：继承 MyBaseController 重写方法**（覆盖单个异常处理器）

```java
@Override
@ExceptionHandler(BaseException.class)
protected MyResponseResult<Void> handleException(BaseException e) {
    log.error("业务异常", e);
    return doJsonMsg(e.getCode(), "【系统提示】" + e.getMessage());
}
```

**方案 2：提供自定义 Bean**（完全替换，`@ConditionalOnMissingBean` 生效）

```java
@Configuration
public class ExceptionConfig {
    @Bean
    public MyMvcExceptionHandler myMvcExceptionHandler(
            MyExceptionResponseResolver resolver) {
        return new MyCustomExceptionHandler(resolver);
    }
}
```

---

## 四、自定义参数校验器

所有验证器均支持 `required` 参数：`required=true` + 空值 → 失败；`required=false` + 空值 → 跳过验证。

### @Date — 日期格式校验

```java
@Date(format = "yyyy-MM-dd", message = "日期格式不正确")
private String birthday;

@Date(format = "yyyy-MM-dd HH:mm:ss", required = false)
private String expireTime;  // 非必填，有值时才校验格式
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `format` | String | `"yyyy-MM-dd"` | SimpleDateFormat 格式字符串 |
| `required` | boolean | `true` | 是否必填 |
| `message` | String | `""` | 校验失败提示 |

### @IdCard — 中国身份证校验

```java
@IdCard(message = "身份证号码格式不正确")
private String idCard;
```

校验规则：支持 15 位 / 18 位，验证地区码、出生日期有效性、18 位校验码，年龄限制 0-150 岁。

### @Number — 数值范围校验

```java
@Number(min = 1, max = 100, message = "数量必须在1-100之间")
private String quantity;  // 注意：作用于字符串类型字段

@Number(min = 0, integer = true, required = false)
private String score;  // 非必填，有值时必须为非负整数
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `min` | long | `Integer.MIN_VALUE` | 最小值（含） |
| `max` | long | `Integer.MAX_VALUE` | 最大值（含） |
| `integer` | boolean | `false` | 是否限制为整数 |
| `required` | boolean | `true` | 是否必填 |

内部使用 `BigDecimal` 比较，避免浮点误差。

### @ValIn — 值域校验（支持枚举）

```java
// 限定字符串值域
@ValIn(value = {"1", "2", "3"}, message = "状态值不合法")
private String status;

// 从枚举类中读取合法值（通过反射获取指定字段的值）
@ValIn(enumType = @EnumType(value = StatusEnum.class, field = "code"))
private String status;

// 枚举过滤：排除某些枚举值
@ValIn(enumType = @EnumType(value = StatusEnum.class, field = "code",
       exclude = {"DELETED"}))
private String status;
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String[] | `{}` | 合法值数组（与 enumType 二选一或组合） |
| `ignoreCase` | boolean | `true` | 忽略大小写 |
| `required` | boolean | `true` | 是否必填 |
| `enumType` | EnumType[] | `{}` | 从枚举类读取合法值 |

### @LimitLength — 字符串长度校验（感知中文）

```java
// 限制总长度，中文按2个字符计算
@LimitLength(min = 2, max = 50, chineseLength = 2, message = "姓名长度不合法")
private String name;

// 中文按1个字符（数据库存储场景）
@LimitLength(max = 20, chineseLength = 1)
private String remark;
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `min` | long | `0` | 最小长度 |
| `max` | long | `Integer.MAX_VALUE` | 最大长度 |
| `chineseLength` | int | `2` | 一个中文字符计为几个长度 |
| `required` | boolean | `true` | 是否必填 |

### 编程式校验（非 Controller 层）

```java
// 触发 JSR 380 校验，失败时抛出 BusinessException（包含所有字段错误）
MyValidatorUtils.validateEntity(dto);

// 指定校验分组
MyValidatorUtils.validateEntity(dto, CreateGroup.class);
```

---

## 五、隐私字段脱敏

在响应对象的字段上添加 `@Privacy` 注解，序列化时自动脱敏。支持 Jackson（默认）、Fastjson、Fastjson2，按类路径自动激活。

### 预设脱敏策略

```java
@Data
public class UserVO {
    @Privacy(type = PrivacyType.PHONE)      // 138****8888
    private String phone;

    @Privacy(type = PrivacyType.NAME)       // 张*明
    private String name;

    @Privacy(type = PrivacyType.ID_CARD)    // 110101****2345
    private String idCard;

    @Privacy(type = PrivacyType.EMAIL)      // zh***@qq.com（本地部分中间50%）
    private String email;

    @Privacy(type = PrivacyType.BANK_CARD)  // ****1234（仅保留后4位）
    private String bankCard;

    @Privacy(type = PrivacyType.ADDRESS)    // 北京市朝阳区****（前6位）
    private String address;
}
```

| PrivacyType | 保留规则 | 示例输出 |
|-------------|---------|---------|
| `PHONE` | 前3 + 后4 | `138****8888` |
| `NAME` | 首尾各1 | `张*明` |
| `ID_CARD` | 前6 + 后4 | `110101****2345` |
| `EMAIL` | 本地部分中间50% | `zh***@qq.com` |
| `BANK_CARD` | 仅后4 | `****1234` |
| `ADDRESS` | 前6位 | `北京市朝阳区****` |
| `CUSTOM` | 自定义（见下方） | 自定义 |

### 自定义脱敏规则

```java
// 保留左3右2，用 # 遮蔽
@Privacy(type = PrivacyType.CUSTOM, left = 3, right = 2, maskChar = '#')
private String customField;  // "ABCDEFGH" → "ABC###GH"

// 遮蔽中间50%
@Privacy(type = PrivacyType.CUSTOM, percent = 0.5)
private String customField2;  // "ABCDEFGH" → "AB****GH"
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type` | PrivacyType | `CUSTOM` | 脱敏策略 |
| `left` | int | `-1` | 保留左侧字符数（-1=不限） |
| `right` | int | `-1` | 保留右侧字符数（-1=不限） |
| `percent` | double | `-1.0` | 遮蔽中间的百分比（0.0-1.0） |
| `maskChar` | char | `'*'` | 遮蔽使用的字符 |

---

## 六、application.yml 配置速查

```yaml
mymvc:
  exception-handler:
    enabled: true                  # 是否启用全局异常处理（默认 true）

  response:
    success-code: 200              # 成功响应的 code 字段值（默认 200）
    success-message: "OK"          # doJsonOut() 使用的默认 msg（默认 "OK"）
    default-success-message: "操作成功"  # doJsonDefaultMsg() 使用的 msg

  messages:
    login-error: "非法授权,请先登录"          # 认证失败提示
    permission-error: "您没有权限，请联系管理员授权"  # 权限不足提示
    request-error: "请求参数格式错误"          # 通用请求错误提示
    duplicate-key-error: "系统已经存在该记录"   # 唯一键冲突提示
    fallback-error: "请求失败,请稍后再试"       # 未知异常 fallback 提示
    json-parse-error: "JSON格式错误，请检查请求参数"  # JSON解析失败提示
    json-type-error: "参数类型错误，请检查数据类型"   # JSON类型不匹配提示
    missing-request-body-error: "缺少请求体"   # 请求体缺失提示
```

---

## 七、完整集成示例

```java
// 1. DTO：结合自定义验证器
@Data
public class CreateUserDTO {
    @LimitLength(min = 2, max = 20, message = "姓名长度2-20字符")
    private String name;

    @IdCard
    private String idCard;

    @Date(format = "yyyy-MM-dd", message = "生日格式须为 yyyy-MM-dd")
    private String birthday;

    @Number(min = 18, max = 80, message = "年龄必须18-80岁")
    private String age;

    @ValIn(value = {"M", "F"}, ignoreCase = false, message = "性别只能为M或F")
    private String gender;

    @Number(required = false, min = 0)
    private String score;  // 非必填
}

// 2. VO：带脱敏注解
@Data
public class UserVO {
    private Long id;
    @Privacy(type = PrivacyType.NAME)
    private String name;
    @Privacy(type = PrivacyType.PHONE)
    private String phone;
    @Privacy(type = PrivacyType.ID_CARD)
    private String idCard;
}

// 3. Controller：继承基础控制器
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController extends MyBaseController {

    @PostMapping
    public MyResponseResult<Void> create(@Valid @RequestBody CreateUserDTO dto) {
        userService.create(dto);
        return doJsonDefaultMsg();  // {"code":200,"msg":"操作成功","data":null}
    }

    @GetMapping("/{id}")
    public MyResponseResult<UserVO> getById(@PathVariable Long id) {
        return doJsonOut(userService.getVO(id));  // name/phone/idCard 自动脱敏
    }

    // 4. 编程式校验（Service 层）
    public void saveInBatch(List<CreateUserDTO> list) {
        list.forEach(dto -> MyValidatorUtils.validateEntity(dto));
        userService.batchSave(list);
    }
}
```

---

## 八、常见陷阱

1. **`@Privacy` 注解只在序列化时生效**。直接在代码中调用 `getXxx()` 不会触发脱敏，脱敏发生在 Jackson/Fastjson 序列化输出阶段。

2. **自定义验证器作用于 String 类型**。`@Number`、`@Date`、`@IdCard`、`@ValIn`、`@LimitLength` 均作用于 `String` 字段，不支持 `Integer`/`Long` 等基本类型。

3. **`@Validated` 与 `@Valid` 的区别**：
   - `@Valid`：触发 Bean 内部字段的嵌套校验，用于 `@RequestBody` 参数
   - `@Validated`：触发方法参数的校验（含自定义验证器），**类上需标注 `@Validated` 才能触发路径变量/请求参数的验证**

4. **多个 JSON 库共存时的脱敏**：starter 按类路径自动激活对应的脱敏模块（Jackson/Fastjson2/Fastjson），三者可同时存在不冲突。但脱敏只作用于实际使用的序列化器输出。

5. **关闭异常处理后**，需要自行处理所有异常响应格式；关闭后 `MyMvcExceptionHandler` Bean 不会注册，也不能被 `@Autowired`。

6. **成功码可自定义为 0**：若前端约定 `0=成功`，设置 `mymvc.response.success-code=0` 即可，不影响异常响应码（异常码不受此配置影响）。

7. **编译参数 `-parameters` 需开启**，否则校验失败的错误消息中参数名会显示为 `arg0`/`arg1`，而非真实字段名。在 `maven-compiler-plugin` 中添加 `<arg>-parameters</arg>`。
