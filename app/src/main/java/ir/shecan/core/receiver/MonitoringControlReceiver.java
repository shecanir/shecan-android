package ir.shecan.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ir.shecan.Shecan;
import ir.shecan.core.service.MonitoringService;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.util.Logger;

public class MonitoringControlReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE =
            "ir.shecan.core.receiver.MonitoringControlReceiver.ACTION_TOGGLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_TOGGLE.equals(intent.getAction())) return;

        try {
            Shecan app = Shecan.getInstance();
            if (ShecanVpnService.isActivated() || (app != null && app.isVpnConnecting())) {
                if (app != null) {
                    app.cancelVpnConnection(context);
                } else {
                    Shecan.deactivateService(context);
                }
            } else {
                Shecan.activateService(context);
            }
        } catch (RuntimeException e) {
            Logger.logException(e);
        } finally {
            MonitoringService.refresh(context);
        }
    }
}
