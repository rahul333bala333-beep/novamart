package com.novamart.notification.service;

import com.novamart.common.api.PageResponse;
import com.novamart.common.security.AuthenticatedUser;
import com.novamart.common.security.CurrentUser;
import com.novamart.notification.domain.Notification;
import com.novamart.notification.dto.NotificationDtos.CreateNotificationRequest;
import com.novamart.notification.dto.NotificationDtos.NotificationResponse;
import com.novamart.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final MockNotificationTransport transport;

    public NotificationService(NotificationRepository notifications,
                               MockNotificationTransport transport) {
        this.notifications = notifications;
        this.transport = transport;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = Notification.queue(
                request.userId(), request.type(), request.channel(), request.recipient(),
                request.subject(), request.body(), request.referenceId());

        // Persist first, then attempt delivery. If delivery throws, the record
        // still exists in FAILED state and can be retried; delivering first would
        // risk sending a message with no trace that it happened.
        notifications.save(notification);
        try {
            if (transport.deliver(notification)) {
                notification.markSent();
            } else {
                notification.markFailed("Transport declined the message");
            }
        } catch (RuntimeException ex) {
            log.warn("Delivery of notification {} failed", notification.getId(), ex);
            notification.markFailed("Transport error");
        }
        return NotificationResponse.from(notification);
    }

    /**
     * Lists notifications, scoped by who is asking.
     *
     * <p>The scoping decision is made here from the verified principal rather
     * than from a request parameter, so a shopper cannot ask for someone else's
     * messages by supplying a different user id.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Notification.Type type, Pageable pageable) {
        AuthenticatedUser caller = CurrentUser.require();
        UUID scope = (caller.isAdmin() || caller.isService()) ? null : caller.id();
        return PageResponse.from(notifications.findFiltered(scope, type, pageable),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id, UUID userId) {
        Notification notification = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new com.novamart.common.error.ApiException(com.novamart.common.error.ErrorCode.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return NotificationResponse.from(notifications.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        java.util.List<Notification> unread = notifications.findByUserIdAndReadFalse(userId);
        for (Notification n : unread) {
            n.markRead();
        }
        notifications.saveAll(unread);
    }
}
