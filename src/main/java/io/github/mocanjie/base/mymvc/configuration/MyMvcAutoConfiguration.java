package io.github.mocanjie.base.mymvc.configuration;

import io.github.mocanjie.base.mymvc.aspect.RequestParamValidAspect;
import io.github.mocanjie.base.mymvc.privacy.fastjson.PrivacyFastjsonFilter;
import io.github.mocanjie.base.mymvc.privacy.fastjson2.PrivacyFastjson2Filter;
import io.github.mocanjie.base.mymvc.privacy.jackson.PrivacyJacksonModule;
import jakarta.validation.Validator;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({RequestParamValidAspect.class})
public class MyMvcAutoConfiguration implements BeanPostProcessor, Ordered {

    @Bean
    @ConditionalOnMissingBean
    public RequestParamValidAspect getRequestParamValidAspect(Validator validator){
        return new RequestParamValidAspect(validator);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Jackson 隐私脱敏配置，仅在 jackson-databind 存在时生效。
     * 使用内部类隔离，避免 jackson 不存在时触发 PrivacyJacksonModule 的类加载。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    static class JacksonPrivacyConfig {
        @Bean
        @ConditionalOnMissingBean(PrivacyJacksonModule.class)
        public PrivacyJacksonModule privacyJacksonModule() {
            return new PrivacyJacksonModule();
        }
    }

    /**
     * Fastjson2 隐私脱敏配置，仅在 fastjson2 存在时生效。
     * 使用内部类隔离，避免 fastjson2 不存在时触发 PrivacyFastjson2Filter 的类加载。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alibaba.fastjson2.JSON")
    static class Fastjson2PrivacyConfig {
        @Bean
        @ConditionalOnMissingBean(PrivacyFastjson2Filter.class)
        public PrivacyFastjson2Filter privacyFastjson2Filter() {
            return new PrivacyFastjson2Filter();
        }
    }

    /**
     * Fastjson1 隐私脱敏配置，仅在 fastjson1 存在时生效。
     * 使用内部类隔离，避免 fastjson1 不存在时触发 PrivacyFastjsonFilter 的类加载。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alibaba.fastjson.JSON")
    static class FastjsonPrivacyConfig {
        @Bean
        @ConditionalOnMissingBean(PrivacyFastjsonFilter.class)
        public PrivacyFastjsonFilter privacyFastjsonFilter() {
            return new PrivacyFastjsonFilter();
        }
    }
}
