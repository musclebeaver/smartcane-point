package com.smartcane.point.controller;

import com.smartcane.point.dto.ChargeRequest;
import com.smartcane.point.dto.WalletResponse;
import com.smartcane.point.service.WalletService;
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
@Tag(name = "Wallet", description = "포인트 지갑 API")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "지갑 잔액 조회")
    @GetMapping("/{userId}/wallet")
    public WalletResponse get(@PathVariable Long userId) {
        return walletService.get(userId);
    }

    @Operation(
            summary = "포인트 충전",
            description = "멱등 지원: X-Idempotency-Key 또는 body.requestId 사용"
            // , security = { @SecurityRequirement(name = "bearerAuth") } // JWT 쓸 때 주석 해제
    )
    @PostMapping("/{userId}/wallet/charge")
    public WalletResponse charge(@PathVariable Long userId,
                                 @RequestBody @Valid ChargeRequest req,
                                 @RequestHeader(value = "X-Idempotency-Key", required = false) String idem) {

        String requestId = (req.requestId() != null && !req.requestId().isBlank())
                ? req.requestId()
                : (idem != null && !idem.isBlank() ? idem : UUID.randomUUID().toString());

        log.info("[API] wallet.charge userId={}, amount={}, orderId={}, requestId={}",
                userId, req.amount(), req.orderId(), requestId);

        return walletService.charge(userId, req.amount(), requestId, req.orderId());
    }
}
