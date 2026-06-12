package ir.shecan.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.core.constant.Constant;
import ir.shecan.data.modelDto.AppConfig;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.activity.BillingPlansActivity;

public class AppUtils {
//    public static long getVersionCode(Context context) {
//        try {
//            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28 and above
//                return PackageInfoCompat.getLongVersionCode(packageInfo);
//            } else { // API 27 and below
//                return packageInfo.versionCode;
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//            return -1; // Return -1 if an error occurs
//        }
//    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "0.0.0";
        }
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
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

    public static void openUrl(String url, Activity activity) {
        if (openInternalUrlIfSupported(url, activity)) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
    }

    private static boolean openInternalUrlIfSupported(String url, Activity activity) {
        if (activity == null || url == null || url.trim().isEmpty()) return false;

        Uri uri;
        try {
            uri = Uri.parse(url.trim());
        } catch (Exception ignored) {
            return false;
        }

        if (!isPurchaseDeepLink(uri)) return false;

        activity.startActivity(new Intent(activity, BillingPlansActivity.class));
        return true;
    }

    private static boolean isPurchaseDeepLink(Uri uri) {
        if (uri == null) return false;

        String scheme = lower(uri.getScheme());
        String host = lower(uri.getHost());
        String firstSegment = uri.getPathSegments().isEmpty() ? "" : lower(uri.getPathSegments().get(0));
        String secondSegment = uri.getPathSegments().size() < 2 ? "" : lower(uri.getPathSegments().get(1));

        if ("shecan".equals(scheme)) {
            return isPurchaseTarget(host) || isPurchaseTarget(firstSegment) || isPurchaseTarget(secondSegment);
        }

        if (("http".equals(scheme) || "https".equals(scheme))
                && ("my.shecan.ir".equals(host) || "shecan.ir".equals(host))) {
            return "app".equals(firstSegment) && isPurchaseTarget(secondSegment);
        }

        return false;
    }

    private static boolean isPurchaseTarget(String value) {
        return "purchase".equals(value)
                || "billing".equals(value)
                || "plans".equals(value)
                || "subscription".equals(value)
                || "buy-service".equals(value)
                || "billing-plans".equals(value);
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public static void adjustUIForFragment(Activity activity, int statusBarColor, int bottomNavigationColor) {

        boolean isLight = isLightTheme(activity);
        applyStatusBarMode(activity, isLight, statusBarColor);
        applyNavigationBarMode(activity, isLight, bottomNavigationColor);
    }

    public static boolean isLightTheme(Context context) {
        int mode = getSavedNightMode(context);
        if (mode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            mode = AppCompatDelegate.getDefaultNightMode();
        }

        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return true;
        }
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return false;
        }

        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getSavedNightMode(Context context) {
        AppConfig appConfig = new AppStorage(context.getApplicationContext()).getAppConfig(AppConfig.class);
        if (appConfig != null) {
            return appConfig.getMode();
        }
        return AppCompatDelegate.MODE_NIGHT_NO;
    }

    public static void applySavedNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedNightMode(context));
    }

    public static void applyStatusBarMode(Activity activity, boolean isLightMode, int statusBarColor) {
        Window window = activity.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(activity, statusBarColor));

        int flags = window.getDecorView().getSystemUiVisibility();
        if (isLightMode) {
            // حالت آیکون‌های تیره (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        } else {
            // حذف حالت آیکون‌های تیره
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    public static void applyNavigationBarMode(Activity activity, boolean isLightMode, int navigationBarColor) {
        Window window = activity.getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(activity, navigationBarColor));

        if (isLightMode) {
            // آیکون‌های تیره (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.getDecorView().setSystemUiVisibility(
                        window.getDecorView().getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                );
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int flags = window.getDecorView().getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                window.getDecorView().setSystemUiVisibility(flags);
            }
        }
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static String buildRedirect(String rawUrl, Context context) {
        AppStorage storage = new AppStorage(context);
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token == null) return null;

        return String.format(Constant.BaseAuthRedirect, token.getApiKey(), rawUrl);
    }

}
