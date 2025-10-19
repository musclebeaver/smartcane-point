CREATE TABLE IF NOT EXISTS idempotency (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  requestKey   VARCHAR(120)  NOT NULL,
  endpoint     VARCHAR(60)   NOT NULL,
  userId       BIGINT        NOT NULL,
  httpStatus   INT           NOT NULL,
  responseBody TEXT          NULL,
  createdAt    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

  CONSTRAINT uk_idem UNIQUE (requestKey, endpoint, userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
