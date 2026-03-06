package io.github.canjiemo.base.mymvc.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;

public class DateValidator implements ConstraintValidator<Date, String> {

	private String message;
    private boolean required;
    private String format;


	@Override
	public void initialize(Date paramA) {
		this.message = paramA.message();
        this.required = paramA.required();
        this.format = paramA.format();
	}

	@Override
	public boolean isValid(String requestVal, ConstraintValidatorContext context) {
		if(required && (requestVal == null || requestVal.trim().isEmpty())){
            if(!StringUtils.isBlank(message)){
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
            }
            return false;
        }

        if(requestVal == null || requestVal.trim().isEmpty()){
            return true;
        }

        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setLenient(false);
            ParsePosition parsePosition = new ParsePosition(0);
            java.util.Date parsedDate = dateFormat.parse(requestVal, parsePosition);
            if (parsedDate != null && parsePosition.getIndex() == requestVal.length()) {
                return true;
            }
        }catch (IllegalArgumentException ignored){
            // Invalid date pattern configuration is treated as validation failure.
        }

        if(!StringUtils.isBlank(message)){
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }
        return false;
	}
}
