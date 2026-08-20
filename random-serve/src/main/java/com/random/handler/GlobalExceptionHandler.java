package com.random.handler;

import com.random.exception.BaseException;
import com.random.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器。
 *
 * <p>统一处理业务异常、参数校验、参数类型、请求体格式等各类异常，
 * 返回标准化错误结果，避免将细节异常直接暴露给前端。</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBusiness(BaseException ex) {
        log.error("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理参数校验失败（@Valid/@Validated 触发）。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null
                ? fieldError.getField() + " " + fieldError.getDefaultMessage()
                : "参数校验失败";
        log.error("参数校验失败：{}", msg);
        return Result.error(msg);
    }

    /**
     * 处理参数类型不匹配（如 @PathVariable Long id 传了非数字）。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("参数类型错误：{} = {}", ex.getName(), ex.getValue());
        return Result.error("参数类型错误：" + ex.getName());
    }

    /**
     * 处理请求体格式错误（JSON 解析失败）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.error("请求体格式错误：{}", ex.getMessage());
        return Result.error("请求体格式错误");
    }

    /**
     * 处理缺少必填参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        log.error("缺少参数：{}", ex.getParameterName());
        return Result.error("缺少参数：" + ex.getParameterName());
    }

    /**
     * 处理数据库唯一约束冲突。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException ex) {
        log.error("数据重复：{}", ex.getMessage());
        return Result.error("数据已存在，请勿重复提交");
    }

    /**
     * 处理无权限访问（@PreAuthorize 校验失败）。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException ex) {
        log.warn("无权限访问：{}", ex.getMessage());
        return Result.error("无权限访问该功能");
    }

    /**
     * 处理文件上传大小超限异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        String msg = "上传文件过大，请确保文件大小不超过 " + e.getMaxUploadSize() / (1024 * 1024) + "MB";
        log.warn("文件上传超限：{}", e.getMessage());
        return Result.error(msg);
    }

    /**
     * 处理其他未预期的系统异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常：", ex);
        return Result.error("系统异常，请稍后重试");
    }

}
