package io.github.canjiemo.base.mymvc.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class LimitLengthValidator implements ConstraintValidator<LimitLength, String> {

	private String message;
	private boolean required;
	private int chineseLength;
	private long min;
	private long max;

	@Override
	public void initialize(LimitLength paramA) {
		this.message = paramA.message();
		this.required = paramA.required();
		this.chineseLength = paramA.chineseLength();
		this.min = paramA.min();
		this.max = paramA.max();
	}

	@Override
	public boolean isValid(String text, ConstraintValidatorContext context) {
		if (StringUtils.isBlank(text)) {
			if (required) {
				addConstraintViolation(context, resolveMessage("字段不能为空"));
				return false;
			}
			return true;
		}

		long len = stringLength(text);

		if (len < min || len > max) {
			addConstraintViolation(context, resolveMessage(String.format("字段长度必须在%d~%d之间", min, max)));
			return false;
		}

		return true;
	}
	
	private long stringLength(String value) {
		long valueLength = 0;
		for (int i = 0; i < value.length(); ) {
			int codePoint = value.codePointAt(i);
			if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
				valueLength += chineseLength;
			} else {
				valueLength += 1;
			}
			i += Character.charCount(codePoint);
		}
		return valueLength;
	}

	private String resolveMessage(String fallbackMessage) {
		return StringUtils.isNotBlank(message) ? message : fallbackMessage;
	}

	private void addConstraintViolation(ConstraintValidatorContext context, String violationMessage) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(violationMessage).addConstraintViolation();
	}

}
