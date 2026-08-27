package com.novamart.auth.dto;

import com.novamart.auth.domain.Address;
import com.novamart.auth.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Every request and response shape this service exposes, plus the mapping from
 * entity to response.
 *
 * <p>Entities are deliberately never returned from a controller. Serialising a
 * {@code User} directly would publish {@code passwordHash} the moment someone
 * added a getter, and would tie the public contract to the database schema so
 * that a column rename becomes a breaking API change.
 *
 * <p>Grouping the records in one file keeps a small, tightly related vocabulary
 * readable in a single screen instead of scattering fifteen three-line files.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    // ---------- requests ----------

    public record RegisterRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
            @Size(max = 20) String phone) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @Size(max = 20) String phone) {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 40) String label,
            @NotBlank @Size(max = 120) String recipientName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 16) String postalCode,
            @NotBlank @Size(max = 80) String country,
            boolean isDefault) {
    }

    public record UpdateUserStatusRequest(boolean enabled) {
    }

    public record UpdateUserRoleRequest(Set<String> roles) {
    }

    // ---------- responses ----------

    public record AuthTokens(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserProfile user) {
    }

    public record UserProfile(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            Set<String> roles,
            boolean enabled,
            Instant createdAt) {

        public static UserProfile from(User user) {
            return new UserProfile(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRoles(),
                    user.isEnabled(),
                    user.getCreatedAt());
        }
    }

    public record AddressResponse(
            UUID id,
            String label,
            String recipientName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            boolean isDefault) {

        public static AddressResponse from(Address address) {
            return new AddressResponse(
                    address.getId(),
                    address.getLabel(),
                    address.getRecipientName(),
                    address.getPhone(),
                    address.getLine1(),
                    address.getLine2(),
                    address.getCity(),
                    address.getState(),
                    address.getPostalCode(),
                    address.getCountry(),
                    address.isDefaultAddress());
        }

        public static List<AddressResponse> from(List<Address> addresses) {
            return addresses.stream().map(AddressResponse::from).toList();
        }
    }
}
