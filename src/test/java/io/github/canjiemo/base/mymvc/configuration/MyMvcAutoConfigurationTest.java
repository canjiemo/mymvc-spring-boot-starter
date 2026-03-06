package io.github.canjiemo.base.mymvc.configuration;

import io.github.canjiemo.base.mymvc.aspect.RequestParamValidAspect;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

class MyMvcAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MyMvcAutoConfiguration.class));

    @Test
    void skipsAspectWhenValidatorBeanIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RequestParamValidAspect.class);
        });
    }

    @Test
    void registersAspectWhenValidatorBeanExists() {
        contextRunner.withUserConfiguration(ValidatorConfig.class)
                .run(context -> assertThat(context).hasSingleBean(RequestParamValidAspect.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class ValidatorConfig {

        @Bean
        Validator validator() {
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.setMessageInterpolator(new ParameterMessageInterpolator());
            validator.afterPropertiesSet();
            return validator;
        }
    }
}
