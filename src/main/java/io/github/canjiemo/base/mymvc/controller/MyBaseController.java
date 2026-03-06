package io.github.canjiemo.base.mymvc.controller;


import io.github.canjiemo.base.mymvc.configuration.MyMvcProperties;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.base.mymvc.support.MyExceptionResponseResolver;
import io.github.canjiemo.base.mymvc.support.MyResponseFactory;
import io.github.canjiemo.mycommon.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

public class MyBaseController {

	@Deprecated(since = "1.0.2")
	public static final String LOGIN_ERROR_MSG = "非法授权,请先登录";
	@Deprecated(since = "1.0.2")
	public static final String PERMISSION_ERROR_MSG = "您没有权限，请联系管理员授权";
	@Deprecated(since = "1.0.2")
	public static final String REQUEST_ERROR_MSG = "请求参数格式错误";
	@Deprecated(since = "1.0.2")
	public static final String DUPLICATEKEY_ERROR_MSG = "系统已经存在该记录";

	private static final MyMvcProperties DEFAULT_PROPERTIES = new MyMvcProperties();
	private static final MyResponseFactory DEFAULT_RESPONSE_FACTORY = new MyResponseFactory(DEFAULT_PROPERTIES);
	private static final MyExceptionResponseResolver DEFAULT_EXCEPTION_RESOLVER =
			new MyExceptionResponseResolver(DEFAULT_RESPONSE_FACTORY, DEFAULT_PROPERTIES);

	@Autowired(required = false)
	private MyResponseFactory responseFactory;

	@Autowired(required = false)
	private MyExceptionResponseResolver exceptionResponseResolver;

	protected @ResponseBody
	<T> MyResponseResult<T> doJsonPagerOut(T pager){
		return getResponseFactory().success(pager);
	}

	protected @ResponseBody
	<T> MyResponseResult<T> doJsonOut(T data){
		return getResponseFactory().success(data);
	}

	protected @ResponseBody <T> MyResponseResult<T> doJsonOut(int code, String msg, T data){
		return getResponseFactory().result(code,msg, data);
	}

	protected @ResponseBody <T> MyResponseResult<T> doJsonOut(String msg, T data){
		return getResponseFactory().success(msg, data);
	}

	protected @ResponseBody MyResponseResult<Void> doJsonMsg(int code, String msg){
		return getResponseFactory().error(code, msg);
	}

	protected @ResponseBody MyResponseResult<Void> doJsonMsg(String msg){
		return getResponseFactory().successMessage(msg);
	}

	protected @ResponseBody MyResponseResult<Void> doJsonDefaultMsg(){
		return getResponseFactory().defaultSuccess();
	}


	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(BaseException.class)
	protected MyResponseResult<Void> handleException(BaseException e){
		return getExceptionResponseResolver().handleBaseException(e);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected MyResponseResult<Void> handleDuplicateKeyException(HttpRequestMethodNotSupportedException e){
		return getExceptionResponseResolver().handleHttpRequestMethodNotSupportedException(e);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected MyResponseResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
		return getExceptionResponseResolver().handleHttpMessageNotReadableException(e);
	}

	@ExceptionHandler(BindException.class)
	protected MyResponseResult<Void> handleException(BindException e){
		return getExceptionResponseResolver().handleBindingResult(e.getBindingResult());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected MyResponseResult<Void> handleException(MethodArgumentNotValidException e){
		return getExceptionResponseResolver().handleMethodArgumentNotValidException(e);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	protected MyResponseResult<Void> handleException(HandlerMethodValidationException e){
		return getExceptionResponseResolver().handleHandlerMethodValidationException(e);
	}

	@ExceptionHandler(Exception.class)
	protected MyResponseResult<Void> handleException(Exception e){
		return getExceptionResponseResolver().handleException(e);
	}

	protected MyResponseFactory getResponseFactory() {
		return responseFactory != null ? responseFactory : DEFAULT_RESPONSE_FACTORY;
	}

	protected MyExceptionResponseResolver getExceptionResponseResolver() {
		return exceptionResponseResolver != null ? exceptionResponseResolver : DEFAULT_EXCEPTION_RESOLVER;
	}
}
