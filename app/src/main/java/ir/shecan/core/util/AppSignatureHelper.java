package ir.shecan.core.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class AppSignatureHelper {

    private static final String TAG = "AppSignature";

    private Context context;

    public AppSignatureHelper(Context context) {
        this.context = context;
    }

    public ArrayList<String> getAppSignatures() {
        ArrayList<String> appCodes = new ArrayList<>();

        try {
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (Signature signature : packageInfo.signingInfo.getApkContentsSigners()) {
                    String hash = hash(packageName, signature.toCharsString());
                    if (hash != null) {
                        appCodes.add(hash);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        return appCodes;
    }

    private static String hash(String packageName, String signature) {
        String appInfo = packageName + " " + signature;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(appInfo.getBytes());
            byte[] hashSignature = messageDigest.digest();

            // فقط ۹ کاراکتر لازم است
            String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
            return base64Hash.substring(0, 11); // معمولاً 11 کاراکتر است
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithm: " + e.getMessage());
        }
        return null;
    }
}
