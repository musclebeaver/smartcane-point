package com.smartcane.point.service;

import com.smartcane.point.dto.ChargeRequest;
import com.smartcane.point.dto.WalletResponse;
import com.smartcane.point.entity.Idempotency;
import com.smartcane.point.entity.PointLedger;
import com.smartcane.point.entity.PointWallet;
import com.smartcane.point.entity.enums.LedgerStatus;
import com.smartcane.point.entity.enums.LedgerType;
import com.smartcane.point.exception.BusinessException;
import com.smartcane.point.repository.IdempotencyRepository;
import com.smartcane.point.repository.PointLedgerRepository;
import com.smartcane.point.repository.PointWalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final PointWalletRepository walletRepo;
    private final PointLedgerRepository ledgerRepo;
    private final IdempotencyRepository idemRepo;

    /**
     * 포인트 충전 (멱등)
     */
    @Transactional
    public WalletResponse charge(Long userId, long amount, String requestId, String orderId) {
        final String ep = "/wallet/charge";
        String reqKey = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;

        if (idemRepo.existsByRequestKeyAndEndpointAndUserId(reqKey, ep, userId)) {
            log.info("[IDEM] charge already processed userId={}, requestId={}", userId, reqKey);
            return get(userId);
        }

        // 경합 구간 보호: PESSIMISTIC_WRITE 또는 @Version
        PointWallet wallet = walletRepo.lockByUserId(userId).orElseGet(() -> {
            PointWallet w = new PointWallet();
            w.setUserId(userId);
            w.setBalance(0L);
            return walletRepo.save(w);
        });

        long before = wallet.getBalance();
        wallet.setBalance(before + amount);
        walletRepo.save(wallet);

        // 불변 원장 기록
        PointLedger ledger = PointLedger.builder()
                .userId(userId)
                .type(LedgerType.CHARGE)
                .amount(amount)               // 양수
                .orderId(orderId)
                .requestId(reqKey)
                .status(LedgerStatus.SUCCESS)
                .memo("charge")
                .build();
        ledgerRepo.save(ledger);

        // 멱등 기록
        idemRepo.save(Idempotency.builder()
                .requestKey(reqKey)
                .endpoint(ep)
                .userId(userId)
                .httpStatus(200)
                .responseBody("{}")
                .createdAt(Instant.now())
                .build());

        log.info("[WALLET] charge userId={} amount={} balance {} -> {} (orderId={}, requestId={})",
                userId, amount, before, wallet.getBalance(), orderId, reqKey);

        return new WalletResponse(userId, wallet.getBalance());
    }

    /**
     * 포인트 차감(결제에 사용) – 멱등
     * 성공 시 잔액 반환
     */
    @Transactional
    public long debit(Long userId, long amount, String requestId, String orderId) {
        final String ep = "/wallet/debit";
        String reqKey = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;

        if (idemRepo.existsByRequestKeyAndEndpointAndUserId(reqKey, ep, userId)) {
            log.info("[IDEM] debit already processed userId={}, requestId={}", userId, reqKey);
            return walletRepo.findByUserId(userId).map(PointWallet::getBalance).orElse(0L);
        }

        PointWallet wallet = walletRepo.lockByUserId(userId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND"));

        if (amount <= 0) throw new BusinessException("INVALID_DEBIT_AMOUNT");
        if (wallet.getBalance() < amount) throw new BusinessException("INSUFFICIENT_POINT");

        long before = wallet.getBalance();
        wallet.setBalance(before - amount);
        walletRepo.save(wallet);

        ledgerRepo.save(PointLedger.builder()
                .userId(userId)
                .type(LedgerType.DEBIT)
                .amount(amount)                 // 양수
                .orderId(orderId)
                .requestId(reqKey)
                .status(LedgerStatus.SUCCESS)
                .memo("debit")
                .build());

        idemRepo.save(Idempotency.builder()
                .requestKey(reqKey)
                .endpoint(ep)
                .userId(userId)
                .httpStatus(200)
                .responseBody("{}")
                .createdAt(Instant.now())
                .build());

        log.info("[WALLET] debit userId={} amount={} balance {} -> {} (orderId={}, requestId={})",
                userId, amount, before, wallet.getBalance(), orderId, reqKey);

        return wallet.getBalance();
    }

    /**
     * 포인트 환불(결제 취소/부분환불에 의해 증가) – 멱등
     */
    @Transactional
    public void refund(Long userId, long amount, String requestId, String orderId, String memo) {
        final String ep = "/wallet/refund";
        String reqKey = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;

        if (idemRepo.existsByRequestKeyAndEndpointAndUserId(reqKey, ep, userId)) {
            log.info("[IDEM] refund already processed userId={}, requestId={}", userId, reqKey);
            return;
        }

        if (amount <= 0) throw new BusinessException("INVALID_REFUND_AMOUNT");

        PointWallet wallet = walletRepo.lockByUserId(userId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND"));

        long before = wallet.getBalance();
        wallet.setBalance(before + amount);
        walletRepo.save(wallet);

        ledgerRepo.save(PointLedger.builder()
                .userId(userId)
                .type(LedgerType.REFUND)
                .amount(amount)                 // 양수
                .orderId(orderId)
                .requestId(reqKey)
                .status(LedgerStatus.SUCCESS)
                .memo(memo != null ? memo : "refund")
                .build());

        idemRepo.save(Idempotency.builder()
                .requestKey(reqKey)
                .endpoint(ep)
                .userId(userId)
                .httpStatus(200)
                .responseBody("{}")
                .createdAt(Instant.now())
                .build());

        log.info("[WALLET] refund userId={} amount={} balance {} -> {} (orderId={}, requestId={}, memo={})",
                userId, amount, before, wallet.getBalance(), orderId, reqKey, memo);
    }

    @Transactional
    public WalletResponse manualAdjust(Long userId, long delta, String requestId, String memo) {
        // 운영자 수기 조정(감사 추적 필요)
        if (delta == 0) return get(userId);
        if (delta > 0) return charge(userId, delta, requestId, memo);
        long newBal = debit(userId, Math.abs(delta), requestId, memo);
        return new WalletResponse(userId, newBal);
    }

    @Transactional
    public WalletResponse get(Long userId) {
        long bal = walletRepo.findByUserId(userId)
                .map(PointWallet::getBalance)
                .orElse(0L);
        return new WalletResponse(userId, bal);
    }
}
