package com.campusscore.web.advice;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.ErrorCode;
import com.campusscore.common.FieldError;
import com.campusscore.common.exception.AuthException;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.ConflictException;
import com.campusscore.common.exception.ForbiddenException;
import com.campusscore.common.exception.NotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常 → 统一响应信封映射。对应 {@code contracts/api.md §0.2 / §0.3}。
 *
 * <p>规则：
 *
 * <ul>
 *   <li>Bean Validation 失败 → {@link ErrorCode#VALIDATION_FAILED}（400 / 1000）+ 字段错误列表；
 *   <li>业务异常 → 由 {@link BusinessException#getErrorCode()} 决定；
 *   <li>Spring Security 相关异常 → 401 / 403 分派；
 *   <li>其它未捕获 → 500 / 1500，**不泄漏堆栈**（只记录到日志）。
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 字段校验 ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> onMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<FieldError> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> FieldError.of(
                                fe.getField(),
                                mapValidationCode(fe.getCode()),
                                fe.getDefaultMessage()))
                        .collect(Collectors.toList());
        return build(
                ErrorCode.VALIDATION_FAILED,
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, "请求参数校验失败", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> onConstraintViolation(
            ConstraintViolationException ex) {
        List<FieldError> errors =
                ex.getConstraintViolations().stream()
                        .map(GlobalExceptionHandler::toFieldError)
                        .collect(Collectors.toList());
        return build(
                ErrorCode.VALIDATION_FAILED,
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, "请求参数校验失败", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> onBadJson(HttpMessageNotReadableException ex) {
        return build(
                ErrorCode.VALIDATION_FAILED,
                ApiResponse.error(ErrorCode.VALIDATION_FAILED, "请求体格式错误"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> onMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {
        return build(
                ErrorCode.NOT_FOUND,
                ApiResponse.error(ErrorCode.NOT_FOUND, "方法不允许"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> onNoHandler(NoHandlerFoundException ex) {
        return build(
                ErrorCode.NOT_FOUND,
                ApiResponse.error(ErrorCode.NOT_FOUND, "资源不存在"));
    }

    // ---------- 鉴权 ----------

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> onAuth(AuthException ex) {
        return build(
                ex.getErrorCode(),
                ApiResponse.error(ex.getErrorCode(), safeMessage(ex, "未认证")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> onSpringAuth(AuthenticationException ex) {
        return build(
                ErrorCode.UNAUTHORIZED,
                ApiResponse.error(ErrorCode.UNAUTHORIZED, "未认证"));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> onForbidden(ForbiddenException ex) {
        return build(
                ex.getErrorCode(),
                ApiResponse.error(ex.getErrorCode(), safeMessage(ex, "无权限")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> onAccessDenied(AccessDeniedException ex) {
        return build(
                ErrorCode.FORBIDDEN,
                ApiResponse.error(ErrorCode.FORBIDDEN, "无权限"));
    }

    // ---------- 业务 ----------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> onNotFound(NotFoundException ex) {
        return build(
                ex.getErrorCode(),
                ApiResponse.error(ex.getErrorCode(), safeMessage(ex, "资源不存在")));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> onConflict(ConflictException ex) {
        return build(
                ex.getErrorCode(),
                ApiResponse.error(ex.getErrorCode(), safeMessage(ex, "资源冲突")));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> onBusiness(BusinessException ex) {
        return build(
                ex.getErrorCode(),
                ApiResponse.error(ex.getErrorCode(), safeMessage(ex, "业务异常")));
    }

    // ---------- 兜底 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> onUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(
                ErrorCode.INTERNAL_ERROR,
                ApiResponse.error(ErrorCode.INTERNAL_ERROR, "服务器内部错误"));
    }

    // ---------- helpers ----------

    private static ResponseEntity<ApiResponse<Void>> build(
            ErrorCode ec, ApiResponse<Void> body) {
        return ResponseEntity.status(HttpStatus.valueOf(ec.httpStatus())).body(body);
    }

    private static String safeMessage(Throwable ex, String fallback) {
        String msg = ex.getMessage();
        return msg == null || msg.isEmpty() ? fallback : msg;
    }

    private static FieldError toFieldError(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath() == null ? "" : cv.getPropertyPath().toString();
        // e.g. "createStudent.body.tel" → "tel"
        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        String code =
                cv.getConstraintDescriptor() == null
                        ? "VALIDATION_FAILED"
                        : mapValidationCode(
                                cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        return FieldError.of(field, code, cv.getMessage());
    }

    /**
     * 将 Bean Validation 注解名映射到 {@code contracts/api.md §0.3} 的字段错误 code。
     */
    private static String mapValidationCode(String annotation) {
        if (annotation == null) {
            return "VALIDATION_FAILED";
        }
        switch (annotation) {
            case "NotBlank":
            case "NotNull":
            case "NotEmpty":
                return "VALIDATION_REQUIRED";
            case "Size":
            case "Length":
                return "VALIDATION_LENGTH";
            case "Pattern":
            case "Email":
                return "VALIDATION_PATTERN";
            case "Digits":
                return "VALIDATION_NUMERIC";
            case "Min":
            case "Max":
            case "Range":
            case "DecimalMin":
            case "DecimalMax":
                return "VALIDATION_RANGE";
            case "AssertTrue":
            case "AssertFalse":
                return "VALIDATION_MISMATCH";
            default:
                return "VALIDATION_FAILED";
        }
    }
}
