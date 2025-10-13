package com.smartcane.point.dto;

public record PaymentResponse(
        String orderId, String status, long totalAmount, long pointAmount, long cashAmount, long walletBalance
) {}
