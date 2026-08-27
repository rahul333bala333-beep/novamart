package com.novamart.auth.service;

import com.novamart.auth.domain.User;
import com.novamart.auth.dto.AuthDtos.UpdateProfileRequest;
import com.novamart.auth.dto.AuthDtos.UserProfile;
import com.novamart.auth.repository.UserRepository;
import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserProfile profileOf(UUID userId) {
        return UserProfile.from(load(userId));
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.updateProfile(request.firstName().trim(), request.lastName().trim(), request.phone());
        return UserProfile.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfile> list(String search, Pageable pageable) {
        String term = StringUtils.hasText(search) ? search.trim() : null;
        return PageResponse.from(users.search(term, pageable), UserProfile::from);
    }

    @Transactional
    public UserProfile updateStatus(UUID userId, boolean enabled) {
        User user = load(userId);
        user.setEnabled(enabled);
        return UserProfile.from(users.save(user));
    }

    @Transactional
    public UserProfile updateRoles(UUID userId, java.util.Set<String> roles) {
        User user = load(userId);
        if (roles != null && !roles.isEmpty()) {
            user.setRoles(roles);
        }
        return UserProfile.from(users.save(user));
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
