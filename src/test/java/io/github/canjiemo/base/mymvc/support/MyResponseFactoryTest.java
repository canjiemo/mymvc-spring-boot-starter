package io.github.canjiemo.base.mymvc.support;

import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyResponseFactoryTest {

    @Test
    void usesConfiguredSuccessDefaults() {
        MyMvcProperties properties = new MyMvcProperties();
        properties.getResponse().setSuccessCode(201);
        properties.getResponse().setSuccessMessage("DONE");
        properties.getResponse().setDefaultSuccessMessage("已完成");

        MyResponseFactory factory = new MyResponseFactory(properties);

        assertThat(factory.success("payload"))
                .extracting("code", "msg", "data")
                .containsExactly(201, "DONE", "payload");
        assertThat(factory.defaultSuccess())
                .extracting("code", "msg", "data")
                .containsExactly(201, "已完成", null);
    }
}
