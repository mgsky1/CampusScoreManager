package com.campusscore.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusscore.domain.User;
import com.campusscore.support.SharedMysqlExtension;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;

/**
 * MyBatis slice test: 只加载 MyBatis 相关 bean + 真实 DataSource（由
 * {@link SharedMysqlExtension} 提供）。V5 dev seed 保证有 6 个用户可查。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = SharedMysqlExtension.class)
class UserMapperTest {

    @Autowired private UserMapper userMapper;

    @Test
    void findByLoginNameFindsSeededTeacher() {
        Optional<User> u = userMapper.findByLoginName("ttt");
        assertThat(u).isPresent();
        assertThat(u.get().getRealName()).isEqualTo("田老师");
        assertThat(u.get().getRole().name()).isEqualTo("TEACHER");
        assertThat(u.get().getPasswordHash()).startsWith("$2a$10$");
    }

    @Test
    void findByLoginNameIsCaseInsensitive() {
        assertThat(userMapper.findByLoginName("TTT")).isPresent();
        assertThat(userMapper.findByLoginName("TtT")).isPresent();
        assertThat(userMapper.findByLoginName("ttt")).isPresent();
    }

    @Test
    void findByLoginNameReturnsEmptyForUnknown() {
        assertThat(userMapper.findByLoginName("no-such-user-here")).isEmpty();
    }

    @Test
    void findByIdWorks() {
        Optional<User> u = userMapper.findById(3L);
        assertThat(u).isPresent();
        assertThat(u.get().getLoginName()).isEqualTo("hhh");
        assertThat(u.get().getRole().name()).isEqualTo("STUDENT");
    }

    @Test
    void findByIdReturnsEmptyForUnknown() {
        assertThat(userMapper.findById(99999L)).isEmpty();
    }
}
