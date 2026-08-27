-- Adds is_read column to notifications table in notification_db

ALTER TABLE notifications ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);
