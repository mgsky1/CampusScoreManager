package com.campusscore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;

import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Score;
import com.campusscore.domain.Subject;
import com.campusscore.persistence.ScoreMapper;
import com.campusscore.persistence.StudentMapper;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.web.dto.EntryOptionsResponse;
import com.campusscore.web.dto.TeacherScoreItemResponse;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.persistence.dto.SubjectEntryOption;
import com.campusscore.persistence.dto.TeacherScoreItem;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/** TeacherScoreService 单元测试 (T085)。 */
@ExtendWith(MockitoExtension.class)
class TeacherScoreServiceTest {

    @Mock ScoreMapper scoreMapper;
    @Mock SubjectMapper subjectMapper;
    @Mock StudentMapper studentMapper;

    @InjectMocks TeacherScoreService service;

    // ---------- listScoresForStudent ----------

    @Test
    void listScoresForStudentReturnsMappedPageWithEditableFlag() {
        when(studentMapper.findById(3L)).thenReturn(Optional.of(
                StudentListItem.builder().id(3L).realName("黄同学").grade(3).build()));
        when(scoreMapper.selectByStudentIdForTeacher(eq(3L), eq(2L), anyInt(), anyInt(), any()))
                .thenReturn(Arrays.asList(
                        TeacherScoreItem.builder().id(1L).subjectId(5L).subjectName("Java EE")
                                .grade(3).score(99).editable(true)
                                .updatedAt(LocalDateTime.of(2026, 7, 4, 12, 0)).build(),
                        TeacherScoreItem.builder().id(2L).subjectId(9L).subjectName("旧课程")
                                .grade(1).score(55).editable(false)
                                .updatedAt(LocalDateTime.of(2024, 6, 1, 0, 0)).build()));
        when(scoreMapper.countByStudentIdForTeacher(3L)).thenReturn(2L);

        PageResult<?> page =
                service.listScoresForStudent(2L, 3L, PageQuery.builder().page(1).size(20).build());

        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getItems()).hasSize(2);
    }

    @Test
    void listScoresForStudentThrowsWhenStudentMissing() {
        when(studentMapper.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () -> service.listScoresForStudent(2L, 99L, new PageQuery()))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- getEntryOptions ----------

    @Test
    void getEntryOptionsReturnsOptionsAndAllEnteredWhenTeacherHasCoverage() {
        when(studentMapper.findById(3L)).thenReturn(Optional.of(
                StudentListItem.builder().id(3L).realName("黄同学").grade(3).build()));
        // teacher has coverage in grade 3 (both subjects taken)
        when(subjectMapper.hasAnyByTeacherAndGrade(2L, 3)).thenReturn(true);
        when(subjectMapper.findEntryOptions(2L, 3, 3L)).thenReturn(Collections.emptyList());

        EntryOptionsResponse r = service.getEntryOptions(2L, 3L);
        assertThat(r.getOptions()).isEmpty();
        assertThat(r.isAllEntered()).isTrue();
        assertThat(r.getStudent().getRealName()).isEqualTo("黄同学");
    }

    @Test
    void getEntryOptionsAllEnteredFalseWhenTeacherHasNoCoverage() {
        when(studentMapper.findById(3L)).thenReturn(Optional.of(
                StudentListItem.builder().id(3L).realName("黄同学").grade(3).build()));
        when(subjectMapper.hasAnyByTeacherAndGrade(1L, 3)).thenReturn(false);
        when(subjectMapper.findEntryOptions(1L, 3, 3L)).thenReturn(Collections.emptyList());

        EntryOptionsResponse r = service.getEntryOptions(1L, 3L);
        assertThat(r.getOptions()).isEmpty();
        assertThat(r.isAllEntered()).isFalse();
    }

    @Test
    void getEntryOptionsReturnsPositiveList() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.hasAnyByTeacherAndGrade(1L, 2)).thenReturn(true);
        when(subjectMapper.findEntryOptions(1L, 2, 4L)).thenReturn(Arrays.asList(
                SubjectEntryOption.builder().subjectId(2L).subjectName("汇编").grade(2).build(),
                SubjectEntryOption.builder().subjectId(3L).subjectName("数据库").grade(2).build()));

        EntryOptionsResponse r = service.getEntryOptions(1L, 4L);
        assertThat(r.getOptions()).hasSize(2);
        assertThat(r.isAllEntered()).isFalse();
    }

    // ---------- createScore ----------

    @Test
    void createScoreValidPathInserts() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.findByIdOwnedByTeacher(2L, 1L)).thenReturn(Optional.of(
                Subject.builder().id(2L).teacherId(1L).grade(2).name("汇编").build()));
        when(scoreMapper.insert(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            s.setId(101L);
            return 1;
        });
        when(scoreMapper.selectByStudentIdForTeacher(eq(4L), eq(1L), anyInt(), anyInt(), any()))
                .thenReturn(Collections.singletonList(
                        TeacherScoreItem.builder().id(101L).subjectId(2L).subjectName("汇编")
                                .grade(2).score(90).editable(true).build()));

        TeacherScoreItemResponse created = service.createScore(1L, 4L, 2L, 90);
        assertThat(created.getId()).isEqualTo(101L);
        assertThat(created.getScore()).isEqualTo(90);
        assertThat(created.isEditable()).isTrue();
    }

    @Test
    void createScoreOutOfRangeThrows2301() {
        assertThatThrownBy(() -> service.createScore(1L, 4L, 2L, -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_OUT_OF_RANGE);
        assertThatThrownBy(() -> service.createScore(1L, 4L, 2L, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_OUT_OF_RANGE);
    }

    @Test
    void createScoreForbiddenWhenSubjectNotOwnedByTeacher() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createScore(1L, 4L, 5L, 88))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_SUBJECT_NOT_OWNED);
    }

    @Test
    void createScoreForbiddenWhenGradeMismatch() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.findByIdOwnedByTeacher(1L, 1L)).thenReturn(Optional.of(
                Subject.builder().id(1L).teacherId(1L).grade(1).name("计算机导论").build()));

        assertThatThrownBy(() -> service.createScore(1L, 4L, 1L, 88))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_SUBJECT_NOT_OWNED);
    }

    @Test
    void createScoreAllEnteredWhenOptionsEmptyButCoverageExists() {
        when(studentMapper.findById(3L)).thenReturn(Optional.of(
                StudentListItem.builder().id(3L).realName("黄同学").grade(3).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 2L)).thenReturn(Optional.of(
                Subject.builder().id(5L).teacherId(2L).grade(3).name("Java EE").build()));
        when(scoreMapper.existsByStudentAndSubject(3L, 5L)).thenReturn(true);
        when(subjectMapper.findEntryOptions(2L, 3, 3L)).thenReturn(Collections.emptyList());
        when(subjectMapper.hasAnyByTeacherAndGrade(2L, 3)).thenReturn(true);

        assertThatThrownBy(() -> service.createScore(2L, 3L, 5L, 88))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_ALL_ENROLLED);
    }

    @Test
    void createScoreDuplicateThrows2302() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.findByIdOwnedByTeacher(2L, 1L)).thenReturn(Optional.of(
                Subject.builder().id(2L).teacherId(1L).grade(2).name("汇编").build()));
        when(scoreMapper.existsByStudentAndSubject(4L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.createScore(1L, 4L, 2L, 88))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_ALREADY_EXISTS);
    }

    @Test
    void createScoreDuplicateKeyExceptionCaughtAs2302() {
        when(studentMapper.findById(4L)).thenReturn(Optional.of(
                StudentListItem.builder().id(4L).realName("张三").grade(2).build()));
        when(subjectMapper.findByIdOwnedByTeacher(2L, 1L)).thenReturn(Optional.of(
                Subject.builder().id(2L).teacherId(1L).grade(2).name("汇编").build()));
        when(scoreMapper.existsByStudentAndSubject(4L, 2L)).thenReturn(false);
        when(scoreMapper.insert(any(Score.class))).thenThrow(new DuplicateKeyException("uk"));

        assertThatThrownBy(() -> service.createScore(1L, 4L, 2L, 88))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_ALREADY_EXISTS);
    }

    // ---------- updateScore ----------

    @Test
    void updateScoreValid() {
        when(scoreMapper.findById(9L)).thenReturn(Optional.of(
                Score.builder().id(9L).studentId(3L).subjectId(5L).teacherId(2L).score(60)
                        .grade(3).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 2L)).thenReturn(Optional.of(
                Subject.builder().id(5L).teacherId(2L).grade(3).name("Java EE").build()));
        when(scoreMapper.updateScore(9L, 95)).thenReturn(1);
        when(scoreMapper.selectByStudentIdForTeacher(eq(3L), eq(2L), anyInt(), anyInt(), any()))
                .thenReturn(Collections.singletonList(
                        TeacherScoreItem.builder().id(9L).subjectId(5L).subjectName("Java EE")
                                .grade(3).score(95).editable(true).build()));

        TeacherScoreItemResponse updated = service.updateScore(2L, 9L, 95);
        assertThat(updated.getScore()).isEqualTo(95);
    }

    @Test
    void updateScoreNotFound1404() {
        when(scoreMapper.findById(9999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateScore(2L, 9999L, 60))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateScoreForbiddenWhenSubjectNoLongerOwnedByTeacher() {
        when(scoreMapper.findById(9L)).thenReturn(Optional.of(
                Score.builder().id(9L).studentId(3L).subjectId(5L).teacherId(2L).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateScore(2L, 9L, 60))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_SUBJECT_NOT_OWNED);
    }

    @Test
    void updateScoreOutOfRange2301() {
        assertThatThrownBy(() -> service.updateScore(2L, 9L, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_OUT_OF_RANGE);
        // 提前校验：不应触发 scoreMapper
        verify(scoreMapper, never()).findById(anyLong());
    }

    // ---------- deleteScore ----------

    @Test
    void deleteScoreValid() {
        when(scoreMapper.findById(9L)).thenReturn(Optional.of(
                Score.builder().id(9L).subjectId(5L).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 2L)).thenReturn(Optional.of(
                Subject.builder().id(5L).teacherId(2L).build()));
        when(scoreMapper.deleteById(9L)).thenReturn(1);

        service.deleteScore(2L, 9L);
        verify(scoreMapper).deleteById(9L);
    }

    @Test
    void deleteScoreNotFound() {
        when(scoreMapper.findById(9999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteScore(2L, 9999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteScoreForbiddenWhenNotOwned() {
        when(scoreMapper.findById(9L)).thenReturn(Optional.of(
                Score.builder().id(9L).subjectId(5L).build()));
        when(subjectMapper.findByIdOwnedByTeacher(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteScore(2L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SCORE_SUBJECT_NOT_OWNED);
    }
}
