package ir.shecan.core.sentry;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import io.sentry.Sentry;
import io.sentry.SentryOptions;

/** Applies the remotely controlled Sentry trace sample rate while keeping the manifest fallback. */
public final class SentrySamplingConfig {
    static final String PREFERENCES_NAME = "app_preferences";
    static final String SAMPLE_RATE_PERCENT_KEY = "SENTRY_SAMPLE_RATE_PERCENT";

    private static volatile Double manifestFallbackRate;

    private SentrySamplingConfig() {
    }

    /**
     * Must be called from the Sentry init callback, after Android manifest options are loaded.
     * If no remote value has been cached, this deliberately leaves the manifest value untouched.
     */
    public static void applyCached(Context context, SentryOptions options) {
        manifestFallbackRate = options.getTracesSampleRate();

        Integer cachedPercent = getCachedPercent(context);
        if (cachedPercent != null) {
            options.setTracesSampleRate(toSdkRate(cachedPercent));
        }
    }

    /**
     * Persists and immediately applies a Home Page value. A missing value clears the cached
     * override and restores the value originally read from AndroidManifest.xml.
     */
    public static void updateFromRemote(Context context, Integer percent) {
        SharedPreferences preferences = preferences(context);
        Double sdkRate;

        if (percent == null) {
            preferences.edit().remove(SAMPLE_RATE_PERCENT_KEY).apply();
            sdkRate = manifestFallbackRate;
        } else {
            int clampedPercent = clampPercent(percent);
            preferences.edit().putInt(SAMPLE_RATE_PERCENT_KEY, clampedPercent).apply();
            sdkRate = toSdkRate(clampedPercent);
        }

        if (Sentry.isEnabled()) {
            Sentry.getCurrentScopes().getOptions().setTracesSampleRate(sdkRate);
        }
    }

    public static Integer readAndroidSampleRate(JSONObject homePage) {
        if (homePage == null) return null;

        JSONObject sentry = homePage.optJSONObject("sentry");
        JSONObject android = sentry != null ? sentry.optJSONObject("android") : null;
        if (android != null && android.has("sample_rate")) {
            return readPercent(android, "sample_rate");
        }

        return readPercent(homePage, "sentry_sample_rate");
    }

    static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    static double toSdkRate(int percent) {
        return clampPercent(percent) / 100.0;
    }

    private static Integer readPercent(JSONObject source, String key) {
        if (source == null || !source.has(key) || source.isNull(key)) return null;

        Object value = source.opt(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer getCachedPercent(Context context) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.contains(SAMPLE_RATE_PERCENT_KEY)) return null;

        try {
            return clampPercent(preferences.getInt(SAMPLE_RATE_PERCENT_KEY, 0));
        } catch (ClassCastException ignored) {
            preferences.edit().remove(SAMPLE_RATE_PERCENT_KEY).apply();
            return null;
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
