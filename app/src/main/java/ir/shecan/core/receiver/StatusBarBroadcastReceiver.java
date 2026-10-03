package ir.shecan.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ir.shecan.Shecan;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.core.util.Logger;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class StatusBarBroadcastReceiver extends BroadcastReceiver {
    public static String STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION = "ir.shecan.core.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION";
    public static String STATUS_BAR_BTN_SETTINGS_CLICK_ACTION = "ir.shecan.core.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        try {
            String action = intent.getAction();

            if (action.equals(STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION)) {
                Shecan.deactivateService(context);
            }

            if (action.equals(STATUS_BAR_BTN_SETTINGS_CLICK_ACTION)) {
                Intent settingsIntent = new Intent(context, MainActivityNew.class)
                        .putExtra(MainActivityNew.LAUNCH_FRAGMENT, MainActivityNew.FRAGMENT_SETTINGS)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                context.startActivity(settingsIntent);
            }

        } catch (Exception e) {
            Logger.logException(e);
        }
    }
}
