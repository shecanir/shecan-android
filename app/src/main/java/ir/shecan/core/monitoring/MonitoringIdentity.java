package ir.shecan.core.monitoring;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.util.UUID;

import ir.shecan.core.util.AppUtils;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;

class MonitoringIdentity {
    private final Context context;
    private final String sessionId = UUID.randomUUID().toString();

    MonitoringIdentity(Context context) {
        this.context = context.getApplicationContext();
    }

    String sessionId() {
        return sessionId;
    }

    String appVersion() {
        return AppUtils.getVersionName(context);
    }

    @SuppressLint("HardwareIds")
    String hashedDeviceId() {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return sha256(androidId != null ? androidId : "");
    }

    String hashedUserId() {
        VerifyApiViewModel token = new AppStorage(context).getToken(VerifyApiViewModel.class);
        if (token == null) return "";
        String userId = token.getId() > 0 ? String.valueOf(token.getId()) : token.getLogin();
        return sha256(userId != null ? userId : "");
    }

    boolean isInSample(int samplingPercent) {
        if (samplingPercent >= 100) return true;
        if (samplingPercent <= 0) return false;

        String hash = hashedDeviceId();
        if (hash.length() < 8) return false;
        int bucket = new BigInteger(hash.substring(0, 8), 16)
                .mod(BigInteger.valueOf(100))
                .intValue();
        return bucket < samplingPercent;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
