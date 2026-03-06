package io.github.canjiemo.base.mymvc.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mymvc")
public class MyMvcProperties {

    private final Response response = new Response();
    private final Messages messages = new Messages();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    public Response getResponse() {
        return response;
    }

    public Messages getMessages() {
        return messages;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public static class Response {
        private int successCode = 200;
        private String successMessage = "OK";
        private String defaultSuccessMessage = "操作成功";

        public int getSuccessCode() {
            return successCode;
        }

        public void setSuccessCode(int successCode) {
            this.successCode = successCode;
        }

        public String getSuccessMessage() {
            return successMessage;
        }

        public void setSuccessMessage(String successMessage) {
            this.successMessage = successMessage;
        }

        public String getDefaultSuccessMessage() {
            return defaultSuccessMessage;
        }

        public void setDefaultSuccessMessage(String defaultSuccessMessage) {
            this.defaultSuccessMessage = defaultSuccessMessage;
        }
    }

    public static class Messages {
        private String loginError = "非法授权,请先登录";
        private String permissionError = "您没有权限，请联系管理员授权";
        private String requestError = "请求参数格式错误";
        private String duplicateKeyError = "系统已经存在该记录";
        private String fallbackError = "请求失败,请稍后再试";
        private String jsonParseError = "JSON格式错误，请检查请求参数";
        private String jsonTypeError = "参数类型错误，请检查数据类型";
        private String missingRequestBodyError = "缺少请求体";

        public String getLoginError() {
            return loginError;
        }

        public void setLoginError(String loginError) {
            this.loginError = loginError;
        }

        public String getPermissionError() {
            return permissionError;
        }

        public void setPermissionError(String permissionError) {
            this.permissionError = permissionError;
        }

        public String getRequestError() {
            return requestError;
        }

        public void setRequestError(String requestError) {
            this.requestError = requestError;
        }

        public String getDuplicateKeyError() {
            return duplicateKeyError;
        }

        public void setDuplicateKeyError(String duplicateKeyError) {
            this.duplicateKeyError = duplicateKeyError;
        }

        public String getFallbackError() {
            return fallbackError;
        }

        public void setFallbackError(String fallbackError) {
            this.fallbackError = fallbackError;
        }

        public String getJsonParseError() {
            return jsonParseError;
        }

        public void setJsonParseError(String jsonParseError) {
            this.jsonParseError = jsonParseError;
        }

        public String getJsonTypeError() {
            return jsonTypeError;
        }

        public void setJsonTypeError(String jsonTypeError) {
            this.jsonTypeError = jsonTypeError;
        }

        public String getMissingRequestBodyError() {
            return missingRequestBodyError;
        }

        public void setMissingRequestBodyError(String missingRequestBodyError) {
            this.missingRequestBodyError = missingRequestBodyError;
        }
    }

    public static class ExceptionHandler {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
