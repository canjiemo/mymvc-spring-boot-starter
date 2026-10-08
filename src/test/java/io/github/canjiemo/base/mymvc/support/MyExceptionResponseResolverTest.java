package io.github.canjiemo.base.mymvc.support;

import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MyExceptionResponseResolverTest {
    @Test
    void preservesCrossParameterValidationErrorsFromSpring7() throws Exception {
        MyMvcProperties properties = new MyMvcProperties();
        MyExceptionResponseResolver resolver = new MyExceptionResponseResolver(new MyResponseFactory(properties), properties);
        MethodValidationResult result = MethodValidationResult.create(this,
                getClass().getDeclaredMethod("preservesCrossParameterValidationErrorsFromSpring7"), List.of(),
                List.of(new DefaultMessageSourceResolvable(new String[]{"invalidRange"}, "结束时间不能早于开始时间")));
        MyResponseResult<Void> response = resolver.handleHandlerMethodValidationException(
                new HandlerMethodValidationException(result));
        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMsg()).isEqualTo("结束时间不能早于开始时间");
    }
}
