package com.novamart.notification.dto;

import com.novamart.notification.domain.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record CreateNotificationRequest(
            @NotNull UUID userId,
            @NotNull Notification.Type type,
            Notification.Channel channel,
            @Size(max = 180) String recipient,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String body,
            @Size(max = 80) String referenceId) {
    }

    public record NotificationResponse(
            UUID id,
            UUID userId,
            Notification.Type type,
            Notification.Channel channel,
            String recipient,
            String subject,
            String body,
            String referenceId,
            Notification.Status status,
            boolean read,
            Instant createdAt,
            Instant sentAt) {

        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getUserId(), n.getType(), n.getChannel(),
                    n.getRecipient(), n.getSubject(), n.getBody(), n.getReferenceId(),
                    n.getStatus(), n.isRead(), n.getCreatedAt(), n.getSentAt());
        }
    }
}
