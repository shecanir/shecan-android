package ir.shecan.core.constant;

public enum RequestStatus {

    READY_TO_CONNECT(0),

    /** جدید */
    NEW(1),

    /** در انتظار تأیید */
    WAITING_FOR_APPROVAL(12),

    /** تأیید شده */
    APPROVED(11),

    /** در حال انجام */
    IN_PROGRESS(2),

    /** فعال */
    ACTIVE(3),

    /** در حال استفاده */
    IN_USE(9),

    /** درحال انقضا */
    EXPIRING(10),

    /** در انتظار فعال‌سازی */
    WAITING_FOR_ACTIVATION(13),

    /** در انتظار پرداخت / تمدید */
    WAITING_FOR_PAYMENT_OR_RENEW(14),

    /** در انتظار پرداخت / فعال‌سازی */
    WAITING_FOR_PAYMENT_OR_ACTIVATION(15),

    /** اتمام پشتیبانی */
    SUPPORT_FINISHED(5),

    /** معلق */
    SUSPENDED(7),

    /** حذف‌شده */
    DELETED(8),

    /** رد شده */
    RETURNED(6),

    /** حل شده */
    RESOLVED(23),

    /** تعلیق به جهت تخلف */
    SUSPENDED_FOR_VIOLATION(27);

    private final int value;

    RequestStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RequestStatus fromValue(int value) {
        for (RequestStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;  // می‌توان Exception هم برگرداند
    }

    public static boolean isRenewalBlocked(int value) {
        return value == SUSPENDED_FOR_VIOLATION.value;
    }
}
