package com.smartcane.point.dto;

public record CreatePaymentRequest(
        long totalAmount, long pointAmount, long cashAmount,
        String orderId, String requestId
) {}