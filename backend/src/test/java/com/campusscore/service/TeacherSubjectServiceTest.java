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

import com.campusscore.common.ErrorCode;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.common.exception.BusinessException;
import com.campusscore.common.exception.ForbiddenException;
import com.campusscore.common.exception.NotFoundException;
import com.campusscore.domain.Subject;
import com.campusscore.persistence.SubjectMapper;
import com.campusscore.web.dto.CreateSubjectRequest;
import com.campusscore.web.dto.UpdateSubjectRequest;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** TeacherSubjectService 单元测试 (T132)。 */
@ExtendWith(MockitoExtension.class)
class TeacherSubjectServiceTest {

    @Mock SubjectMapper subjectMapper;

    @InjectMocks TeacherSubjectService service;

    private Subject sub(long id, long teacherId, String name, int grade) {
        return Subject.builder().id(id).teacherId(teacherId).name(name).grade(grade).build();
    }

    private CreateSubjectRequest createReq(String name, int grade) {
        CreateSubjectRequest r = new CreateSubjectRequest();
        r.setName(name);
        r.setGrade(grade);
        return r;
    }

    private UpdateSubjectRequest updateReq(String name, int grade) {
        UpdateSubjectRequest r = new UpdateSubjectRequest();
        r.setName(name);
        r.setGrade(grade);
        return r;
    }

    // ---------- listMine ----------

    @Test
    void listMineReturnsOnlyOwnedSubjectsPaged() {
        when(subjectMapper.selectMineByTeacher(
                eq(1L), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(Arrays.asList(sub(1, 1, "计算机导论", 1), sub(2, 1, "汇编语言", 2)));
        when(subjectMapper.countMineByTeacher(eq(1L), any())).thenReturn(2L);

        PageResult<Subject> r = service.listMine(1L, PageQuery.builder().page(1).size(20).build());
        assertThat(r.getItems()).hasSize(2);
        assertThat(r.getTotal()).isEqualTo(2L);
    }

    // ---------- create ----------

    @Test
    void createInsertsAndReturnsSubjectWhenNameFree() {
        when(subjectMapper.existsByTeacherAndName(1L, "云计算")).thenReturn(0L);
        // insert 内部由 mybatis 回填 id，用 ArgumentCaptor + then answer 模拟
        org.mockito.Mockito.doAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(999L);
            return 1;
        }).when(subjectMapper).insert(any(Subject.class));
        when(subjectMapper.findById(999L)).thenReturn(Optional.of(sub(999, 1, "云计算", 3)));

        Subject r = service.create(1L, createReq("云计算", 3));
        assertThat(r.getId()).isEqualTo(999L);
        assertThat(r.getName()).isEqualTo("云计算");

        ArgumentCaptor<Subject> cap = ArgumentCaptor.forClass(Subject.class);
        verify(subjectMapper).insert(cap.capture());
        assertThat(cap.getValue().getTeacherId()).isEqualTo(1L);
    }

    @Test
    void createThrows2201WhenSameTeacherNameTaken() {
        when(subjectMapper.existsByTeacherAndName(1L, "Java EE")).thenReturn(1L);

        assertThatThrownBy(() -> service.create(1L, createReq("Java EE", 3)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER));

        verify(subjectMapper, never()).insert(any());
    }

    @Test
    void createAllowsDifferentTeacherToHaveSameName() {
        // 教师 2 的视角下同名不冲突
        when(subjectMapper.existsByTeacherAndName(2L, "计算机导论")).thenReturn(0L);
        org.mockito.Mockito.doAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(1000L);
            return 1;
        }).when(subjectMapper).insert(any(Subject.class));
        when(subjectMapper.findById(1000L)).thenReturn(Optional.of(sub(1000, 2, "计算机导论", 1)));

        Subject r = service.create(2L, createReq("计算机导论", 1));
        assertThat(r.getTeacherId()).isEqualTo(2L);
    }

    // ---------- update ----------

    @Test
    void updateChangesFieldsWhenOwnerAndNameFree() {
        when(subjectMapper.findById(2L)).thenReturn(Optional.of(sub(2, 1, "汇编语言", 2)));
        when(subjectMapper.existsByTeacherAndNameExcludingId(1L, "汇编语言进阶", 2L)).thenReturn(0L);
        when(subjectMapper.update(any(Subject.class))).thenReturn(1);
        when(subjectMapper.findById(2L)).thenReturn(
                Optional.of(sub(2, 1, "汇编语言", 2)),  // pre-check
                Optional.of(sub(2, 1, "汇编语言进阶", 3))  // post-update return
        );

        Subject r = service.update(1L, 2L, updateReq("汇编语言进阶", 3));
        assertThat(r.getName()).isEqualTo("汇编语言进阶");
        assertThat(r.getGrade()).isEqualTo(3);
    }

    @Test
    void updateThrows1404WhenSubjectMissing() {
        when(subjectMapper.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, 999L, updateReq("x", 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrows1403WhenOwnerMismatch() {
        when(subjectMapper.findById(5L)).thenReturn(Optional.of(sub(5, 2, "Java EE", 3)));
        assertThatThrownBy(() -> service.update(1L, 5L, updateReq("Java EE 改", 3)))
                .isInstanceOf(ForbiddenException.class);
        verify(subjectMapper, never()).update(any());
    }

    @Test
    void updateThrows2201WhenRenamedIntoOwnDuplicate() {
        when(subjectMapper.findById(2L)).thenReturn(Optional.of(sub(2, 1, "汇编语言", 2)));
        when(subjectMapper.existsByTeacherAndNameExcludingId(1L, "计算机导论", 2L)).thenReturn(1L);
        assertThatThrownBy(() -> service.update(1L, 2L, updateReq("计算机导论", 1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER));
    }

    // ---------- delete ----------

    @Test
    void deleteSucceedsWhenOwnerAndNoScores() {
        when(subjectMapper.findById(2L)).thenReturn(Optional.of(sub(2, 1, "汇编语言", 2)));
        when(subjectMapper.countScoresBySubjectId(2L)).thenReturn(0L);
        when(subjectMapper.deleteById(2L)).thenReturn(1);

        service.delete(1L, 2L);
        verify(subjectMapper).deleteById(2L);
    }

    @Test
    void deleteThrows1404WhenSubjectMissing() {
        when(subjectMapper.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 999L))
                .isInstanceOf(NotFoundException.class);
        verify(subjectMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteThrows1403WhenOwnerMismatch() {
        when(subjectMapper.findById(5L)).thenReturn(Optional.of(sub(5, 2, "Java EE", 3)));
        assertThatThrownBy(() -> service.delete(1L, 5L))
                .isInstanceOf(ForbiddenException.class);
        verify(subjectMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteThrows2202WhenScoresExist() {
        when(subjectMapper.findById(5L)).thenReturn(Optional.of(sub(5, 1, "Java EE", 3)));
        when(subjectMapper.countScoresBySubjectId(5L)).thenReturn(3L);
        assertThatThrownBy(() -> service.delete(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBJECT_HAS_SCORES));
        verify(subjectMapper, never()).deleteById(anyLong());
    }
}
