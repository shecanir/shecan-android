package org.itxtech.daedalus;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import org.itxtech.daedalus.activity.MainActivity;
import org.itxtech.daedalus.service.DaedalusVpnService;
import org.itxtech.daedalus.util.Configurations;
import org.itxtech.daedalus.util.LanguageHelper;
import org.itxtech.daedalus.util.Logger;
import org.itxtech.daedalus.util.Rule;
import org.itxtech.daedalus.util.server.DNSServer;
import org.itxtech.daedalus.util.server.DNSServerHelper;
import org.itxtech.daedalus.util.server.LocaleHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class Daedalus extends Application {
    static {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                FirebaseCrash.report(e);
            }
        });
    }

    private static final String SHORTCUT_ID_ACTIVATE = "shortcut_activate";

    public static final List<DNSServer> DNS_SERVERS = new ArrayList<DNSServer>() {{
        add(new DNSServer("dns.shecan.ir", R.string.server_shecan_primary, 5353));
        add(new DNSServer("dns.shecan.ir", R.string.server_shecan_secondary, 53));
    }};

    public static final List<Rule> RULES = new ArrayList<Rule>() {{
    }};

    public static final String[] DEFAULT_TEST_DOMAINS = new String[]{
            "check.shecan.ir"
    };

    public static Configurations configurations;

    public static String rulePath = null;
    public static String logPath = null;
    private static String configPath = null;

    private static Daedalus instance = null;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        Logger.init();

        initData();

        updateLocale();
    }

    private void initDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) {
            Logger.warning(dir + " is not a directory. Delete result: " + String.valueOf(directory.delete()));
        }
        if (!directory.exists()) {
            Logger.debug(dir + " does not exist. Create result: " + String.valueOf(directory.mkdirs()));
        }
    }

    private void initData() {
        PreferenceManager.setDefaultValues(this, R.xml.perf_settings, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String path;
        if (getExternalFilesDir(null) != null) {
            path = getExternalFilesDir(null).getPath();
        } else {
            path = getFilesDir().getPath();
        }
        rulePath = path + "/rules/";
        logPath = path + "/logs/";
        configPath = path + "/config.json";

        initDirectory(rulePath);
        initDirectory(logPath);

        if (configPath != null) {
            configurations = Configurations.load(new File(configPath));
        } else {
            configurations = new Configurations();
        }
    }

    public static <T> T parseJson(Class<T> beanClass, JsonReader reader) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(reader, beanClass);
    }


    public static SharedPreferences getPrefs() {
        return getInstance().prefs;
    }

    public static boolean isDarkTheme() {
        return getInstance().prefs.getBoolean("settings_dark_theme", false);
    }

    @Override
    public void onTerminate() {
        Log.d("Daedalus", "onTerminate");
        super.onTerminate();

        instance = null;
        prefs = null;
        Logger.shutdown();
    }

    public static Intent getServiceIntent(Context context) {
        return new Intent(context, DaedalusVpnService.class);
    }

    public static boolean switchService() {
        if (DaedalusVpnService.isActivated()) {
            deactivateService(instance);
            return false;
        } else {
            activateService(instance);
            return true;
        }
    }

    public static boolean activateService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            return false;
        } else {
            DaedalusVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
            DaedalusVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            context.startService(Daedalus.getServiceIntent(context).setAction(DaedalusVpnService.ACTION_ACTIVATE));
            return true;
        }
    }

    public static void deactivateService(Context context) {
        context.startService(getServiceIntent(context).setAction(DaedalusVpnService.ACTION_DEACTIVATE));
        context.stopService(getServiceIntent(context));
    }

    public static void updateShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Log.d("Daedalus", "Updating shortcut");
            boolean activate = DaedalusVpnService.isActivated();
            String notice = activate ? context.getString(R.string.button_text_deactivate) : context.getString(R.string.button_text_activate);
            ShortcutInfo info = new ShortcutInfo.Builder(context, Daedalus.SHORTCUT_ID_ACTIVATE)
                    .setLongLabel(notice)
                    .setShortLabel(notice)
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(new Intent(context, MainActivity.class).setAction(Intent.ACTION_VIEW)
                            .putExtra(MainActivity.LAUNCH_ACTION, activate ? MainActivity.LAUNCH_ACTION_DEACTIVATE : MainActivity.LAUNCH_ACTION_ACTIVATE))
                    .build();
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(SHORTCUT_SERVICE);
            shortcutManager.addDynamicShortcuts(Collections.singletonList(info));
        }
    }

    public static void donate() {
        openUri("https://qr.alipay.com/a6x07022gffiehykicipv1a");
    }

    public static void openUri(String uri) {
        try {
            instance.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Logger.logException(e);
        }
    }

    public void updateLocale() {
        setLocale(LanguageHelper.getLanguage());
    }

    private void setLocale(String lang) {
        LocaleHelper.setLocale(this, new Locale(lang));
    }

    public static Daedalus getInstance() {
        return instance;
    }

    public static void changeLanguageType(String locale) {
        getInstance().setLocale(locale);
    }

    public static Locale getLanguageType(Context context) {

        if (instance != null) { // Use currently edited context instance to get locale
            context = instance;
        }

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return config.getLocales().get(0);
        } else {
            return config.locale;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}

