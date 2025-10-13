package com.smartcane.point.entity.enums;

public enum CancelReason {
    USER_REQUEST,            // 사용자 단순 변심/오입력
    DUPLICATE,               // 중복 결제
    FRAUD_SUSPECT,           // 이상징후
    SYSTEM_ERROR,            // 내부 오류
    PG_REJECT,               // PG 거절/실패 동기화
    PARTIAL_REFUND,          // 부분 환불 사유 (일부 미이용 등)
    EXPIRED_RESERVATION,     // 예약 만료(미확정)
    ADMIN_FORCE              // 운영자 강제 취소
}
