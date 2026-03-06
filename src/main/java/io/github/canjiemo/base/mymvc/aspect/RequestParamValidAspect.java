package io.github.canjiemo.base.mymvc.aspect;

import io.github.canjiemo.mycommon.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;



/**
 * spring mvc 请求参数验证拦截
 * 拦截所有带有@org.springframework.validation.annotation.Validated注解的方法或类
 * @author mo
 *
 */
@Aspect
public class RequestParamValidAspect {

    @Pointcut("@annotation(org.springframework.validation.annotation.Validated) || @within(org.springframework.validation.annotation.Validated)")
    public void controllerBefore(){};

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExecutableValidator executableValidator;

    public RequestParamValidAspect(Validator validator) {
        this.executableValidator = validator.forExecutables();
    }

    @Before("controllerBefore()")
    public void before(JoinPoint point){
        Object target = point.getThis();
        Object [] args = point.getArgs();
        Method interfaceMethod = ((MethodSignature)point.getSignature()).getMethod();
        Method method = AopUtils.getMostSpecificMethod(interfaceMethod, target.getClass());
        Set<ConstraintViolation<Object>> validResult = validMethodParams(target, method, args);
        if (!validResult.isEmpty()) {
            String [] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            List<String> errors = validResult.stream()
                    .map(constraintViolation -> formatViolation(constraintViolation, method, parameterNames))
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .toList();
            throw new BusinessException(String.join(", ", errors));
        }
    }

    private <T> Set<ConstraintViolation<T>> validMethodParams(T obj, Method method, Object [] params){
        return executableValidator.validateParameters(obj, method, params);
    }

    private String formatViolation(ConstraintViolation<?> constraintViolation, Method method, String[] parameterNames) {
        Integer paramIndex = null;
        for (Path.Node node : constraintViolation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PARAMETER) {
                paramIndex = node.as(Path.ParameterNode.class).getParameterIndex();
                break;
            }
        }

        String message = constraintViolation.getMessage();
        if (paramIndex == null) {
            return message;
        }

        String paramName = resolveParameterName(method, parameterNames, paramIndex);
        if (StringUtils.hasText(paramName)) {
            return paramName + " " + message;
        }
        return message;
    }

    private String resolveParameterName(Method method, String[] parameterNames, int paramIndex) {
        if (parameterNames != null && paramIndex >= 0 && paramIndex < parameterNames.length) {
            return parameterNames[paramIndex];
        }

        Parameter[] parameters = method.getParameters();
        if (paramIndex >= 0 && paramIndex < parameters.length) {
            return parameters[paramIndex].getName();
        }

        return null;
    }
}
