package io.github.canjiemo.base.mymvc.controller;


import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.exception.BaseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.MessageSourceResolvable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyBaseController {


	public static final String LOGIN_ERROR_MSG = "非法授权,请先登录";
	public static final String PERMISSION_ERROR_MSG = "您没有权限，请联系管理员授权";
	public static final String REQUEST_ERROR_MSG = "请求参数格式错误";
	public static final String DUPLICATEKEY_ERROR_MSG = "系统已经存在该记录";

	private static final Logger log = LoggerFactory.getLogger(MyBaseController.class);

	protected @ResponseBody
	<T> MyResponseResult<T> doJsonPagerOut(T pager){
		return new MyResponseResult(pager);
	}

	protected @ResponseBody
	MyResponseResult doJsonOut(Object data){
		return new MyResponseResult(data);
	}

	protected @ResponseBody MyResponseResult doJsonOut(int code, String msg, Object data){
		return new MyResponseResult(code,msg, data);
	}

	protected @ResponseBody MyResponseResult doJsonOut(String msg, Object data){
		return new MyResponseResult(msg, data);
	}

	protected @ResponseBody MyResponseResult doJsonMsg(int code, String msg){
		return new MyResponseResult(code, msg);
	}

	protected @ResponseBody MyResponseResult doJsonMsg(String msg){
		return doJsonMsg(200,msg);
	}

	protected @ResponseBody MyResponseResult doJsonDefaultMsg(){
		return doJsonMsg(200,"操作成功");
	}


	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(BaseException.class)
	protected MyResponseResult handleException(BaseException e){
		return doJsonMsg(e.getCode(), e.getMessage());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	protected MyResponseResult handleDuplicateKeyException(HttpRequestMethodNotSupportedException e){
		return doJsonMsg(HttpStatus.METHOD_NOT_ALLOWED.value(), e.getMessage());
	}
	@ExceptionHandler(HttpMessageNotReadableException.class)
	protected MyResponseResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
		String message = e.getMessage();
		log.warn("JSON解析异常: {}", message);

		// 更精确的错误信息
		if (message != null) {
			if (message.contains("JSON parse error")) {
				return doJsonMsg(HttpStatus.BAD_REQUEST.value(), "JSON格式错误，请检查请求参数");
			} else if (message.contains("Cannot deserialize")) {
				return doJsonMsg(HttpStatus.BAD_REQUEST.value(), "参数类型错误，请检查数据类型");
			} else if (message.contains("Required request body is missing")) {
				return doJsonMsg(HttpStatus.BAD_REQUEST.value(), "缺少请求体");
			}
		}

		return doJsonMsg(HttpStatus.BAD_REQUEST.value(), REQUEST_ERROR_MSG);
	}

	@ExceptionHandler(BindException.class)
	protected MyResponseResult handleException(BindException e){
		BindingResult bindingResult = e.getBindingResult();
		return doJsonMsg(HttpStatus.BAD_REQUEST.value(), getErrorsMsg(bindingResult));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	protected MyResponseResult handleException(MethodArgumentNotValidException e){
		return doJsonMsg(HttpStatus.BAD_REQUEST.value(), getErrorsMsg(e.getBindingResult()));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	protected MyResponseResult handleException(HandlerMethodValidationException e){
		return doJsonMsg(HttpStatus.BAD_REQUEST.value(), getErrorsMsg(e));
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

		return REQUEST_ERROR_MSG;
	}

	private String joinErrors(List<String> errMsg) {
		List<String> messages = errMsg.stream()
				.filter(StringUtils::hasText)
				.distinct()
				.sorted()
				.collect(Collectors.toCollection(ArrayList::new));
		if (messages.isEmpty()) {
			return REQUEST_ERROR_MSG;
		}
		return String.join(",", messages);
	}


	@ExceptionHandler(Exception.class)
	protected MyResponseResult handleException(Exception e){
		Throwable te = ExceptionUtils.getRootCause(e);
		if(te==null){
			List<Throwable> throwableList = ExceptionUtils.getThrowableList(e);
			te = throwableList.get(0);
		}
		if(te instanceof BaseException) {
			BaseException be = (BaseException)te;
			return doJsonMsg(be.getCode(), be.getMessage());
		}
		if(te.getClass().getName().equalsIgnoreCase("org.springframework.dao.DuplicateKeyException")){
			return doJsonMsg(HttpStatus.INTERNAL_SERVER_ERROR.value(), DUPLICATEKEY_ERROR_MSG);
		}
		if(te.getClass().getName().equalsIgnoreCase("org.springframework.security.access.AccessDeniedException")){
			return doJsonMsg(HttpStatus.FORBIDDEN.value(), PERMISSION_ERROR_MSG);
		}
		if(te.getClass().getName().equalsIgnoreCase("org.springframework.security.core.AuthenticationException")){
			return doJsonMsg(HttpStatus.FORBIDDEN.value(), LOGIN_ERROR_MSG);
		}
		if(te instanceof HttpMediaTypeNotSupportedException){
			return doJsonMsg(HttpStatus.UNPROCESSABLE_ENTITY.value(), REQUEST_ERROR_MSG);
		}
		log.error("处理异常",e);
		return doJsonMsg(HttpStatus.INTERNAL_SERVER_ERROR.value(), "请求失败,请稍后再试");
	}

}
