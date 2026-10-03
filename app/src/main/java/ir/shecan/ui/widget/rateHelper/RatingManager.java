package ir.shecan.ui.widget.rateHelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import ir.shecan.core.constant.Constant;
import ir.shecan.core.util.ToastManager;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.storage.AppStorage;

public class RatingManager {

    private static final long THREE_HOURS = 3 * 60 * 60 * 1000L;
    private static final long TWO_DAYS = 2 * 24 * 60 * 60 * 1000L;
    private static final long ONE_MONTH = 30L * 24 * 60 * 60 * 1000L;

    private final Context context;
    private final AppStorage storage;

    public RatingManager(Context context) {
        this.context = context;
        this.storage = new AppStorage(context);
    }

    public void initFirstOpenIfNeeded() {
        RatingState state = getState();
        if (state.firstOpenTime == 0) {
            state.firstOpenTime = System.currentTimeMillis();
            saveState(state);
        }
    }

    public boolean shouldShowRatingDialog() {

        RatingState state = getState();
        long now = System.currentTimeMillis();

        // ⛔ اگر ۲ روز از آخرین dismiss گذشته
        if (state.lastRatingValue == 0 && state.lastPromptDismissTime > 0 && now - state.lastPromptDismissTime >= TWO_DAYS)
            return true;

//        // ✅ شرط اول: ۳ ساعت گذشته و هنوز امتیاز نداده
//        if (state.lastRatingValue == 0 && now - state.firstOpenTime >= THREE_HOURS) {
//            return true;
//        }

        // ✅ شرط دوم: امتیاز کمتر از ۴ داده و ۱ ماه گذشته
        if (state.lastRatingValue >= 1 && state.lastRatingValue <= 3 && now - state.lastRatingTime >= ONE_MONTH) {
            return true;
        }

        return false;
    }

    public void onDialogDismissed() {
        RatingState state = getState();
        state.lastPromptDismissTime = System.currentTimeMillis();
        saveState(state);
    }

    public void onUserRated(
            String url,
            int ratingValue, boolean isProUser,
            String mobile,
            long weeklyConnectionTime,
            String appVersion,
            String storeName) {

        RatingState state = getState();
        state.lastRatingValue = ratingValue;
        state.lastRatingTime = System.currentTimeMillis();
        saveState(state);

        sendRatingToServer(url, ratingValue, isProUser, mobile,
                weeklyConnectionTime, appVersion, storeName);

        if (ratingValue >= 4) {
            openMarketForRating();
        } else {
            handleLowRating(isProUser);
        }
    }

    private void handleLowRating(boolean isProUser) {

        String message;

        if (!isProUser) {
            message = "سرویس رایگان از کیفیت پایین‌تری برخوردار است.\n\n" +
                    "برای تجربه بهتر، نسبت به ثبت تیکت در پنل کاربری خود در سایت شکن (Shecan.ir) اقدام کنید.";
        } else {
            message = "از طریق ثبت تیکت در پنل کاربری خود در سایت شکن (Shecan.ir)، " +
                    "مشکل را مطرح کنید تا بتوانیم کمکتان کنیم.";
        }

        showSupportDialog(message);
    }

    private void openMarketForRating() {

        String packageName = context.getPackageName();

        if (Constant.IsCafeBazaarMode) {
            try {
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setData(Uri.parse("bazaar://details?id=" + packageName));
                intent.setPackage("com.farsitel.bazaar");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        if (Constant.IsMyketMode) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("myket://comment?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        RatingState state = getState();
        state.hasRatedInStore = true;
        saveState(state);
    }

    private void sendRatingToServer(
            String url,
            int rating,
            boolean isProUser,
            String mobile,
            long weeklyConnectionTime,
            String appVersion,
            String storeName) {

        AuthApi api = new AuthApi(context);

        api.sendRatingApi(
                url,
                mobile,
                weeklyConnectionTime,
                appVersion,
                storeName,
                rating,
                isProUser,
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void data, boolean fromCache) {
                        Log.e("sendRatingApi", "onSuccess: ");
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        ToastManager.show(context, message);
                    }
                }
        );
    }

    private RatingState getState() {
        RatingState state = storage.getRatingState(RatingState.class);
        return state != null ? state : new RatingState();
    }

    private void saveState(RatingState state) {
        storage.saveRatingState(state);
    }

    private void showSupportDialog(String message) {

        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(context);

        builder.setTitle("پشتیبانی شکن");

        android.widget.TextView textView = new android.widget.TextView(context);
        textView.setPadding(50, 40, 50, 10);
        textView.setTextSize(15);

        // لینک Shecan.ir قابل کلیک
        String url = "https://shecan.ir";
        android.text.SpannableString spannable =
                new android.text.SpannableString(message);

        int start = message.indexOf("Shecan.ir");
        if (start >= 0) {
            int end = start + "Shecan.ir".length();

            android.text.style.ClickableSpan clickableSpan =
                    new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull android.view.View widget) {

                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    };

            spannable.setSpan(clickableSpan, start, end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(0xFF1976D2),
                    start, end,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textView.setText(spannable);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        builder.setView(textView);

        builder.setPositiveButton("باشه", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog dialog = builder.create();
//        dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
        dialog.show();
    }
}