CREATE TABLE IF NOT EXISTS idempotency (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  key_hash        VARCHAR(64) NOT NULL UNIQUE,
  request_id      VARCHAR(100) NOT NULL,
  method          VARCHAR(16)  NOT NULL,
  path            VARCHAR(255) NOT NULL,
  status          ENUM('PENDING','COMPLETED','FAILED') NOT NULL,
  response_code   INT NULL,
  response_body   MEDIUMTEXT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency(key_hash);
