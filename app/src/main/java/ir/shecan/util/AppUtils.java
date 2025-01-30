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
}
