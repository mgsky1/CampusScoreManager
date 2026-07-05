package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.security.AppUserDetails;
import com.campusscore.service.TeacherScoreService;
import com.campusscore.web.dto.CreateScoreRequest;
import com.campusscore.web.dto.EntryOptionsResponse;
import com.campusscore.web.dto.TeacherScoreItemResponse;
import com.campusscore.web.dto.UpdateScoreRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端 · 成绩管理。见 {@code contracts/api.md §6}。
 */
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherScoreController {

    private final TeacherScoreService teacherScoreService;

    /** 6.1 查看某学生在当前教师课程上的成绩（分页）。 */
    @GetMapping("/students/{studentId}/scores")
    public ApiResponse<PageResult<TeacherScoreItemResponse>> listStudentScores(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("studentId") long studentId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort) {
        PageQuery q = PageQuery.builder().page(page).size(size).sort(sort).build();
        return ApiResponse.ok(
                teacherScoreService.listScoresForStudent(principal.getId(), studentId, q));
    }

    /** 6.2 查询该学生可录入的课程候选。 */
    @GetMapping("/students/{studentId}/entry-options")
    public ApiResponse<EntryOptionsResponse> getEntryOptions(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("studentId") long studentId) {
        return ApiResponse.ok(
                teacherScoreService.getEntryOptions(principal.getId(), studentId));
    }

    /** 6.3 录入成绩。 */
    @PostMapping("/students/{studentId}/scores")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherScoreItemResponse> createScore(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("studentId") long studentId,
            @Valid @RequestBody CreateScoreRequest body) {
        return ApiResponse.ok(teacherScoreService.createScore(
                principal.getId(), studentId, body.getSubjectId(), body.getScore()));
    }

    /** 6.4 修改成绩。 */
    @PostMapping("/scores/{scoreId}/update")
    public ApiResponse<TeacherScoreItemResponse> updateScore(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("scoreId") long scoreId,
            @Valid @RequestBody UpdateScoreRequest body) {
        return ApiResponse.ok(
                teacherScoreService.updateScore(principal.getId(), scoreId, body.getScore()));
    }

    /** 6.5 删除成绩。 */
    @PostMapping("/scores/{scoreId}/delete")
    public ApiResponse<Void> deleteScore(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("scoreId") long scoreId) {
        teacherScoreService.deleteScore(principal.getId(), scoreId);
        return ApiResponse.ok(null);
    }
}
