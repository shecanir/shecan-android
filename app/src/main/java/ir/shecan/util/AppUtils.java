package ir.shecan.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.pm.PackageInfoCompat;

public class AppUtils {
    public static long getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28 and above
                return PackageInfoCompat.getLongVersionCode(packageInfo);
            } else { // API 27 and below
                return packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1; // Return -1 if an error occurs
        }
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "0.0.0";
        }
    }

    public static int compareVersionNames(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < length; i++) {
            int num1 = parseVersionPart(v1Parts, i);
            int num2 = parseVersionPart(v2Parts, i);

            if (num1 > num2) return 1; // version1 is greater
            if (num1 < num2) return -1; // version2 is greater
        }

        return 0; // Versions are equal
    }

    // Helper function to safely parse numeric parts
    private static int parseVersionPart(String[] parts, int index) {
        if (index < parts.length) {
            String part = parts[index].replaceAll("[^0-9]", ""); // Remove non-numeric characters
            return part.isEmpty() ? 0 : Integer.parseInt(part);
        }
        return 0;
    }


}
