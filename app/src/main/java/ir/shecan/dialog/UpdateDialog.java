package ir.shecan.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.RequiresApi;

import ir.shecan.R;
import ir.shecan.Shecan;

public class UpdateDialog {
    final Activity activity;

    public UpdateDialog(Activity activity) {
        this.activity = activity;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void show(Boolean isForceUpdate) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_update_app, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        Button openUrlButton = dialogView.findViewById(R.id.updateBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        final AlertDialog dialog = builder.create();

        if (isForceUpdate) {
            cancelBtn.setVisibility(View.GONE);
            dialog.setCancelable(false);
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            if (isForceUpdate) {
                dialog.setCanceledOnTouchOutside(false);
            }
        }

        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = Shecan.ShecanInfo.getUpdateLink(); // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
            }
        });


        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
