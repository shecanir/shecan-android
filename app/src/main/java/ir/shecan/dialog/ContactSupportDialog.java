package ir.shecan.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import ir.shecan.R;
import ir.shecan.Shecan;

public class ContactSupportDialog {
    final Activity activity;

    public ContactSupportDialog(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_contact_support, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        Button contactButton = dialogView.findViewById(R.id.contactButton);
        Button renewalButton = dialogView.findViewById(R.id.renewalButton2);
        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                String url = Shecan.ShecanInfo.getTicketingLink(); // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
            }
        });

        renewalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                String url = Shecan.ShecanInfo.getPurchaseLink(); // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
            }
        });

        dialog.show();
    }
}
