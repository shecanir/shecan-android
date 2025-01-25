package ir.shecan;

import android.app.Application;
import android.content.Context;
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
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.onesignal.OneSignal;

import ir.shecan.R;

import ir.shecan.activity.MainActivity;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.Configurations;
import ir.shecan.util.LanguageHelper;
import ir.shecan.util.Logger;
import ir.shecan.util.Rule;
import ir.shecan.util.server.DNSServer;
import ir.shecan.util.server.DNSServerHelper;
import ir.shecan.util.server.LocaleHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
public class Shecan extends Application {
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

    private static Shecan instance = null;
    private SharedPreferences prefs;

    private static final String ONESIGNAL_APP_ID = "64aafa29-46dc-46ea-8ac1-36830a241e90";

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        Logger.init();

        initData();
        initOneSignal();

        updateLocale();
    }

    private void initOneSignal(){
        // Enable verbose OneSignal logging for debugging (optional)
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // Initialize OneSignal
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

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

    @Override
    public void onTerminate() {
        Log.d("Shecan", "onTerminate");
        super.onTerminate();

        instance = null;
        prefs = null;
        Logger.shutdown();
    }

    public static Intent getServiceIntent(Context context) {
        return new Intent(context, ShecanVpnService.class);
    }

    public static boolean switchService() {
        if (ShecanVpnService.isActivated()) {
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
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            context.startService(Shecan.getServiceIntent(context).setAction(ShecanVpnService.ACTION_ACTIVATE));
            return true;
        }
    }

    public static void deactivateService(Context context) {
        context.startService(getServiceIntent(context).setAction(ShecanVpnService.ACTION_DEACTIVATE));
        context.stopService(getServiceIntent(context));
    }

    public static void setFreeMode(Context context) {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_PRO_MODE, false)
                .apply();
    }

    public static void setProMode(Context context) {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_PRO_MODE, true)
                .apply();
    }

    public static void setDynamicIPMode(Context context) {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_DYNAMIC_IP_MODE, true)
                .apply();
    }

    public static void setStaticIPMode(Context context) {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_DYNAMIC_IP_MODE, false)
                .apply();
    }


    public static void updateShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Log.d("Shecan", "Updating shortcut");
            boolean activate = ShecanVpnService.isActivated();
            String notice = activate ? context.getString(R.string.button_text_deactivate) : context.getString(R.string.button_text_activate);
            ShortcutInfo info = new ShortcutInfo.Builder(context, Shecan.SHORTCUT_ID_ACTIVATE)
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

    public static Shecan getInstance() {
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

