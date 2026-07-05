package com.campusscore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.domain.Role;
import com.campusscore.domain.Student;
import com.campusscore.domain.Teacher;
import com.campusscore.domain.User;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.TeacherMapper;
import com.campusscore.persistence.UserMapper;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.web.dto.ChangePasswordRequest;
import com.campusscore.web.dto.ProfileResponse;
import com.campusscore.web.dto.UpdateProfileRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** AccountService 单元测试 (T150)。 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock UserMapper userMapper;
    @Mock StudentMapper studentMapper;
    @Mock TeacherMapper teacherMapper;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AccountService service;

    private User user(long id, Role role, String loginName, String realName, String hash) {
        return User.builder()
                .id(id).role(role).loginName(loginName)
                .realName(realName).passwordHash(hash).build();
    }

    private StudentListItem stuRow(long id, int grade) {
        return StudentListItem.builder()
                .id(id).loginName("s" + id).realName("学生" + id)
                .tel("13800138000").address("SMU").grade(grade).build();
    }

    // ---------- getMyProfile ----------

    @Test
    void getMyProfileForStudentIncludesTelAddressGrade() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                user(3L, Role.STUDENT, "zs", "张三", "hash")));
        when(studentMapper.findById(3L)).thenReturn(Optional.of(stuRow(3L, 2)));

        ProfileResponse r = service.getMyProfile(3L);
        assertThat(r.getRole()).isEqualTo(Role.STUDENT);
        assertThat(r.getTel()).isEqualTo("13800138000");
        assertThat(r.getAddress()).isEqualTo("SMU");
        assertThat(r.getGrade()).isEqualTo(2);
    }

    @Test
    void getMyProfileForTeacherIncludesTelOnly() {
        when(userMapper.findById(1L)).thenReturn(Optional.of(
                user(1L, Role.TEACHER, "ttt", "田老师", "hash")));
        when(teacherMapper.findById(1L)).thenReturn(Optional.of(
                Teacher.builder().id(1L).tel("18065853353").build()));

        ProfileResponse r = service.getMyProfile(1L);
        assertThat(r.getRole()).isEqualTo(Role.TEACHER);
        assertThat(r.getTel()).isEqualTo("18065853353");
        assertThat(r.getAddress()).isNull();
        assertThat(r.getGrade()).isNull();
    }

    // ---------- updateMyProfile ----------

    @Test
    void updateProfileForStudentPreservesGradeAndUpdatesTelAddress() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                user(3L, Role.STUDENT, "zs", "张三", "hash")));
        when(studentMapper.findById(3L)).thenReturn(Optional.of(stuRow(3L, 2)));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setTel("13900139000");
        req.setAddress("厦大翔安");
        service.updateMyProfile(3L, req);

        ArgumentCaptor<Student> cap = ArgumentCaptor.forClass(Student.class);
        verify(studentMapper).updateStudentRow(cap.capture());
        Student s = cap.getValue();
        assertThat(s.getTel()).isEqualTo("13900139000");
        assertThat(s.getAddress()).isEqualTo("厦大翔安");
        assertThat(s.getGrade()).isEqualTo(2); // grade 不可通过本接口修改
        // 教师方法不应被调用
        verify(teacherMapper, never()).updateTeacherRow(any());
    }

    @Test
    void updateProfileForStudentRejectsBlankAddress() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                user(3L, Role.STUDENT, "zs", "张三", "hash")));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setTel("13900139000");
        req.setAddress("   ");

        assertThatThrownBy(() -> service.updateMyProfile(3L, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));

        verify(studentMapper, never()).updateStudentRow(any());
    }

    @Test
    void updateProfileForTeacherIgnoresAddress() {
        when(userMapper.findById(1L)).thenReturn(Optional.of(
                user(1L, Role.TEACHER, "ttt", "田老师", "hash")));
        when(teacherMapper.findById(1L)).thenReturn(Optional.of(
                Teacher.builder().id(1L).tel("18065853353").build()));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setTel("13900139000");
        req.setAddress("这行会被忽略");
        service.updateMyProfile(1L, req);

        ArgumentCaptor<Teacher> cap = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherMapper).updateTeacherRow(cap.capture());
        assertThat(cap.getValue().getTel()).isEqualTo("13900139000");
        // student mapper 不应被调用
        verify(studentMapper, never()).updateStudentRow(any());
    }

    // ---------- changeMyPassword ----------

    @Test
    void changePasswordSucceeds() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                user(3L, Role.STUDENT, "zs", "张三", "old-hash")));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass1")).thenReturn("new-hash");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("newpass1");
        req.setConfirmPassword("newpass1");
        service.changeMyPassword(3L, req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateBasic(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void changePasswordThrows2002WhenOldPasswordMismatch() {
        when(userMapper.findById(3L)).thenReturn(Optional.of(
                user(3L, Role.STUDENT, "zs", "张三", "hash")));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newpass1");
        req.setConfirmPassword("newpass1");

        assertThatThrownBy(() -> service.changeMyPassword(3L, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OLD_PASSWORD_MISMATCH));

        verify(userMapper, never()).updateBasic(any());
    }

    @Test
    void changePasswordThrows2003WhenConfirmMismatch() {
        // service 层不再依赖 findById（会短路），但保守起见让它成立
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("newpass1");
        req.setConfirmPassword("different");

        assertThatThrownBy(() -> service.changeMyPassword(3L, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CONFIRM_PASSWORD_MISMATCH));

        verify(userMapper, never()).updateBasic(any());
    }
}
