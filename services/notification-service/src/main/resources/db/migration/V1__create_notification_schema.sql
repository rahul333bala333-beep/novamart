-- notification-service schema.

CREATE TABLE notifications (
    id             VARCHAR(36)          NOT NULL,
    user_id        VARCHAR(36)          NOT NULL,
    type           VARCHAR(40)   NOT NULL,
    channel        VARCHAR(20)   NOT NULL,
    recipient      VARCHAR(180),
    subject        VARCHAR(200)  NOT NULL,
    body           VARCHAR(4000) NOT NULL,
    reference_id   VARCHAR(80),
    -- SENT means the transport accepted the message. With the mock transport
    -- currently wired in, that means it was written to the service log.
    status         VARCHAR(20)   NOT NULL,
    failure_reason VARCHAR(300),
    created_at     TIMESTAMP     NOT NULL,
    sent_at        TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_created ON notifications (created_at);
CREATE INDEX idx_notifications_reference ON notifications (reference_id);
