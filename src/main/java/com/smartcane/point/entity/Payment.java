package com.smartcane.point.entity;

import com.smartcane.point.entity.enums.PaymentMethod;
import com.smartcane.point.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="payment", indexes = @Index(name="uk_order", columnList="orderId", unique=true))
@Getter @Setter
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false, length=100)
    private String orderId;            // 주문(멱등키)

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private PaymentMethod method;      // POINT/CARD/MIXED

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private PaymentStatus status;      // PENDING→CAPTURED or CANCELED/FAILED

    @Column(nullable=false)
    private long totalAmount;          // 주문 총액
    @Column(nullable=false)
    private long pointAmount;          // 포인트 사용액
    @Column(nullable=false)
    private long cashAmount;           // 현금(PG) 사용액 (0 허용)

    @Column(length=100)
    private String lastRequestId;      // 마지막 처리의 requestId 기록(추적)

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();
    @Column(nullable=false)
    private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}

