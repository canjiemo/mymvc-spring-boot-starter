package io.github.canjiemo.base.mymvc.controller;

import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.base.mymvc.support.MyExceptionResponseResolver;
import io.github.canjiemo.base.mymvc.support.MyResponseFactory;
import jakarta.validation.Valid;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyMvcExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MyMvcProperties properties = new MyMvcProperties();
        properties.getMessages().setMissingRequestBodyError("请求体丢失");

        MyResponseFactory responseFactory = new MyResponseFactory(properties);
        MyExceptionResponseResolver resolver = new MyExceptionResponseResolver(responseFactory, properties);

        mockMvc = MockMvcBuilders.standaloneSetup(new PlainController())
                .setControllerAdvice(new MyMvcExceptionHandler(resolver))
                .setValidator(createValidator())
                .build();
    }

    @Test
    void handlesValidationErrorsWithoutBaseController() throws Exception {
        mockMvc.perform(post("/plain/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("名字不能为空"));
    }

    @Test
    void usesConfiguredMessageForMissingRequestBody() throws Exception {
        mockMvc.perform(post("/plain/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请求体丢失"));
    }

    private Validator createValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        return validator;
    }

    @RestController
    static class PlainController {

        @PostMapping("/plain/users")
        MyResponseResult<Void> create(@Valid @RequestBody PlainRequest request) {
            return new MyResponseResult<>(200, "OK", null);
        }
    }

    static class PlainRequest {

        @jakarta.validation.constraints.NotBlank(message = "名字不能为空")
        public String name;
    }
}
