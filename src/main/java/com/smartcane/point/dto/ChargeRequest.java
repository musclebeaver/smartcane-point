package com.smartcane.point.dto;

public record ChargeRequest(long amount, String requestId, String orderId) {}
