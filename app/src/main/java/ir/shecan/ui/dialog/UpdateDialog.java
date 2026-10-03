package ir.shecan.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.update.SiteApkUpdater;

public class UpdateDialog {
    final Activity activity;

    public UpdateDialog(Activity activity) {
        this.activity = activity;
    }

    public void show(Boolean isForceUpdate) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_update_app, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        Button openUrlButton = dialogView.findViewById(R.id.updateBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        TextView statusTv = dialogView.findViewById(R.id.updateStatusTv);
        ProgressBar progressBar = dialogView.findViewById(R.id.updateProgress);
        final AlertDialog dialog = builder.create();
        final SiteApkUpdater[] siteUpdater = new SiteApkUpdater[1];
        final boolean[] siteApkReady = {false};

        if (isForceUpdate) {
            cancelBtn.setVisibility(View.GONE);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        openUrlButton.setOnClickListener(v -> {
            if ("Site".equals(BuildConfig.STORE)) {
                if (siteApkReady[0] && siteUpdater[0] != null) {
                    siteUpdater[0].installDownloadedApk();
                    return;
                }
                siteUpdater[0] = new SiteApkUpdater(activity);
                siteUpdater[0].download(BuildConfig.SITE_APK_UPDATE_URL, new SiteApkUpdater.Listener() {
                    @Override
                    public void onMissingUrl() {
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_url_missing), false, 0);
                    }

                    @Override
                    public void onInstallPermissionRequired() {
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_install_permission_required), false, 0);
                    }

                    @Override
                    public void onDownloadStarted() {
                        siteApkReady[0] = false;
                        openUrlButton.setEnabled(false);
                        cancelBtn.setEnabled(false);
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_download_started), true, 0);
                    }

                    @Override
                    public void onProgress(int progress) {
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_downloading_percent, progress), true, progress);
                    }

                    @Override
                    public void onDownloaded() {
                        siteApkReady[0] = true;
                        openUrlButton.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        openUrlButton.setText(R.string.site_update_install);
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_install_ready), true, 100);
                    }

                    @Override
                    public void onFailed() {
                        siteApkReady[0] = false;
                        openUrlButton.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        openUrlButton.setText(R.string.update);
                        setUpdateStatus(statusTv, progressBar, activity.getString(R.string.site_update_download_failed), false, 0);
                    }
                });
                return;
            }
            String url = Shecan.ShecanInfo.getUpdateLink();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.55f);
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setUpdateStatus(TextView statusTv, ProgressBar progressBar, String message, boolean showProgress, int progress) {
        statusTv.setVisibility(View.VISIBLE);
        statusTv.setText(message);
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        progressBar.setProgress(Math.max(0, Math.min(100, progress)));
    }
}
