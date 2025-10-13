package com.smartcane.point.dto;

// 결제 취소(부분/전액)
public record CancelPaymentRequest(
        String orderId, long cancelAmount, String requestId,
        String reasonCode, String reasonMessage
) {}