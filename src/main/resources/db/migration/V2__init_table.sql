-- V1__init.sql  (MySQL 8, utf8mb4, snake_case)

-- 선택) 비어있다면 스키마 생성
-- CREATE DATABASE IF NOT EXISTS smartcane_point
--   DEFAULT CHARACTER SET utf8mb4
--   DEFAULT COLLATE utf8mb4_unicode_ci;
-- USE smartcane_point;

-- 1) 멱등 처리 기록
CREATE TABLE IF NOT EXISTS idempotency (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  request_key    VARCHAR(120) NOT NULL,
  endpoint       VARCHAR(60)  NOT NULL,
  user_id        BIGINT       NOT NULL,
  http_status    INT          NOT NULL,
  response_body  TEXT         NULL,
  created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_idempotency PRIMARY KEY (id),
  CONSTRAINT uk_idem UNIQUE KEY (request_key, endpoint, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 결제 본표 (payment)
--   method: POINT/CARD/MIXED
--   status: PENDING/CAPTURED/CANCELED/FAILED
CREATE TABLE IF NOT EXISTS payment (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  order_id         VARCHAR(100) NOT NULL,
  method           VARCHAR(20)  NOT NULL,
  status           VARCHAR(20)  NOT NULL,
  total_amount     BIGINT       NOT NULL,
  point_amount     BIGINT       NOT NULL,
  cash_amount      BIGINT       NOT NULL,
  last_request_id  VARCHAR(100) NULL,
  created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_payment PRIMARY KEY (id),
  CONSTRAINT uk_payment_order UNIQUE KEY (order_id),
  CONSTRAINT chk_payment_method CHECK (method IN ('POINT','CARD','MIXED')),
  CONSTRAINT chk_payment_status CHECK (status IN ('PENDING','CAPTURED','CANCELED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) 결제 취소 (payment_cancel)
CREATE TABLE IF NOT EXISTS payment_cancel (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  payment_id      BIGINT       NOT NULL,
  cancel_amount   BIGINT       NOT NULL,
  reason_code     VARCHAR(30)  NOT NULL,
  reason_message  VARCHAR(255) NULL,
  created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_payment_cancel PRIMARY KEY (id),
  CONSTRAINT fk_paymentcancel_payment
    FOREIGN KEY (payment_id) REFERENCES payment(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) 포인트 원장 (point_ledger)
--   type: CHARGE/DEBIT/REFUND/CANCEL
--   status: SUCCESS/FAILED/PENDING
CREATE TABLE IF NOT EXISTS point_ledger (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  type        VARCHAR(20)  NOT NULL,
  amount      BIGINT       NOT NULL,
  order_id    VARCHAR(100) NULL,
  request_id  VARCHAR(100) NULL,
  status      VARCHAR(20)  NOT NULL,
  memo        VARCHAR(255) NULL,
  created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_point_ledger PRIMARY KEY (id),
  CONSTRAINT chk_ledger_type   CHECK (type   IN ('CHARGE','DEBIT','REFUND','CANCEL')),
  CONSTRAINT chk_ledger_status CHECK (status IN ('SUCCESS','FAILED','PENDING')),
  INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) 포인트 지갑 (point_wallet)
CREATE TABLE IF NOT EXISTS point_wallet (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  balance     BIGINT NOT NULL,
  version     BIGINT NOT NULL DEFAULT 0,  -- @Version
  created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT pk_point_wallet PRIMARY KEY (id),
  CONSTRAINT uk_point_wallet_user UNIQUE KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
