package ir.shecan.core.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

public final class ToastManager {

    private static Toast currentToast;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ToastManager() {
        // جلوگیری از ساخت instance
    }

    public static void show(Context context, String message) {
        if (context == null) return;
        if (TextUtils.isEmpty(message)) return;

        Context appContext = context.getApplicationContext();

        mainHandler.post(() -> {
            try {
                if (currentToast != null) {
                    currentToast.cancel();
                }

                currentToast = Toast.makeText(appContext, message, Toast.LENGTH_LONG);
                currentToast.show();

            } catch (Exception ignored) {
                // هرگز اجازه نده کرش کنه
            }
        });
    }

    public static void showShort(Context context, String message) {
        if (context == null) return;
        if (TextUtils.isEmpty(message)) return;

        Context appContext = context.getApplicationContext();

        mainHandler.post(() -> {
            try {
                if (currentToast != null) {
                    currentToast.cancel();
                }

                currentToast = Toast.makeText(appContext, message, Toast.LENGTH_SHORT);
                currentToast.show();

            } catch (Exception ignored) {
            }
        });
    }
}
