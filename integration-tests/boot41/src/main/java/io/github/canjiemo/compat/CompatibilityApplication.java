package io.github.canjiemo.compat;

import io.github.canjiemo.base.myjdbc.audit.AuditFieldProvider;
import io.github.canjiemo.tools.dict.IMyDict;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

/** 三组件联合测试入口，通过标准自动配置发现组件，禁止手工导入组件配置类。 */
@SpringBootApplication
public class CompatibilityApplication {
    @Bean
    IMyDict dictionary() {
        return (type, value) -> "employee_status".equals(type) && "1".equals(value) ? "在职" : "未知";
    }

    @Bean
    AuditFieldProvider auditor() {
        return () -> 100L;
    }

    @Bean
    ValidationService validationService() {
        return new ValidationService();
    }

    public static class ValidationService {
        @Validated
        public void check(@Min(value = 1, message = "数量至少为1") int count) {}
    }
}
