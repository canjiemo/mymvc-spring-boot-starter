package io.github.canjiemo.base.mymvc.controller;

import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.base.mymvc.validator.LimitLength;
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

class MyBaseControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setValidator(createValidator())
                .build();
    }

    @Test
    void returnsBusinessCodeForRequestBodyValidationErrors() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("姓名长度必须在2-20字符之间"));
    }

    private Validator createValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        return validator;
    }

    @RestController
    static class TestController extends MyBaseController {

        @PostMapping("/users")
        MyResponseResult create(@Valid @RequestBody UserRequest request) {
            return doJsonDefaultMsg();
        }
    }

    static class UserRequest {

        @LimitLength(min = 2, max = 20, message = "姓名长度必须在2-20字符之间")
        public String name;
    }
}
