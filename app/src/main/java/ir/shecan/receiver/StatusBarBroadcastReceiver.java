package ir.shecan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ir.shecan.Shecan;
import ir.shecan.activity.MainActivity;
import ir.shecan.util.Logger;

import java.lang.reflect.Method;

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
    public static String STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION = "ir.shecan.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION";
    public static String STATUS_BAR_BTN_SETTINGS_CLICK_ACTION = "ir.shecan.receiver.StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION)) {
            Shecan.deactivateService(context);
        }
        if (intent.getAction().equals(STATUS_BAR_BTN_SETTINGS_CLICK_ACTION)) {
            Intent settingsIntent = new Intent(context, MainActivity.class).putExtra(MainActivity.LAUNCH_FRAGMENT, MainActivity.FRAGMENT_SETTINGS);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            try {
                Object statusBarManager = context.getSystemService("statusbar");
                Method collapse = statusBarManager.getClass().getMethod("collapsePanels");
                collapse.invoke(statusBarManager);
            } catch (Exception e) {
                Logger.logException(e);
            }
        }
    }
}
