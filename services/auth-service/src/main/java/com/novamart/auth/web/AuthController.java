package com.novamart.auth.web;

import com.novamart.auth.dto.AuthDtos.AuthTokens;
import com.novamart.auth.dto.AuthDtos.LoginRequest;
import com.novamart.auth.dto.AuthDtos.RefreshRequest;
import com.novamart.auth.dto.AuthDtos.RegisterRequest;
import com.novamart.auth.service.AuthService;
import com.novamart.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the {@code /auth} half of the API contract.
 *
 * <p>Controllers in this codebase do three things and nothing else: bind and
 * validate input, delegate to a service, and choose a status code. There is no
 * business logic here to test around.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, sign-in and token lifecycle")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a new shopper account")
    public ResponseEntity<ApiResponse<AuthTokens>> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Account created successfully", tokens));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Exchange credentials for a token pair")
    public ApiResponse<AuthTokens> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of("Signed in successfully", authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotate an expired access token")
    public ApiResponse<AuthTokens> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of("Session refreshed", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the caller's refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
