package com.novamart.auth.web;

import com.novamart.auth.dto.AuthDtos.AddressRequest;
import com.novamart.auth.dto.AuthDtos.AddressResponse;
import com.novamart.auth.service.AddressService;
import com.novamart.common.api.ApiResponse;
import com.novamart.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@Tag(name = "Users", description = "Address book")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "List the caller's saved addresses")
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.of("Addresses retrieved", addressService.listFor(CurrentUser.requireId()));
    }

    @PostMapping
    @Operation(summary = "Add an address")
    public ResponseEntity<ApiResponse<AddressResponse>> create(@Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.create(CurrentUser.requireId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Address saved", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address")
    public ApiResponse<AddressResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody AddressRequest request) {
        return ApiResponse.of("Address updated",
                addressService.update(CurrentUser.requireId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        addressService.delete(CurrentUser.requireId(), id);
        return ResponseEntity.noContent().build();
    }
}
