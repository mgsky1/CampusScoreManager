package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.domain.Subject;
import com.campusscore.security.AppUserDetails;
import com.campusscore.service.TeacherSubjectService;
import com.campusscore.web.dto.CreateSubjectRequest;
import com.campusscore.web.dto.SubjectItemResponse;
import com.campusscore.web.dto.UpdateSubjectRequest;
import java.util.stream.Collectors;
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
 * 教师端 · 我的课程 CRUD。见 {@code contracts/api.md §5}。
 */
@RestController
@RequestMapping("/api/v1/teacher/subjects")
@RequiredArgsConstructor
public class TeacherSubjectController {

    private final TeacherSubjectService teacherSubjectService;

    /** 5.1 分页查询本教师授课记录。 */
    @GetMapping
    public ApiResponse<PageResult<SubjectItemResponse>> listMine(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort) {
        PageQuery q = PageQuery.builder()
                .page(page).size(size).keyword(keyword).sort(sort).build();
        PageResult<Subject> src = teacherSubjectService.listMine(principal.getId(), q);
        return ApiResponse.ok(PageResult.of(
                src.getItems().stream().map(SubjectItemResponse::from).collect(Collectors.toList()),
                src.getTotal(),
                src.getPage(),
                src.getSize()));
    }

    /** 5.2 新增授课记录。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubjectItemResponse> create(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody CreateSubjectRequest body) {
        return ApiResponse.ok(SubjectItemResponse.from(
                teacherSubjectService.create(principal.getId(), body)));
    }

    /** 5.3 修改授课记录。 */
    @PostMapping("/{id}/update")
    public ApiResponse<SubjectItemResponse> update(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateSubjectRequest body) {
        return ApiResponse.ok(SubjectItemResponse.from(
                teacherSubjectService.update(principal.getId(), id, body)));
    }

    /** 5.4 删除授课记录。 */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AppUserDetails principal,
            @PathVariable("id") long id) {
        teacherSubjectService.delete(principal.getId(), id);
        return ApiResponse.ok(null);
    }
}
