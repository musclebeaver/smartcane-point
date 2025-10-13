package com.smartcane.point.controller;

import com.smartcane.point.dto.CancelPaymentRequest;
import com.smartcane.point.dto.CreatePaymentRequest;
import com.smartcane.point.dto.PaymentResponse;
import com.smartcane.point.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "포인트 결제/취소 API")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "포인트 결제",
            description = "전액 포인트 결제 스켈레톤 (혼합결제는 추후 확장)"
            // , security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping("/{userId}/payments/pay")
    public PaymentResponse pay(@PathVariable Long userId,
                               @RequestBody @Valid CreatePaymentRequest req,
                               @RequestHeader(value = "X-Idempotency-Key", required = false) String idem) {

        String requestId = (req.requestId() != null && !req.requestId().isBlank())
                ? req.requestId()
                : (idem != null && !idem.isBlank() ? idem : UUID.randomUUID().toString());

        CreatePaymentRequest fixed = new CreatePaymentRequest(
                req.totalAmount(),
                req.pointAmount(),
                req.cashAmount(),
                req.orderId(),
                requestId
        );

        log.info("[API] payments.pay userId={}, orderId={}, total={}, point={}, cash={}, requestId={}",
                userId, fixed.orderId(), fixed.totalAmount(), fixed.pointAmount(), fixed.cashAmount(), requestId);

        return paymentService.payWithPoints(userId, fixed);
    }

    @Operation(
            summary = "결제 취소/부분 환불",
            description = "사유 코드/메시지 포함"
            // , security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping("/{userId}/payments/cancel")
    public PaymentResponse cancel(@PathVariable Long userId,
                                  @RequestBody @Valid CancelPaymentRequest req,
                                  @RequestHeader(value = "X-Idempotency-Key", required = false) String idem) {

        String requestId = (req.requestId() != null && !req.requestId().isBlank())
                ? req.requestId()
                : (idem != null && !idem.isBlank() ? idem : UUID.randomUUID().toString());

        CancelPaymentRequest fixed = new CancelPaymentRequest(
                req.orderId(),
                req.cancelAmount(),
                requestId,
                req.reasonCode(),
                req.reasonMessage()
        );

        log.info("[API] payments.cancel userId={}, orderId={}, cancelAmount={}, reasonCode={}, requestId={}",
                userId, fixed.orderId(), fixed.cancelAmount(), fixed.reasonCode(), requestId);

        return paymentService.cancel(userId, fixed);
    }
}
