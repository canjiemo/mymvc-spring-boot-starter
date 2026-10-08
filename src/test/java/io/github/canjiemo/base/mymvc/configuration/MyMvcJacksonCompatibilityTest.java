package io.github.canjiemo.base.mymvc.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.canjiemo.base.mymvc.privacy.Privacy;
import io.github.canjiemo.base.mymvc.privacy.PrivacyType;
import io.github.canjiemo.base.mymvc.privacy.jackson.PrivacyJacksonModule;
import io.github.canjiemo.base.mymvc.privacy.jackson3.PrivacyJackson3Module;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MyMvcJacksonCompatibilityTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MyMvcAutoConfiguration.class));

    @Test
    void bootDefaultMapperMasksSensitiveDataWithoutManualRegistration() {
        runner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PrivacyJackson3Module.class);
            Patient patient = new Patient();
            String json = context.getBean(JsonMapper.class).writeValueAsString(patient);
            assertThat(json).contains("138****8888", "110101********1234", "9007199254740993")
                    .doesNotContain("13812348888", "110101199001011234");
            assertThat(patient.phone).isEqualTo("13812348888");
        });
    }

    @Test
    void respectsRenamedPropertiesAndNullEmptyInclusion() {
        JsonMapper mapper = JsonMapper.builder().addModule(new PrivacyJackson3Module()).build();
        String json = mapper.writeValueAsString(new OptionalPatient());
        assertThat(mapper.readTree(json)).isEqualTo(mapper.readTree(
                "{\"mobile_number\":\"138****8888\",\"count\":7}"));
    }

    @Test
    void masksNestedAndCollectionValues() {
        JsonMapper mapper = JsonMapper.builder().addModule(new PrivacyJackson3Module()).build();
        String json = mapper.writeValueAsString(java.util.Map.of("patients", java.util.List.of(new Patient())));
        assertThat(json).contains("138****8888").doesNotContain("13812348888");
    }

    @Test
    void masksArrayShapeAndUnwrappedValues() {
        JsonMapper mapper = JsonMapper.builder().addModule(new PrivacyJackson3Module()).build();
        assertThat(mapper.writeValueAsString(new ArrayPatient())).isEqualTo("[\"138****8888\"]");
        assertThat(mapper.writeValueAsString(new UnwrappedPatient()))
                .contains("contact_phone", "138****8888").doesNotContain("13812348888");
    }

    @Test
    void retainsJackson2OptionalAdapter() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new PrivacyJacksonModule());
        assertThat(mapper.writeValueAsString(new Patient())).contains("138****8888")
                .doesNotContain("13812348888");
    }

    @Test
    void startsWithJackson3AndWithoutJackson2() {
        runner.withClassLoader(new FilteredClassLoader("com.fasterxml.jackson.databind"))
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(PrivacyJackson3Module.class).doesNotHaveBean(PrivacyJacksonModule.class));
    }

    @Test
    void startsWithJackson2AndWithoutJackson3() {
        runner.withClassLoader(new FilteredClassLoader("tools.jackson"))
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(PrivacyJacksonModule.class).doesNotHaveBean(PrivacyJackson3Module.class));
    }

    @Test
    void startsWithoutEitherOptionalJacksonLibrary() {
        runner.withClassLoader(new FilteredClassLoader("tools.jackson", "com.fasterxml.jackson.databind"))
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean(PrivacyJacksonModule.class).doesNotHaveBean(PrivacyJackson3Module.class));
    }

    @Test
    void respectsUserSuppliedPrivacyModule() {
        PrivacyJackson3Module customModule = new PrivacyJackson3Module();
        runner.withBean("customPrivacyModule", PrivacyJackson3Module.class, () -> customModule)
                .run(context -> {
                    assertThat(context).hasSingleBean(PrivacyJackson3Module.class);
                    assertThat(context.getBean(PrivacyJackson3Module.class)).isSameAs(customModule);
                });
    }

    public static class Patient {
        public String id = "9007199254740993";
        @Privacy(type = PrivacyType.PHONE)
        public String phone = "13812348888";
        @Privacy(type = PrivacyType.ID_CARD)
        public String identity = "110101199001011234";
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public static class ArrayPatient {
        @Privacy(type = PrivacyType.PHONE)
        public String phone = "13812348888";
    }

    public static class UnwrappedPatient {
        @JsonUnwrapped(prefix = "contact_")
        public Patient patient = new Patient();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class OptionalPatient {
        @Privacy(type = PrivacyType.PHONE)
        @JsonProperty("mobile_number")
        public String phone = "13812348888";
        @Privacy
        public String empty = "";
        @Privacy
        public String absent;
        @Privacy
        public Integer count = 7;
    }
}
