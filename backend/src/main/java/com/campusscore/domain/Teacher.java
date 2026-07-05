package com.campusscore.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 教师扩展信息，对应 {@code teacher} 表。主键与 {@link User#getId()} 相同。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {
    private Long id;
    private String tel;
}
