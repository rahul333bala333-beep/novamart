package com.novamart.notification.web;

import com.novamart.common.api.ApiResponse;
import com.novamart.common.api.PageResponse;
import com.novamart.notification.domain.Notification;
import com.novamart.notification.dto.NotificationDtos.CreateNotificationRequest;
import com.novamart.notification.dto.NotificationDtos.NotificationResponse;
import com.novamart.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
@Tag(name = "Notifications", description = "Transactional message log (mock delivery)")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications; a shopper sees only their own")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Notification.Type type) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.of("Notifications retrieved", notificationService.list(type, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get number of unread notifications for caller")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.of("Unread count", notificationService.getUnreadCount(com.novamart.common.security.CurrentUser.requireId()));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ApiResponse.of("Notification marked as read",
                notificationService.markAsRead(id, com.novamart.common.security.CurrentUser.requireId()));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for caller")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        notificationService.markAllAsRead(com.novamart.common.security.CurrentUser.requireId());
        return ResponseEntity.ok(ApiResponse.of("All notifications marked as read", "SUCCESS"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE')")
    @Operation(summary = "Record and dispatch a notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Notification recorded", notificationService.create(request)));
    }
}
