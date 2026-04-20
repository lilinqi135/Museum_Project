package com.xiaozhi.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import com.xiaozhi.common.web.ResultMessage;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 
 * @author Joey
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultMessage handleValidationException(Exception e) {
        String errorMessage = "请求参数不合法";
        if (e instanceof MethodArgumentNotValidException methodArgumentNotValidException
                && methodArgumentNotValidException.getBindingResult().hasFieldErrors()) {
            errorMessage = methodArgumentNotValidException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        } else if (e instanceof BindException bindException && bindException.getBindingResult().hasFieldErrors()) {
            errorMessage = bindException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        }
        return ResultMessage.error(errorMessage);
    }

    /**
     * Multipart 缺少必填部分异常
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultMessage handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        String requestPartName = e.getRequestPartName();
        if ("file".equals(requestPartName)) {
            return ResultMessage.error("上传文件不能为空，请在 form-data 中传入 file 字段");
        }
        return ResultMessage.error("缺少必填表单字段: " + requestPartName);
    }

    /**
     * 用户名不存在异常
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultMessage handleUsernameNotFoundException(UsernameNotFoundException e, WebRequest request) {
        logger.warn("用户名不存在异常: {}", e.getMessage(), e);
        return ResultMessage.error("用户名不存在");
    }

    /**
     * 用户密码不匹配异常
     */
    @ExceptionHandler(UserPasswordNotMatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultMessage handleUserPasswordNotMatchException(UserPasswordNotMatchException e, WebRequest request) {
        logger.warn("用户密码不匹配异常: {}", e.getMessage(), e);
        return ResultMessage.error("用户密码不正确");
    }

    /**
     * 权限不足异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResultMessage handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
        logger.warn("权限不足: {}", e.getMessage());
        return ResultMessage.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
    }

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResultMessage handleNotLoginException(NotLoginException e, WebRequest request) {
        return ResultMessage.error(HttpStatus.UNAUTHORIZED.value(), "登录已过期，请重新登录");
    }

    /**
     * Sa-Token 权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResultMessage handleNotPermissionException(NotPermissionException e, WebRequest request) {
        logger.warn("权限不足: {}", e.getMessage());
        return ResultMessage.error(HttpStatus.FORBIDDEN.value(), "权限不足");
    }

    /**
     * Sa-Token 角色不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResultMessage handleNotRoleException(NotRoleException e, WebRequest request) {
        logger.warn("角色权限不足: {}", e.getMessage());
        return ResultMessage.error(HttpStatus.FORBIDDEN.value(), "角色权限不足");
    }

    /**
     * 资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultMessage handleResourceNotFoundException(ResourceNotFoundException e, WebRequest request) {
        logger.warn("资源不存在: {}", e.getMessage());
        return ResultMessage.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    /**
     * 静态资源找不到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultMessage handleNoResourceFoundException(NoResourceFoundException e, WebRequest request) {
        logger.warn("静态资源找不到: {}", e.getResourcePath());
        return ResultMessage.error(HttpStatus.NOT_FOUND.value(), "请求的资源不存在");
    }

    /**
     * 请求路径不存在异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultMessage handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ResultMessage.error(HttpStatus.NOT_FOUND.value(), "请求的接口不存在");
    }

    /**
     * 请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResultMessage handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        logger.warn("请求方法不支持: {} {}, 支持的方法: {}", e.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());
        return ResultMessage.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持");
    }

    /**
     * 异步请求超时异常
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ResultMessage handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e, WebRequest request) {
        logger.warn("异步请求超时: {}", request.getDescription(false));
        return ResultMessage.error(HttpStatus.REQUEST_TIMEOUT.value(), "请求超时，请稍后重试");
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultMessage handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("业务异常: {}", e.getMessage(), e);
        return ResultMessage.error("操作失败：" + e.getMessage());
    }

    /**
     * 系统异常 - 作为最后的兜底处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultMessage handleException(Exception e, WebRequest request) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return ResultMessage.error("服务器错误，请联系管理员");
    }
}
