package io.github.canjiemo.base.mymvc.controller;

import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.base.mymvc.support.MyExceptionResponseResolver;
import io.github.canjiemo.mycommon.exception.BaseException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class MyMvcExceptionHandler {

    private final MyExceptionResponseResolver exceptionResponseResolver;

    public MyMvcExceptionHandler(MyExceptionResponseResolver exceptionResponseResolver) {
        this.exceptionResponseResolver = exceptionResponseResolver;
    }

    @ExceptionHandler(BaseException.class)
    public MyResponseResult<Void> handleException(BaseException e) {
        return exceptionResponseResolver.handleBaseException(e);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public MyResponseResult<Void> handleException(HttpRequestMethodNotSupportedException e) {
        return exceptionResponseResolver.handleHttpRequestMethodNotSupportedException(e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public MyResponseResult<Void> handleException(HttpMessageNotReadableException e) {
        return exceptionResponseResolver.handleHttpMessageNotReadableException(e);
    }

    @ExceptionHandler(BindException.class)
    public MyResponseResult<Void> handleException(BindException e) {
        return exceptionResponseResolver.handleBindingResult(e.getBindingResult());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MyResponseResult<Void> handleException(MethodArgumentNotValidException e) {
        return exceptionResponseResolver.handleMethodArgumentNotValidException(e);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public MyResponseResult<Void> handleException(HandlerMethodValidationException e) {
        return exceptionResponseResolver.handleHandlerMethodValidationException(e);
    }

    @ExceptionHandler(Exception.class)
    public MyResponseResult<Void> handleException(Exception e) {
        return exceptionResponseResolver.handleException(e);
    }
}
