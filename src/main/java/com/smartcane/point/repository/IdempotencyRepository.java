package com.smartcane.point.repository;

import com.smartcane.point.entity.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<Idempotency, Long> {
    boolean existsByRequestKeyAndEndpointAndUserId(String k, String ep, Long userId);
}
