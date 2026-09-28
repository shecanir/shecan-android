package ir.shecan.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import ir.shecan.core.util.PersianTools;

public class OtpReceiver extends BroadcastReceiver {

    public interface OtpReceiveListener {
        void onOtpReceived(String otp);
    }

    private OtpReceiveListener listener;

    public void setListener(OtpReceiveListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {

            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch (status.getStatusCode()) {

                case CommonStatusCodes.SUCCESS:
                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    if (message == null) {
                        break;
                    }

                    // استخراج 6 رقم
                    String digits = PersianTools.extractEnglishDigits(message);
                    if (digits.length() < 6) {
                        break;
                    }
                    String otp = digits.substring(0, 6);

                    if (listener != null) listener.onOtpReceived(otp);
                    break;

                case CommonStatusCodes.TIMEOUT:
                    break;
            }
        }
    }
}
