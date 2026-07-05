package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.common.PageQuery;
import com.campusscore.common.PageResult;
import com.campusscore.security.AppUserDetails;
import com.campusscore.service.StudentScoreService;
import com.campusscore.web.dto.ScoreItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生视角接口。见 {@code contracts/api.md §3}。
 */
@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentScoreService studentScoreService;

    /** 我的成绩：分页 + 排序。 */
    @GetMapping("/scores")
    public ApiResponse<PageResult<ScoreItemResponse>> myScores(
            @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort) {
        PageQuery q = PageQuery.builder().page(page).size(size).sort(sort).build();
        return ApiResponse.ok(studentScoreService.listMyScores(principal.getId(), q));
    }
}
