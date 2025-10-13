package com.smartcane.point.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name="payment_cancel")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCancel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long paymentId;

    @Column(nullable=false)
    private long cancelAmount;        // 부분/전액 환불액

    @Column(nullable=false, length=30)
    private String reasonCode;        // CancelReason.name()

    @Column(length=255)
    private String reasonMessage;     // 상세 메시지(사용자/운영자 입력)

    @Column(nullable=false, updatable=false)
    private Instant createdAt = Instant.now();
}