package com.novamart.auth.web;

import com.novamart.auth.dto.AuthDtos.UpdateProfileRequest;
import com.novamart.auth.dto.AuthDtos.UserProfile;
import com.novamart.auth.service.UserService;
import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "Users", description = "Profile and administration")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the signed-in user's profile")
    public ApiResponse<UserProfile> me() {
        // The id comes from the verified token, never from the request, so a
        // caller cannot read another account by changing a parameter.
        return ApiResponse.of("Profile retrieved", userService.profileOf(CurrentUser.requireId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the signed-in user's profile")
    public ApiResponse<UserProfile> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.of("Profile updated",
                userService.updateProfile(CurrentUser.requireId(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (admin)")
    public ApiResponse<PageResponse<UserProfile>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.of("Users retrieved", userService.list(search, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable a user account (admin)")
    public ApiResponse<UserProfile> updateStatus(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
                                                 @Valid @RequestBody com.novamart.auth.dto.AuthDtos.UpdateUserStatusRequest request) {
        return ApiResponse.of("User status updated", userService.updateStatus(id, request.enabled()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user roles (admin)")
    public ApiResponse<UserProfile> updateRole(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
                                               @Valid @RequestBody com.novamart.auth.dto.AuthDtos.UpdateUserRoleRequest request) {
        return ApiResponse.of("User role updated", userService.updateRoles(id, request.roles()));
    }
}
