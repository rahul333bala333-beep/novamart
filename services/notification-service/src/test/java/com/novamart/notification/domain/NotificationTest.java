package com.novamart.notification.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    private static Notification queued() {
        return Notification.queue(UUID.randomUUID(), Notification.Type.ORDER_CONFIRMATION,
                Notification.Channel.EMAIL, "demo@novamart.dev",
                "Your order is confirmed", "Thanks for your order.", "order-123");
    }

    @Test
    void aNewNotificationStartsQueued() {
        Notification notification = queued();

        // Queued, not sent: the record exists before delivery is attempted, so a
        // transport failure still leaves a trace.
        assertThat(notification.getStatus()).isEqualTo(Notification.Status.QUEUED);
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void markingSentStampsTheTime() {
        Notification notification = queued();
        notification.markSent();

        assertThat(notification.getStatus()).isEqualTo(Notification.Status.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    void markingFailedKeepsTheReasonAndLeavesSentAtEmpty() {
        Notification notification = queued();
        notification.markFailed("Transport error");

        assertThat(notification.getStatus()).isEqualTo(Notification.Status.FAILED);
        assertThat(notification.getFailureReason()).isEqualTo("Transport error");
        // A failed message must never carry a sent timestamp.
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void theChannelDefaultsToEmailWhenUnspecified() {
        Notification notification = Notification.queue(UUID.randomUUID(),
                Notification.Type.WELCOME, null, "demo@novamart.dev", "Welcome", "Hello", null);

        assertThat(notification.getChannel()).isEqualTo(Notification.Channel.EMAIL);
    }

    @Test
    void theReferenceLinksBackToTheOrder() {
        assertThat(queued().getReferenceId()).isEqualTo("order-123");
    }
}
