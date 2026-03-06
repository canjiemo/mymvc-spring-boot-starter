package io.github.canjiemo.base.mymvc.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DateValidatorTest {

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Test
    void acceptsStrictlyValidDate() {
        DateRequest request = new DateRequest();
        request.date = "2024-02-29";

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidCalendarDate() {
        DateRequest request = new DateRequest();
        request.date = "2024-02-31";

        Set<ConstraintViolation<DateRequest>> violations = VALIDATOR.validate(request);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("请输入正确的日期");
    }

    @Test
    void rejectsTrailingCharacters() {
        DateRequest request = new DateRequest();
        request.date = "2024-02-29abc";

        Set<ConstraintViolation<DateRequest>> violations = VALIDATOR.validate(request);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("请输入正确的日期");
    }

    static class DateRequest {

        @Date(format = "yyyy-MM-dd", message = "请输入正确的日期")
        String date;
    }
}
