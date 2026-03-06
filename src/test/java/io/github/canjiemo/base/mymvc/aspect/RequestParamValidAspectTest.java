package io.github.canjiemo.base.mymvc.aspect;

import io.github.canjiemo.base.mymvc.validator.Number;
import io.github.canjiemo.mycommon.exception.BusinessException;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestParamValidAspectTest {

    @Test
    void validatesClassLevelValidatedBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            TestService testService = context.getBean(TestService.class);

            assertThatThrownBy(() -> testService.getUser("0"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("id 用户ID必须大于0");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @org.springframework.context.annotation.EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        Validator validator() {
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.setMessageInterpolator(new ParameterMessageInterpolator());
            validator.afterPropertiesSet();
            return validator;
        }

        @Bean
        RequestParamValidAspect requestParamValidAspect(Validator validator) {
            return new RequestParamValidAspect(validator);
        }

        @Bean
        TestService testService() {
            return new TestService();
        }
    }

    @Validated
    static class TestService {

        public void getUser(@Number(min = 1, message = "用户ID必须大于0") String id) {
        }
    }
}
