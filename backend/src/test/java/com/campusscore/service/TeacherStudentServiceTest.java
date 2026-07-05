package com.campusscore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Role;
import com.campusscore.domain.Student;
import com.campusscore.domain.User;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.web.dto.CreateStudentRequest;
import com.campusscore.web.dto.UpdateStudentRequest;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** TeacherStudentService 单元测试 (T112)。 */
@ExtendWith(MockitoExtension.class)
class TeacherStudentServiceTest {

    @Mock StudentMapper studentMapper;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks TeacherStudentService service;

    private StudentListItem stu(long id, String loginName, String realName, int grade) {
        return StudentListItem.builder()
                .id(id).loginName(loginName).realName(realName)
                .tel("13800138000").address("SMU").grade(grade).build();
    }

    private CreateStudentRequest createReq(String loginName) {
        CreateStudentRequest r = new CreateStudentRequest();
        r.setLoginName(loginName);
        r.setRealName("测试学生");
        r.setPassword("Campus@123");
        r.setTel("13800138000");
        r.setAddress("SMU");
        r.setGrade(2);
        return r;
    }

    private UpdateStudentRequest updateReq(String loginName) {
        UpdateStudentRequest r = new UpdateStudentRequest();
        r.setLoginName(loginName);
        r.setRealName("更新的学生");
        r.setTel("13900139000");
        r.setAddress("New Addr");
        r.setGrade(3);
        return r;
    }

    // ---------- search / findById ----------

    @Test
    void searchReturnsPageResult() {
        when(studentMapper.search(any(), eq(0), eq(20), any()))
                .thenReturn(Arrays.asList(stu(3L, "hhh", "黄同学", 3), stu(4L, "zs", "张三", 2)));
        when(studentMapper.count(any())).thenReturn(2L);

        PageResult<StudentListItem> r = service.search(PageQuery.builder().page(1).size(20).build());
        assertThat(r.getTotal()).isEqualTo(2L);
        assertThat(r.getItems()).hasSize(2);
    }

    @Test
    void findByIdThrowsNotFound() {
        when(studentMapper.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- create ----------

    @Test
    void createInsertsUserAndStudentAndReturnsDetail() {
        when(userMapper.countByLoginNameIgnoreCase("newstu")).thenReturn(0L);
        when(passwordEncoder.encode("Campus@123")).thenReturn("$2a$10$hashed");
        // 模拟 insert 回填 id
        org.mockito.Mockito.doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(studentMapper.findById(101L)).thenReturn(Optional.of(stu(101L, "newstu", "测试学生", 2)));

        StudentListItem result = service.create(createReq("newstu"));
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getLoginName()).isEqualTo("newstu");

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCap.capture());
        assertThat(userCap.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(userCap.getValue().getRole()).isEqualTo(Role.STUDENT);

        ArgumentCaptor<Student> stuCap = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).insertStudentRow(stuCap.capture());
        assertThat(stuCap.getValue().getId()).isEqualTo(101L);
        assertThat(stuCap.getValue().getGrade()).isEqualTo(2);
    }

    @Test
    void createThrows2101WhenLoginNameTaken() {
        when(userMapper.countByLoginNameIgnoreCase("dup")).thenReturn(1L);
        assertThatThrownBy(() -> service.create(createReq("dup")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录名");
        verify(userMapper, never()).insert(any());
        verify(studentMapper, never()).insertStudentRow(any());
    }

    // ---------- update ----------

    @Test
    void updateValidReturnsDetail() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                User.builder().id(3L).loginName("hhh").realName("旧").role(Role.STUDENT).build()));
        when(userMapper.countByLoginNameIgnoreCaseExcludingId("hhh", 3L)).thenReturn(0L);
        when(studentMapper.findById(3L)).thenReturn(Optional.of(stu(3L, "hhh", "更新的学生", 3)));

        StudentListItem r = service.update(3L, updateReq("hhh"));
        assertThat(r.getRealName()).isEqualTo("更新的学生");

        // password 未提供 → 不应 encode
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateEncodesPasswordWhenProvided() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                User.builder().id(3L).loginName("hhh").realName("旧").role(Role.STUDENT).build()));
        when(userMapper.countByLoginNameIgnoreCaseExcludingId("hhh", 3L)).thenReturn(0L);
        when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$newhash");
        when(studentMapper.findById(3L)).thenReturn(Optional.of(stu(3L, "hhh", "更新的学生", 3)));

        UpdateStudentRequest req = updateReq("hhh");
        req.setPassword("NewPass123");
        service.update(3L, req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateBasic(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("$2a$10$newhash");
    }

    @Test
    void updateThrows1404WhenStudentMissing() {
        when(userMapper.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, updateReq("x")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrows1404WhenUserIsNotStudent() {
        when(userMapper.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).loginName("ttt").realName("田老师").role(Role.TEACHER).build()));
        assertThatThrownBy(() -> service.update(1L, updateReq("x")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrows2101OnLoginNameConflict() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                User.builder().id(3L).loginName("hhh").realName("旧").role(Role.STUDENT).build()));
        when(userMapper.countByLoginNameIgnoreCaseExcludingId("zs", 3L)).thenReturn(1L);
        assertThatThrownBy(() -> service.update(3L, updateReq("zs")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录名");
    }

    // ---------- delete ----------

    @Test
    void deleteValidCallsUserMapperDelete() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                User.builder().id(3L).loginName("hhh").realName("旧").role(Role.STUDENT).build()));
        service.delete(3L);
        verify(userMapper).deleteById(3L);
    }

    @Test
    void deleteThrows1404WhenMissing() {
        when(userMapper.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);
        verify(userMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteThrows1404WhenNotStudent() {
        when(userMapper.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).loginName("ttt").realName("田老师").role(Role.TEACHER).build()));
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(NotFoundException.class);
        verify(userMapper, never()).deleteById(anyLong());
    }
}
