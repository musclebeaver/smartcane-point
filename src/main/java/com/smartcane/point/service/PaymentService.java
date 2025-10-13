package com.smartcane.point.service;

import com.smartcane.point.dto.CancelPaymentRequest;
import com.smartcane.point.dto.CreatePaymentRequest;
import com.smartcane.point.dto.PaymentResponse;
import com.smartcane.point.dto.WalletResponse;
import com.smartcane.point.entity.Payment;
import com.smartcane.point.entity.PaymentCancel;
import com.smartcane.point.entity.enums.PaymentMethod;
import com.smartcane.point.entity.enums.PaymentStatus;
import com.smartcane.point.exception.BusinessException;
import com.smartcane.point.repository.PaymentCancelRepository;
import com.smartcane.point.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final PaymentCancelRepository cancelRepo;
    private final WalletService walletService;

    /**
     * 전액/일부 포인트 결제 (PG 연동 없는 순수 포인트 결제 시나리오)
     * - 멱등키: orderId(비즈니스), requestId(API)
     */
    @Transactional
    public PaymentResponse payWithPoints(Long userId, CreatePaymentRequest req) {
        String requestId = (req.requestId() == null || req.requestId().isBlank())
                ? UUID.randomUUID().toString()
                : req.requestId();

        if (req.totalAmount() <= 0) throw new BusinessException("INVALID_TOTAL_AMOUNT");
        if (req.pointAmount() <= 0) throw new BusinessException("INVALID_POINT_AMOUNT");
        if (req.pointAmount() > req.totalAmount()) throw new BusinessException("POINT_EXCEED_TOTAL");
        if (req.cashAmount() != 0) throw new BusinessException("CASH_AMOUNT_NOT_SUPPORTED"); // 혼합결제는 추후

        // 주문 기준 멱등: 이미 CAPTURED면 현재 상태 리턴
        Payment payment = paymentRepo.findByOrderId(req.orderId()).orElseGet(() -> {
            Payment p = new Payment();
            p.setUserId(userId);
            p.setOrderId(req.orderId());
            p.setMethod(PaymentMethod.POINT);
            p.setStatus(PaymentStatus.PENDING);
            p.setTotalAmount(req.totalAmount());
            p.setPointAmount(req.pointAmount());
            p.setCashAmount(req.cashAmount()); // 0
            return p;
        });

        if (PaymentStatus.CAPTURED.equals(payment.getStatus())) {
            WalletResponse w = walletService.get(userId);
            log.info("[PAY] idempotent CAPTURED orderId={}, userId={}", req.orderId(), userId);
            return new PaymentResponse(payment.getOrderId(), payment.getStatus().name(),
                    payment.getTotalAmount(), payment.getPointAmount(), payment.getCashAmount(), w.balance());
        }
        if (PaymentStatus.CANCELED.equals(payment.getStatus())) {
            WalletResponse w = walletService.get(userId);
            log.info("[PAY] already CANCELED orderId={}, userId={}", req.orderId(), userId);
            return new PaymentResponse(payment.getOrderId(), payment.getStatus().name(),
                    payment.getTotalAmount(), payment.getPointAmount(), payment.getCashAmount(), w.balance());
        }

        // 포인트 차감(멱등: WalletService가 보장)
        long newBalance = walletService.debit(userId, req.pointAmount(), requestId, req.orderId());

        // CAPTURE 확정
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setLastRequestId(requestId);
        paymentRepo.save(payment);

        log.info("[PAY] CAPTURED orderId={}, userId={}, pointAmount={}, newBalance={}",
                req.orderId(), userId, req.pointAmount(), newBalance);

        return new PaymentResponse(payment.getOrderId(), payment.getStatus().name(),
                payment.getTotalAmount(), payment.getPointAmount(), payment.getCashAmount(), newBalance);
    }

    /**
     * 결제 취소/부분 환불
     * - 환불액은 기존 결제에서 사용된 pointAmount 이내
     * - 멱등: 동일 orderId 전액취소가 이미 처리되었으면 상태 그대로 반환
     */
    @Transactional
    public PaymentResponse cancel(Long userId, CancelPaymentRequest req) {
        String requestId = (req.requestId() == null || req.requestId().isBlank())
                ? UUID.randomUUID().toString()
                : req.requestId();

        Payment payment = paymentRepo.findByOrderId(req.orderId())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND"));

        // 사용자/주문 매칭 체크(권장)
        if (!Objects.equals(payment.getUserId(), userId)) {
            throw new BusinessException("PAYMENT_USER_MISMATCH");
        }

        // 이미 취소된 주문(전액): 멱등 응답
        if (PaymentStatus.CANCELED.equals(payment.getStatus())) {
            WalletResponse w = walletService.get(userId);
            log.info("[CANCEL] idempotent already canceled orderId={}, userId={}", req.orderId(), userId);
            return new PaymentResponse(payment.getOrderId(), payment.getStatus().name(),
                    payment.getTotalAmount(), payment.getPointAmount(), payment.getCashAmount(), w.balance());
        }

        long cancelAmount = req.cancelAmount();
        if (cancelAmount <= 0) throw new BusinessException("INVALID_CANCEL_AMOUNT");
        if (cancelAmount > payment.getPointAmount()) throw new BusinessException("EXCEED_POINT_PAID");

        // 환불(멱등: WalletService가 보장)
        String memo = "cancel:" + (req.reasonCode() != null ? req.reasonCode() : "UNKNOWN");
        walletService.refund(userId, cancelAmount, requestId, req.orderId(), memo);

        // 취소 레코드
        cancelRepo.save(PaymentCancel.builder()
                .paymentId(payment.getId())
                .cancelAmount(cancelAmount)
                .reasonCode(req.reasonCode() != null ? req.reasonCode() : "UNKNOWN")
                .reasonMessage(req.reasonMessage())
                .build());

        // 결제 상태 갱신(부분/전액)
        long remain = payment.getPointAmount() - cancelAmount;
        payment.setPointAmount(remain);
        payment.setStatus(remain == 0 ? PaymentStatus.CANCELED : PaymentStatus.CAPTURED);
        payment.setLastRequestId(requestId);
        paymentRepo.save(payment);

        WalletResponse w = walletService.get(userId);

        log.info("[CANCEL] orderId={}, userId={}, cancelAmount={}, remainPoint={}, walletBalance={}",
                req.orderId(), userId, cancelAmount, remain, w.balance());

        return new PaymentResponse(payment.getOrderId(), payment.getStatus().name(),
                payment.getTotalAmount(), payment.getPointAmount(), payment.getCashAmount(), w.balance());
    }
}
