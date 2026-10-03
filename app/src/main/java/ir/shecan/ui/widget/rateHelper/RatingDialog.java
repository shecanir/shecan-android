package ir.shecan.ui.widget.rateHelper;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.activity.MainActivityNew;

public class RatingDialog extends Dialog {

    private static final String TAG = "RatingDialog";
    private static RatingDialog visibleDialog;

    private RatingBar ratingBar;
    private Button btnSubmit;
    private Button btnLater;
    private TextView txtDescription;
    private RatingManager ratingManager;
    private String url;

    private boolean userRated = false;

    public RatingDialog(Context context, String url) {
        super(context);
        ratingManager = new RatingManager(context);
        this.url = url;
    }

    public static boolean showIfNotVisible(@NonNull Context context, String url) {
        RatingDialog dialog;
        synchronized (RatingDialog.class) {
            if (visibleDialog != null) {
                return false;
            }
            dialog = new RatingDialog(context, url);
            visibleDialog = dialog;
        }

        try {
            dialog.show();
            return true;
        } catch (RuntimeException exception) {
            dialog.releaseVisibleReference();
            Log.w(TAG, "Unable to display rating dialog", exception);
            return false;
        }
    }

    private void releaseVisibleReference() {
        synchronized (RatingDialog.class) {
            if (visibleDialog == this) {
                visibleDialog = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_rating);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ratingBar = findViewById(R.id.ratingBar);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnLater = findViewById(R.id.btnLater);
        txtDescription = findViewById(R.id.txtDescription);

        Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        findViewById(android.R.id.content).startAnimation(fadeIn);

        setOnDismissListener(dialog -> {
            releaseVisibleReference();
            if (!userRated) {
                ratingManager.onDialogDismissed();
            }
        });

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {

            if (rating <= 2) {
                txtDescription.setText("چه چیزی اذیتت کرد؟ بهمون بگو 🙏");
            } else if (rating == 3) {
                txtDescription.setText("مرسی ❤️ چطور می‌تونیم بهترش کنیم؟");
            } else {
                txtDescription.setText("عالیه! خوشحالیم که راضی بودی 😍");
            }
        });

        btnLater.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> {

            float rating = ratingBar.getRating();

            if (rating == 0) {
                dismiss();
                return;
            }

            AppStorage storage = new AppStorage(getContext());
            VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

            if (token != null) {
                userRated = true;

                long weeklyTime = Shecan.getPrefs()
                        .getLong("weekly_connection_time", 0L);

                AppStorage appStorage = new AppStorage(getContext());
                IssuesViewModel viewModel = appStorage.getIssue(IssuesViewModel.class);
                boolean isHaveProItemInList = false;
                if (viewModel != null && viewModel.getIssues() != null) {
                    for (IssuesViewModel.IssuesDTO issue : viewModel.getIssues()) {
                        ServiceItem item = ServiceItemMapper.map(getContext(), issue);
                        boolean isFreeMode = "0".equals(item.getOrderCode());
                        if (!isFreeMode) {
                            isHaveProItemInList = true;
                        }
                    }
                }

                ratingManager.onUserRated(
                        url,
                        (int) rating,
                        ShecanVpnService.isProMode() || isHaveProItemInList,
                        token.getLogin(),
                        weeklyTime,
                        BuildConfig.VERSION_NAME,
                        Constant.Store
                );
            }

            dismiss();
        });
    }
}
