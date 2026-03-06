package io.github.canjiemo.base.mymvc.configuration;

import io.github.canjiemo.base.mymvc.aspect.RequestParamValidAspect;
import io.github.canjiemo.base.mymvc.controller.MyMvcExceptionHandler;
import io.github.canjiemo.base.mymvc.support.MyExceptionResponseResolver;
import io.github.canjiemo.base.mymvc.support.MyResponseFactory;
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
            assertThat(context).hasSingleBean(MyResponseFactory.class);
            assertThat(context).hasSingleBean(MyExceptionResponseResolver.class);
            assertThat(context).hasSingleBean(MyMvcExceptionHandler.class);
        });
    }

    @Test
    void registersAspectWhenValidatorBeanExists() {
        contextRunner.withUserConfiguration(ValidatorConfig.class)
                .run(context -> assertThat(context).hasSingleBean(RequestParamValidAspect.class));
    }

    @Test
    void canDisableGlobalExceptionHandler() {
        contextRunner.withPropertyValues("mymvc.exception-handler.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MyMvcExceptionHandler.class));
    }

    @Test
    void bindsCustomProperties() {
        contextRunner.withPropertyValues(
                        "mymvc.response.default-success-message=处理完成",
                        "mymvc.messages.fallback-error=系统繁忙"
                )
                .run(context -> {
                    MyMvcProperties properties = context.getBean(MyMvcProperties.class);
                    assertThat(properties.getResponse().getDefaultSuccessMessage()).isEqualTo("处理完成");
                    assertThat(properties.getMessages().getFallbackError()).isEqualTo("系统繁忙");
                });
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
