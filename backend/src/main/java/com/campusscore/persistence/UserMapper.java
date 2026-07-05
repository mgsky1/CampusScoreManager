package com.campusscore.persistence;

import com.campusscore.domain.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * User 持久层。SQL 定义在 {@code resources/mapper/UserMapper.xml}。
 */
@Mapper
public interface UserMapper {

    /**
     * 按登录名查用户（大小写不敏感，匹配 {@code LOWER(login_name)} 唯一索引）。
     */
    Optional<User> findByLoginName(@Param("loginName") String loginName);

    /** 按主键查用户。 */
    Optional<User> findById(@Param("id") Long id);

    /**
     * 插入 user 行；成功后回填 {@link User#getId()}。
     */
    int insert(User user);

    /**
     * 更新 user 的可变字段：{@code real_name} / {@code login_name} / {@code password_hash}（后者仅在非 null 时更新）。
     */
    int updateBasic(User user);

    /** 按 id 删除 user（`student` / `teacher` / `score` 通过外键 CASCADE 级联清理）。 */
    int deleteById(@Param("id") long id);

    /** 大小写不敏感统计登录名条数。 */
    long countByLoginNameIgnoreCase(@Param("loginName") String loginName);

    /** 大小写不敏感统计登录名条数，排除指定 id（用于更新时的冲突检测）。 */
    long countByLoginNameIgnoreCaseExcludingId(
            @Param("loginName") String loginName, @Param("id") long id);
}
