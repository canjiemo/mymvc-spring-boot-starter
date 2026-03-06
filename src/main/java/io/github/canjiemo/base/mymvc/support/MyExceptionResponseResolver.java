package io.github.canjiemo.base.mymvc.support;

import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.exception.BaseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyExceptionResponseResolver {

    private static final Logger log = LoggerFactory.getLogger(MyExceptionResponseResolver.class);

    private final MyResponseFactory responseFactory;
    private final MyMvcProperties properties;

    public MyExceptionResponseResolver(MyResponseFactory responseFactory, MyMvcProperties properties) {
        this.responseFactory = responseFactory;
        this.properties = properties;
    }

    public MyResponseResult<Void> handleBaseException(BaseException e) {
        return responseFactory.error(e.getCode(), e.getMessage());
    }

    public MyResponseResult<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return responseFactory.error(HttpStatus.METHOD_NOT_ALLOWED.value(), e.getMessage());
    }

    public MyResponseResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = e.getMessage();
        log.warn("JSON解析异常: {}", message);

        if (message != null) {
            if (message.contains("JSON parse error")) {
                return responseFactory.error(HttpStatus.BAD_REQUEST.value(), properties.getMessages().getJsonParseError());
            }
            if (message.contains("Cannot deserialize")) {
                return responseFactory.error(HttpStatus.BAD_REQUEST.value(), properties.getMessages().getJsonTypeError());
            }
            if (message.contains("Required request body is missing")) {
                return responseFactory.error(HttpStatus.BAD_REQUEST.value(), properties.getMessages().getMissingRequestBodyError());
            }
        }

        return responseFactory.error(HttpStatus.BAD_REQUEST.value(), properties.getMessages().getRequestError());
    }

    public MyResponseResult<Void> handleBindingResult(BindingResult bindingResult) {
        return responseFactory.error(HttpStatus.BAD_REQUEST.value(), getErrorsMsg(bindingResult));
    }

    public MyResponseResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return handleBindingResult(e.getBindingResult());
    }

    public MyResponseResult<Void> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        return responseFactory.error(HttpStatus.BAD_REQUEST.value(), getErrorsMsg(e));
    }

    public MyResponseResult<Void> handleException(Exception e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        Throwable cause = rootCause != null ? rootCause : e;

        if (cause instanceof BaseException baseException) {
            return responseFactory.error(baseException.getCode(), baseException.getMessage());
        }
        if (cause.getClass().getName().equalsIgnoreCase("org.springframework.dao.DuplicateKeyException")) {
            return responseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), properties.getMessages().getDuplicateKeyError());
        }
        if (cause.getClass().getName().equalsIgnoreCase("org.springframework.security.access.AccessDeniedException")) {
            return responseFactory.error(HttpStatus.FORBIDDEN.value(), properties.getMessages().getPermissionError());
        }
        if (cause.getClass().getName().equalsIgnoreCase("org.springframework.security.core.AuthenticationException")) {
            return responseFactory.error(HttpStatus.FORBIDDEN.value(), properties.getMessages().getLoginError());
        }
        if (cause instanceof HttpMediaTypeNotSupportedException) {
            return responseFactory.error(HttpStatus.UNPROCESSABLE_ENTITY.value(), properties.getMessages().getRequestError());
        }
        log.error("处理异常", e);
        return responseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), properties.getMessages().getFallbackError());
    }

    private String getErrorsMsg(BindingResult result) {
        List<String> errMsg = new ArrayList<>();
        for (FieldError error : result.getFieldErrors()) {
            errMsg.add(resolveMessage(error));
        }
        for (ObjectError error : result.getGlobalErrors()) {
            errMsg.add(resolveMessage(error));
        }
        return joinErrors(errMsg);
    }

    private String getErrorsMsg(HandlerMethodValidationException exception) {
        List<String> errMsg = new ArrayList<>();
        for (ParameterValidationResult validationResult : exception.getAllValidationResults()) {
            String parameterName = validationResult.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : validationResult.getResolvableErrors()) {
                String message = resolveMessage(error);
                if (StringUtils.hasText(parameterName) && StringUtils.hasText(message)) {
                    errMsg.add(parameterName + " " + message);
                } else {
                    errMsg.add(message);
                }
            }
        }
        return joinErrors(errMsg);
    }

    private String resolveMessage(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        if (StringUtils.hasText(message)) {
            return message;
        }

        String[] codes = error.getCodes();
        if (codes != null && codes.length > 0) {
            return codes[0];
        }

        return properties.getMessages().getRequestError();
    }

    private String joinErrors(List<String> errMsg) {
        List<String> messages = errMsg.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
        if (messages.isEmpty()) {
            return properties.getMessages().getRequestError();
        }
        return String.join(",", messages);
    }
}
