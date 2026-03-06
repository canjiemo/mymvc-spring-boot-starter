package io.github.canjiemo.base.mymvc.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LimitLengthValidatorTest {

    private static final Validator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Test
    void usesCustomMessageWhenValueIsOutOfRange() {
        LengthRequest request = new LengthRequest();
        request.text = "a";

        Set<ConstraintViolation<LengthRequest>> violations = VALIDATOR.validate(request);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("长度不合法");
    }

    @Test
    void usesCustomMessageWhenRequiredValueIsBlank() {
        LengthRequest request = new LengthRequest();
        request.text = "";

        Set<ConstraintViolation<LengthRequest>> violations = VALIDATOR.validate(request);
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("长度不合法");
    }

    static class LengthRequest {

        @LimitLength(min = 2, max = 4, message = "长度不合法")
        String text;
    }
}
