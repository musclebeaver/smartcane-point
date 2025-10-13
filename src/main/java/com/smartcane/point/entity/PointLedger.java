package com.smartcane.point.entity;

import com.smartcane.point.entity.enums.LedgerStatus;
import com.smartcane.point.entity.enums.LedgerType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="point_ledger",
        indexes = {@Index(name="idx_user_created", columnList="userId,createdAt")})
@Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class PointLedger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private LedgerType type;        // CHARGE/DEBIT/REFUND/CANCEL

    @Column(nullable=false)
    private long amount;            // 양수만 저장, 부호는 type으로 해석

    @Column(length=100)
    private String orderId;         // 비즈니스 멱등(주문ID)

    @Column(length=100)
    private String requestId;       // API 멱등키

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private LedgerStatus status;    // SUCCESS/FAILED/PENDING

    @Column(length=255)
    private String memo;            // 사유/메모

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();
}
