package com.smartgrain.common.exception;

import com.smartgrain.common.api.Result;
import com.smartgrain.common.api.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * - 业务异常 → 返回对应业务码 + HTTP 200（便于前端按 code 判断）
 * - 认证/权限异常 → 返回 401/403
 * - 参数校验异常 → 400 + 40001
 * - 其他未知异常 → 500 + 50000（堆栈打印到日志，不暴露给前端）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("[BusinessException] code={}, msg={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getResultCode(), ex.getMessage());
    }

    /** 参数校验 - @Valid RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /** 参数校验 - @ModelAttribute */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /** 参数校验 - @Validated Param */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("[TypeMismatch] {}", ex.getMessage());
        return Result.fail(ResultCode.PARAM_INVALID, "参数类型错误: " + ex.getName());
    }

    /** 唯一键冲突 */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException ex) {
        log.warn("[DuplicateKey] {}", ex.getMessage());
        return Result.fail(ResultCode.DUPLICATE_KEY);
    }

    /** Spring Security - 未认证 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        log.warn("[Auth] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ResultCode.UNAUTHORIZED, ex.getMessage()));
    }

    /** Spring Security - 无权限 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[AccessDenied] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ResultCode.FORBIDDEN));
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoHandlerFoundException ex) {
        log.warn("[NotFound] {}", ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(ResultCode.NOT_FOUND));
    }

    /** 兜底异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Result<Void>> handleUnknown(Throwable ex) {
        log.error("[UnknownException]", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ResultCode.INTERNAL_ERROR));
    }
}
