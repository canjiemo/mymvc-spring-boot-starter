package io.github.canjiemo.compat;

import io.github.canjiemo.base.myjdbc.builder.TableInfoBuilder;
import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import io.github.canjiemo.base.myjdbc.tenant.TenantContext;
import io.github.canjiemo.base.myjdbc.metrics.MyJdbcCacheMetrics;
import io.github.canjiemo.base.mymvc.aspect.RequestParamValidAspect;
import io.github.canjiemo.base.mymvc.privacy.jackson3.PrivacyJackson3Module;
import io.github.canjiemo.tools.dict.MyDictHelper;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = CompatibilityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "spring.datasource.url=jdbc:h2:mem:boot41compat;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.hikari.connection-init-sql=CREATE TABLE IF NOT EXISTS employee (id BIGINT PRIMARY KEY, status INT, delete_flag INT, tenant_id BIGINT, created_by BIGINT)",
                "myjdbc.validate-schema=true", "myjdbc.fail-on-validation-error=true", "myjdbc.tenant.enabled=true"})
class StartersIntegrationTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired IBaseDao dao;
    @Autowired JsonMapper mapper;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void prepare() {
        jdbc.execute("DELETE FROM employee");
        jdbc.update("INSERT INTO employee VALUES (9007199254740993,1,0,10,100),(2,1,1,10,100),(3,1,0,20,200)");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void discoversAllStartersThroughBootAutoConfiguration() {
        assertThat(context.getBean(MyDictHelper.class)).isNotNull();
        assertThat(context.getBean(RequestParamValidAspect.class)).isNotNull();
        assertThat(context.getBean(PrivacyJackson3Module.class)).isNotNull();
        assertThat(TableInfoBuilder.getTableInfo(Employee.class)).isNotNull();
    }

    @Test
    void actuatorDiscoversMyJdbcMetrics() {
        assertThat(context.getBeansOfType(MyJdbcCacheMetrics.class)).hasSize(1);
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        assertThat(registry.find("myjdbc.sql_rewrite_cache.size").gauge()).isNotNull();
        assertThat(registry.find("myjdbc.sql_rewrite_cache.hits").gauge()).isNotNull();
        assertThat(registry.find("myjdbc.sql_rewrite_cache.misses").gauge()).isNotNull();
    }

    @Test
    void defaultHttpJsonIncludesGeneratedDescriptionStringIdAndMaskedPhone() throws Exception {
        JsonNode response = get("/employee");
        assertThat(response.path("code").asInt()).isEqualTo(200);
        assertThat(response.at("/data/id").isString()).isTrue();
        assertThat(response.at("/data/id").asString()).isEqualTo("9007199254740993");
        assertThat(response.at("/data/statusDesc").asString()).isEqualTo("在职");
        assertThat(response.at("/data/phone").asString()).isEqualTo("138****8888");
    }

    @Test
    void queryFiltersOtherTenantsAndDeletedRows() {
        TenantContext.setTenantId(10L);
        assertThat(dao.queryListForSql("SELECT * FROM employee", Map.of(), Employee.class))
                .extracting(Employee::getId).containsExactly(9007199254740993L);
        TenantContext.setTenantId(20L);
        assertThat(dao.queryListForSql("SELECT * FROM employee", Map.of(), Employee.class))
                .extracting(Employee::getId).containsExactly(3L);
    }

    @Test
    void insertFillsTenantAndAuditFieldsAndParticipatesInBootTransaction() {
        TenantContext.setTenantId(10L);
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setStatus(1);
        employee.setDeleteFlag(0);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            dao.insertPO(employee, false);
            assertThat(jdbc.queryForObject("SELECT created_by FROM employee WHERE id=4", Long.class)).isEqualTo(100L);
            assertThat(jdbc.queryForObject("SELECT tenant_id FROM employee WHERE id=4", Long.class)).isEqualTo(10L);
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id=4", Integer.class)).isZero();
    }

    @Test
    void preservesRequestBodyValidationResponse() throws Exception {
        JsonNode response = post("{\"name\":\"\"}");
        assertThat(response.path("code").asInt()).isEqualTo(400);
        assertThat(response.path("msg").asString()).isEqualTo("姓名不能为空");
    }

    @Test
    void handlesSpring7NativeMethodValidation() throws Exception {
        JsonNode response = get("/native-validation?count=0");
        assertThat(response.path("code").asInt()).isEqualTo(400);
        assertThat(response.path("msg").asString()).contains("数量至少为1");
    }

    @Test
    void appliesMyMvcValidationAspectWithBoot4AspectjStarter() {
        assertThatThrownBy(() -> context.getBean(CompatibilityApplication.ValidationService.class).check(0))
                .isInstanceOf(BusinessException.class).hasMessageContaining("数量至少为1");
    }

    @Test
    void handlesMalformedAndMissingJsonBodies() throws Exception {
        assertThat(post("{broken").path("code").asInt()).isEqualTo(400);
        assertThat(post("").path("code").asInt()).isEqualTo(400);
    }

    @Test
    void preservesBusinessExceptionResponse() throws Exception {
        assertThat(get("/business-error").path("msg").asString()).isEqualTo("测试业务异常");
    }

    @Test
    void jackson2IsNotRequiredByConsumer() {
        assertThatThrownBy(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private JsonNode get(String path) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build());
    }

    private JsonNode post(String body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/employee"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body());
    }
}
