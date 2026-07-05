package com.campusscore.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 学生扩展信息，对应 {@code student} 表。主键与 {@link User#getId()} 相同。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    private Long id;
    private String tel;
    private String address;
    private Integer grade;
}
