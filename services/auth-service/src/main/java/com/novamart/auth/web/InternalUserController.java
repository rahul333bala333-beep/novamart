package com.novamart.auth.web;

import com.novamart.auth.dto.AuthDtos.AddressResponse;
import com.novamart.auth.dto.AuthDtos.UserProfile;
import com.novamart.auth.service.AddressService;
import com.novamart.auth.service.UserService;
import com.novamart.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints other Nova Mart services call, not browsers.
 *
 * <p>order-service needs the shopper's email to raise a notification and their
 * chosen address to snapshot onto the order. It cannot read {@code auth_db}, so
 * it asks over HTTP like any other client.
 *
 * <p>Everything here requires the {@code SERVICE} role, which is only granted by
 * presenting the internal token. The gateway never forwards that header from
 * outside, so these paths are unreachable from the internet even though they sit
 * on the same port.
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@PreAuthorize("hasRole('SERVICE')")
@Tag(name = "Internal", description = "Service-to-service only")
public class InternalUserController {

    private final UserService userService;
    private final AddressService addressService;

    public InternalUserController(UserService userService, AddressService addressService) {
        this.userService = userService;
        this.addressService = addressService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Resolve a user for another service")
    public ApiResponse<UserProfile> get(@PathVariable UUID userId) {
        return ApiResponse.of("User retrieved", userService.profileOf(userId));
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Resolve one of a user's addresses for another service")
    public ApiResponse<AddressResponse> address(@PathVariable UUID userId, @PathVariable UUID addressId) {
        return ApiResponse.of("Address retrieved", addressService.getFor(userId, addressId));
    }
}
