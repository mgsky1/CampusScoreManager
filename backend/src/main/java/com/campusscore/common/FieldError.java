package com.campusscore.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 单字段校验错误。对应 {@code contracts/api.md §0.3}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {
    /** 字段名（业务层面，如 {@code loginName}）。 */
    private String field;

    /** 校验错误的稳定 code，供前端映射本地文案（如 {@code VALIDATION_PATTERN}）。 */
    private String code;

    /** 人类可读消息（可选）。 */
    private String message;

    public static FieldError of(String field, String code, String message) {
        return new FieldError(field, code, message);
    }
}
