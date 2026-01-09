-- Notifications Table - Evolution 4

-- !Ups

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  flagged BOOLEAN NOT NULL DEFAULT FALSE,
  pending BOOLEAN NOT NULL DEFAULT TRUE,
  notification_type VARCHAR(100) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT NOT NULL,
  url TEXT,
  metadata JSONB
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(notification_type);
CREATE INDEX idx_notifications_pending ON notifications(pending);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_pending ON notifications(user_id, pending);

-- !Downs

DROP TABLE IF EXISTS notifications;
