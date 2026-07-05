package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.persistence.dto.StudentListItem;
import com.campusscore.service.TeacherStudentService;
import com.campusscore.web.dto.CreateStudentRequest;
import com.campusscore.web.dto.StudentDetailResponse;
import com.campusscore.web.dto.UpdateStudentRequest;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端 · 学生管理。见 {@code contracts/api.md §4}。
 */
@RestController
@RequestMapping("/api/v1/teacher/students")
@RequiredArgsConstructor
public class TeacherStudentController {

    private final TeacherStudentService teacherStudentService;

    /** 4.1 分页 + 关键字搜索学生。 */
    @GetMapping
    public ApiResponse<PageResult<StudentDetailResponse>> search(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort) {
        PageQuery q = PageQuery.builder()
                .page(page).size(size).keyword(keyword).sort(sort).build();
        PageResult<StudentListItem> src = teacherStudentService.search(q);
        return ApiResponse.ok(PageResult.of(
                src.getItems().stream().map(StudentDetailResponse::from).collect(Collectors.toList()),
                src.getTotal(),
                src.getPage(),
                src.getSize()));
    }

    /** 4.2 按 id 查看学生档案。 */
    @GetMapping("/{id}")
    public ApiResponse<StudentDetailResponse> detail(@PathVariable("id") long id) {
        return ApiResponse.ok(StudentDetailResponse.from(teacherStudentService.findById(id)));
    }

    /** 4.3 新增学生。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentDetailResponse> create(
            @Valid @RequestBody CreateStudentRequest body) {
        return ApiResponse.ok(StudentDetailResponse.from(teacherStudentService.create(body)));
    }

    /** 4.4 修改学生资料。 */
    @PostMapping("/{id}/update")
    public ApiResponse<StudentDetailResponse> update(
            @PathVariable("id") long id,
            @Valid @RequestBody UpdateStudentRequest body) {
        return ApiResponse.ok(StudentDetailResponse.from(teacherStudentService.update(id, body)));
    }

    /** 4.5 删除学生（级联清理成绩）。 */
    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable("id") long id) {
        teacherStudentService.delete(id);
        return ApiResponse.ok(null);
    }
}
