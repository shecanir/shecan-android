package ir.shecan.core.billing;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

/** Parses the callback formats used by the site payment flow. */
public final class SitePaymentCallback {

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILED = "failed";

    private SitePaymentCallback() {
    }

    public static boolean isCallback(Uri uri) {
        if (uri == null) return false;

        if ("shecan".equalsIgnoreCase(uri.getScheme())
                && "payment-callback".equalsIgnoreCase(uri.getHost())) {
            return true;
        }

        if (!"my.shecan.ir".equalsIgnoreCase(uri.getHost())) return false;
        String path = uri.getPath();
        return path != null
                && (path.startsWith("/app/payment-callback")
                || path.startsWith("/panel/payment"));
    }

    public static boolean hasResolvableOutcome(Uri uri) {
        return resolveResult(uri) != null || resolvePaymentId(uri) > 0L;
    }

    public static String resolveResult(Uri uri) {
        if (!isCallback(uri)) return null;

        String result = normalizeResult(uri.getQueryParameter("status"));
        if (result != null) return result;
        result = normalizeResult(uri.getQueryParameter("status_id"));
        if (result != null) return result;
        result = normalizeResult(uri.getQueryParameter("result"));
        if (result != null) return result;
        result = normalizeResult(uri.getQueryParameter("success"));
        if (result != null) return result;
        result = normalizeResult(uri.getQueryParameter("payment_status"));
        if (result != null) return result;
        result = normalizeResult(uri.getQueryParameter("payment_status_id"));
        if (result != null) return result;
        return normalizeResult(uri.getPath());
    }

    public static String normalizeResult(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) return null;
        if (normalized.contains("fail")
                || normalized.contains("cancel")
                || normalized.contains("error")
                || "nok".equals(normalized)
                || "0".equals(normalized)
                || "false".equals(normalized)) {
            return RESULT_FAILED;
        }
        if (normalized.contains("success")
                || normalized.contains("paid")
                || "ok".equals(normalized)
                || "1".equals(normalized)
                || "20".equals(normalized)
                || "true".equals(normalized)) {
            return RESULT_SUCCESS;
        }
        return null;
    }

    public static long resolvePaymentId(Uri uri) {
        if (uri == null || !isCallback(uri)) return 0L;

        List<String> segments = uri.getPathSegments();
        if (segments != null
                && segments.size() >= 3
                && "panel".equals(segments.get(0))
                && "payment".equals(segments.get(1))) {
            long pathPaymentId = parsePositiveLong(segments.get(2));
            if (pathPaymentId > 0L) return pathPaymentId;
        }

        long queryPaymentId = parsePositiveLong(uri.getQueryParameter("payment_id"));
        if (queryPaymentId > 0L) return queryPaymentId;
        queryPaymentId = parsePositiveLong(uri.getQueryParameter("paymentId"));
        if (queryPaymentId > 0L) return queryPaymentId;
        queryPaymentId = parsePositiveLong(uri.getQueryParameter("payment"));
        if (queryPaymentId > 0L) return queryPaymentId;
        queryPaymentId = parsePositiveLong(uri.getQueryParameter("id"));
        if (queryPaymentId > 0L) return queryPaymentId;
        return parsePositiveLong(uri.getQueryParameter("issue_id"));
    }

    private static long parsePositiveLong(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(parsed, 0L);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
