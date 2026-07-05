package com.campusscore.web.controller;

import com.campusscore.common.ApiResponse;
import com.campusscore.security.AppUserDetails;
import com.campusscore.service.AccountService;
import com.campusscore.web.dto.ChangePasswordRequest;
import com.campusscore.web.dto.ProfileResponse;
import com.campusscore.web.dto.UpdateProfileRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号自服务。见 {@code contracts/api.md §2}。
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** 2.1 GET /profile */
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(
            @AuthenticationPrincipal AppUserDetails principal) {
        return ApiResponse.ok(accountService.getMyProfile(principal.getId()));
    }

    /** 2.2 POST /profile/update */
    @PostMapping("/profile/update")
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest body) {
        return ApiResponse.ok(accountService.updateMyProfile(principal.getId(), body));
    }

    /** 2.3 POST /password/change */
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest body) {
        accountService.changeMyPassword(principal.getId(), body);
        return ApiResponse.ok(null);
    }
}
