package ir.shecan.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

import ir.shecan.R;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDio.DialogActionApiInput;
import ir.shecan.data.modelDio.DialogDismissApiInput;
import ir.shecan.data.modelDto.DynamicDialogViewModel;
import ir.shecan.data.modelDto.EmptyResponse;

public class DynamicAppDialog {

    private static AlertDialog visibleDialog;
    private static final Set<String> SHOWN_DIALOG_IDS = new HashSet<>();

    private final Activity activity;
    private final String mobileNumber;
    private AuthApi authApi;

    public DynamicAppDialog(@NonNull Activity activity, String mobileNumber) {
        this.activity = activity;
        this.mobileNumber = mobileNumber != null ? mobileNumber : "";
    }

    public boolean showIfValid(DynamicDialogViewModel model) {
        if (activity.isFinishing() || model == null || !model.isActive() || !model.hasContent()) {
            return false;
        }
        if (!model.isDismissible() && !hasButton(model.getPrimaryButton()) && !hasButton(model.getSecondaryButton())) {
            return false;
        }
        String shownKey = getShownKey(model);
        synchronized (DynamicAppDialog.class) {
            if (visibleDialog != null && visibleDialog.isShowing()) return false;
            if (SHOWN_DIALOG_IDS.contains(shownKey)) return false;
        }

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_dynamic_app, null);
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dialogView).create();
        boolean dismissible = model.isDismissible();
        boolean[] dismissLogged = {false};

        TextView closeBtn = dialogView.findViewById(R.id.closeBtn);
        TextView titleView = dialogView.findViewById(R.id.dialogTitle);
        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
        Button primaryBtn = dialogView.findViewById(R.id.primaryBtn);
        Button secondaryBtn = dialogView.findViewById(R.id.secondaryBtn);

        titleView.setText(safe(model.getTitle()));
        titleView.setVisibility(isBlank(model.getTitle()) ? View.GONE : View.VISIBLE);
        messageView.setText(safe(model.getMessage()));
        messageView.setVisibility(isBlank(model.getMessage()) ? View.GONE : View.VISIBLE);

        bindActionButton(dialog, primaryBtn, model, model.getPrimaryButton(), "primary");
        bindActionButton(dialog, secondaryBtn, model, model.getSecondaryButton(), "secondary");

        closeBtn.setVisibility(dismissible ? View.VISIBLE : View.GONE);
        closeBtn.setOnClickListener(v -> {
            logDismissOnce(model, dismissLogged);
            dialog.dismiss();
        });

        dialog.setCancelable(dismissible);
        dialog.setCanceledOnTouchOutside(dismissible);
        dialog.setOnCancelListener(d -> logDismissOnce(model, dismissLogged));
        dialog.setOnDismissListener(d -> {
            synchronized (DynamicAppDialog.class) {
                if (visibleDialog == dialog) visibleDialog = null;
            }
        });

        dialog.show();
        synchronized (DynamicAppDialog.class) {
            visibleDialog = dialog;
            SHOWN_DIALOG_IDS.add(shownKey);
        }
        logDisplay(model);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.55f);
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        return true;
    }

    private void bindActionButton(
            AlertDialog dialog,
            Button button,
            DynamicDialogViewModel model,
            DynamicDialogViewModel.DialogButton action,
            String buttonType
    ) {
        if (!hasButton(action)) {
            button.setVisibility(View.GONE);
            return;
        }

        button.setVisibility(View.VISIBLE);
        button.setText(action.getTitle());
        button.setOnClickListener(v -> {
            logAction(model, buttonType);
            if (action.hasRedirectLink()) {
                AppUtils.openUrl(action.getRedirectLink(), activity);
            }
            dialog.dismiss();
        });
    }

    private void logDismissOnce(DynamicDialogViewModel model, boolean[] dismissLogged) {
        if (dismissLogged[0]) return;
        dismissLogged[0] = true;
        TrackingUtils.logEvent(activity, TrackingUtils.EVENT_DYNAMIC_DIALOG_DISMISS,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_DIALOG_ID, safe(model.getDialogId())));
        if (isBlank(model.getDialogId())) return;
        getAuthApi().dialogDismiss(new DialogDismissApiInput(model.getDialogId(), mobileNumber), emptyCallback());
    }

    private void logAction(DynamicDialogViewModel model, String buttonType) {
        android.os.Bundle bundle = TrackingUtils.bundleOf(TrackingUtils.PARAM_DIALOG_ID, safe(model.getDialogId()));
        TrackingUtils.put(bundle, TrackingUtils.PARAM_BUTTON_TYPE, buttonType);
        TrackingUtils.logEvent(activity, TrackingUtils.EVENT_DYNAMIC_DIALOG_ACTION, bundle);
        if (isBlank(model.getDialogId())) return;
        getAuthApi().dialogAction(new DialogActionApiInput(model.getDialogId(), mobileNumber, buttonType), emptyCallback());
    }

    private void logDisplay(DynamicDialogViewModel model) {
        TrackingUtils.logEvent(activity, TrackingUtils.EVENT_DYNAMIC_DIALOG_DISPLAY,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_DIALOG_ID, safe(model.getDialogId())));
    }

    private ApiCallback<EmptyResponse> emptyCallback() {
        return new ApiCallback<EmptyResponse>() {
            @Override
            public void onSuccess(EmptyResponse res, boolean fromCache) {
            }

            @Override
            public void onError(int statusCode, String message) {
            }
        };
    }

    private boolean hasButton(DynamicDialogViewModel.DialogButton button) {
        return button != null && button.hasTitle();
    }

    private AuthApi getAuthApi() {
        if (authApi == null) {
            authApi = new AuthApi(activity.getApplicationContext());
        }
        return authApi;
    }

    private String getShownKey(DynamicDialogViewModel model) {
        if (!isBlank(model.getDialogId())) return model.getDialogId().trim();
        return safe(model.getTitle()) + "|" + safe(model.getMessage());
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
