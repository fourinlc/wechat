package com.xxx.server.exception;

import com.xxx.server.pojo.RespBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 请求方式不支持
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public RespBean handleException(HttpRequestMethodNotSupportedException e) {
        log.error(e.getMessage(), e);
        return RespBean.error("不支持' " + e.getMethod() + "'请求");
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public RespBean validationBodyException(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();
        StringBuilder stringBuilder = new StringBuilder();
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            stringBuilder.append(constraintViolation.getMessageTemplate());
        }
        return RespBean.error("请填写正确信息：" + stringBuilder);
    }

    /**spring异常处理*/
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RespBean methodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<ObjectError> allErrors = exception.getBindingResult().getAllErrors();
        StringBuilder stringBuilder = new StringBuilder();
        for (ObjectError allError : allErrors) {
            stringBuilder.append(allError.getDefaultMessage());
        }
        return RespBean.error("请填写正确信息：" + stringBuilder.toString());
    }

    // 断言类自定义异常
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public Object businessException(Exception e)
    {
        log.error(e.getMessage(), e);
        return RespBean.error(e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public RespBean notFount(RuntimeException e) {
        // 记录异常信息
        log.error("运行时异常:", e);
        return RespBean.error("未知错误");
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public RespBean validatedBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return RespBean.error(message);
    }

    /**
     * 通用系统异常
     */
    @ExceptionHandler(Exception.class)
    public RespBean handleException(Exception e) {
        log.error(e.getMessage(), e);
        return RespBean.error("服务器错误，请联系管理员");
    }
}
