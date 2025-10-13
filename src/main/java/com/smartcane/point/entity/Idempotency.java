package com.smartcane.point.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="idempotency",
        uniqueConstraints = @UniqueConstraint(name="uk_idem", columnNames={"requestKey","endpoint","userId"}))
@Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class Idempotency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String requestKey;      // 헤더 X-Idempotency-Key or requestId

    @Column(nullable=false, length=60)
    private String endpoint;        // /wallet/charge, /payments/pay ...

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false)
    private int httpStatus;

    @Column(columnDefinition="TEXT")
    private String responseBody;

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();
}
