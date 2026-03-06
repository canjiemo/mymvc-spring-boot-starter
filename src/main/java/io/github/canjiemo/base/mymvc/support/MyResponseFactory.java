package io.github.canjiemo.base.mymvc.support;

import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;

public class MyResponseFactory {

    private final MyMvcProperties properties;

    public MyResponseFactory(MyMvcProperties properties) {
        this.properties = properties;
    }

    public <T> MyResponseResult<T> success(T data) {
        return success(properties.getResponse().getSuccessMessage(), data);
    }

    public <T> MyResponseResult<T> success(String msg, T data) {
        return result(properties.getResponse().getSuccessCode(), msg, data);
    }

    public MyResponseResult<Void> successMessage(String msg) {
        return result(properties.getResponse().getSuccessCode(), msg, null);
    }

    public MyResponseResult<Void> defaultSuccess() {
        return successMessage(properties.getResponse().getDefaultSuccessMessage());
    }

    public <T> MyResponseResult<T> result(int code, String msg, T data) {
        return new MyResponseResult<>(code, msg, data);
    }

    public MyResponseResult<Void> error(int code, String msg) {
        return result(code, msg, null);
    }
}
