package ir.shecan.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.data.modelDto.ServiceItem;

/** Shared rules for service selection, connection and renewal. */
public final class ServiceSelectionPolicy {
    private ServiceSelectionPolicy() {}

    public static boolean isFreeService(ServiceItem item) {
        return item == null || "0".equals(item.getOrderCode());
    }

    public static boolean isActivePaidService(ServiceItem item) {
        if (item == null || item.isClosed() || isFreeService(item)) return false;
        RequestStatus status = RequestStatus.fromValue(item.statusId);
        return status == RequestStatus.ACTIVE || status == RequestStatus.IN_USE
                || status == RequestStatus.EXPIRING;
    }

    public static ServiceItem findPreferredActivePaidService(List<ServiceItem> items) {
        if (items != null) {
            for (ServiceItem item : items) if (isActivePaidService(item)) return item;
        }
        return null;
    }

    public static boolean shouldOpenRenewal(ServiceItem item) {
        if (item == null || RequestStatus.isRenewalBlocked(item.statusId)) return false;
        RequestStatus status = RequestStatus.fromValue(item.statusId);
        return status == RequestStatus.SUPPORT_FINISHED
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_RENEW
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_ACTIVATION
                || status == RequestStatus.SUSPENDED
                || isDueDateExpired(item.dueDate);
    }

    public static boolean isDueDateExpired(String dueDate) {
        try {
            if (dueDate == null || dueDate.trim().isEmpty()) return false;
            String normalized = dueDate.trim();
            if (normalized.contains("T")) normalized = normalized.substring(0, normalized.indexOf("T"));
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(normalized);
            return date != null && date.getTime() < System.currentTimeMillis();
        } catch (Exception ignored) {
            return false;
        }
    }
}
