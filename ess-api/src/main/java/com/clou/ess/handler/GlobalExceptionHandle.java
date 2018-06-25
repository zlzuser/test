package com.clou.ess.handler;

import com.clou.ess.exception.BootException;
import com.clou.ess.model.ResultBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.util.Set;

/**
 * 全局异常处理（返回JSON）
 */
@RestControllerAdvice
public class GlobalExceptionHandle {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandle.class);

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResultBody handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        logger.error("缺少请求参数: {} ==> 参数类型: {}", e.getParameterName(), e.getParameterType());
        String message = String.format("%s: %s ==> %s: %s", "缺少请求参数", e.getParameterName(), "参数类型", e.getParameterType());
        return new ResultBody(4001, message);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultBody handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.error("参数解析失败", e);
        return new ResultBody(4002, "参数解析失败");
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultBody handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.error("参数验证失败", e);
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String code = error.getDefaultMessage();
        String message = String.format("%s:%s==>%s", "参数验证失败", field, code);
        return new ResultBody(4003, message);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResultBody handleBindException(BindException e) {
        logger.error("参数绑定失败", e);
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String code = error.getDefaultMessage();
        String message = String.format("%s:%s==>%s", "参数绑定失败", field, code);
        return new ResultBody(4004, message);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultBody handleServiceException(ConstraintViolationException e) {
        logger.error("参数验证失败", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String message = String.format("%s:%s", "参数验证失败", violation.getMessage());
        return new ResultBody(4005, message);
    }

    /**
     * 400 - Bad Request
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public ResultBody handleValidationException(ValidationException e) {
        logger.error("参数验证失败", e);
        return new ResultBody(4006, "参数验证异常");
    }

    /**
     * 405 - Method Not Allowed
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultBody handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.error("不支持当前请求方法", e);
        return new ResultBody(4007, "不支持当前请求方法");
    }

    /**
     * 415 - Unsupported Media Type
     */
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResultBody handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        logger.error("不支持当前媒体类型", e);
        return new ResultBody(4008, "不支持当前媒体类型");
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public ResultBody handleServiceException(RuntimeException e) {
        logger.error("业务逻辑异常", e);
        return new ResultBody(5001, "业务逻辑异常");
    }

    /**
     * 操作数据库出现异常:名称重复，外键关联
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResultBody handleException(DataIntegrityViolationException e) {
        logger.error("操作数据库出现异常:", e);
        return new ResultBody(5002, "操作数据库出现异常");
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResultBody handleException(Exception e) {
        logger.error("系统异常", e);
        return new ResultBody(5005, "系统异常");
    }


    /**
     * 自定义异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(BootException.class)
    public ResultBody handleBootException(BootException e) {
        logger.error(e.getMessage());
        return new ResultBody(e.getErrorCode(), e.getMessage());
    }
}
