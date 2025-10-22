CREATE TABLE IF NOT EXISTS idempotency (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_key   VARCHAR(120) NOT NULL,
  endpoint      VARCHAR(60)  NOT NULL,
  user_id       BIGINT       NOT NULL,
  http_status   INT          NOT NULL,
  response_body TEXT         NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_idem UNIQUE (request_key, endpoint, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
