package com.novamart.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user", columnList = "user_id"),
                @Index(name = "idx_notifications_created", columnList = "created_at")
        })
public class Notification {

    public enum Type {
        WELCOME,
        ORDER_CONFIRMATION,
        PAYMENT_CONFIRMATION,
        PAYMENT_FAILED,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED
    }

    public enum Channel {
        EMAIL, SMS, IN_APP
    }

    public enum Status {
        QUEUED,
        /**
         * The transport accepted the message. With the mock transport in use this
         * means it was written to the service log; it does <em>not</em> mean an
         * email left a real mail server.
         */
        SENT,
        FAILED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private Channel channel;

    @Column(name = "recipient", length = 180)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    /** The order or payment this message is about, when there is one. */
    @Column(name = "reference_id", length = 80)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public static Notification queue(UUID userId, Type type, Channel channel, String recipient,
                                     String subject, String body, String referenceId) {
        Notification n = new Notification();
        n.id = UUID.randomUUID();
        n.userId = userId;
        n.type = type;
        n.channel = channel == null ? Channel.EMAIL : channel;
        n.recipient = recipient;
        n.subject = subject;
        n.body = body;
        n.referenceId = referenceId;
        n.status = Status.QUEUED;
        n.read = false;
        n.createdAt = Instant.now();
        return n;
    }

    public void markRead() {
        this.read = true;
    }

    public void markSent() {
        this.status = Status.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.failureReason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRead() {
        return read;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
