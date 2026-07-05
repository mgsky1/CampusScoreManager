package com.campusscore.service;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.NotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号自服务：查询 / 更新档案，修改密码。见 {@code contracts/api.md §2}。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;

    // ---------- Profile ----------

    public ProfileResponse getMyProfile(long userId) {
        User u = userMapper.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return buildProfile(u);
    }

    @Transactional
    public ProfileResponse updateMyProfile(long userId, UpdateProfileRequest req) {
        User u = userMapper.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (u.getRole() == Role.STUDENT) {
            // 学生必须同时提供 address（DTO 定义为可选，此处兜底强校验）
            String address = req.getAddress();
            if (address == null || address.trim().isEmpty()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED, "address 不能为空");
            }
            StudentListItem s = studentMapper.findById(userId)
                    .orElseThrow(() -> new NotFoundException("学生扩展信息不存在"));
            studentMapper.updateStudentRow(Student.builder()
                    .id(userId)
                    .tel(req.getTel())
                    .address(address.trim())
                    .grade(s.getGrade())  // 保持 grade 不变（合约禁止此处修改）
                    .build());
        } else if (u.getRole() == Role.TEACHER) {
            teacherMapper.updateTeacherRow(Teacher.builder()
                    .id(userId).tel(req.getTel()).build());
        } else {
            throw new BusinessException(ErrorCode.FORBIDDEN, "未知角色");
        }
        return getMyProfile(userId);
    }

    // ---------- Password ----------

    @Transactional
    public void changeMyPassword(long userId, ChangePasswordRequest req) {
        // Bean Validation 已保证 newPassword.equals(confirmPassword)；此处再复核并映射专用错误码
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(
                    ErrorCode.CONFIRM_PASSWORD_MISMATCH, "新密码与确认密码不一致");
        }
        User u = userMapper.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        if (!passwordEncoder.matches(req.getOldPassword(), u.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.OLD_PASSWORD_MISMATCH, "原密码错误");
        }
        String newHash = passwordEncoder.encode(req.getNewPassword());
        userMapper.updateBasic(User.builder()
                .id(userId)
                .loginName(u.getLoginName())
                .realName(u.getRealName())
                .passwordHash(newHash)
                .build());
    }

    // ---------- helpers ----------

    private ProfileResponse buildProfile(User u) {
        ProfileResponse.ProfileResponseBuilder b = ProfileResponse.builder()
                .id(u.getId())
                .loginName(u.getLoginName())
                .realName(u.getRealName())
                .role(u.getRole());
        if (u.getRole() == Role.STUDENT) {
            StudentListItem s = studentMapper.findById(u.getId())
                    .orElseThrow(() -> new NotFoundException("学生扩展信息不存在"));
            b.tel(s.getTel()).address(s.getAddress()).grade(s.getGrade());
        } else if (u.getRole() == Role.TEACHER) {
            Teacher t = teacherMapper.findById(u.getId()).orElse(null);
            if (t != null) b.tel(t.getTel());
        }
        return b.build();
    }
}
