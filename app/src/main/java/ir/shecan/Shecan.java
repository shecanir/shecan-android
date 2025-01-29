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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.pushpole.sdk.NotificationButtonData;
import com.pushpole.sdk.NotificationData;
import com.pushpole.sdk.PushPole;

import org.json.JSONObject;

import ir.shecan.R;

import ir.shecan.activity.MainActivity;
import ir.shecan.fragment.HomeFragment;
import ir.shecan.service.ApiResponseListener;
import ir.shecan.service.ConnectionStatusListener;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class Shecan extends Application implements ConnectionStatusListener {
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
        // Pro DNS
        add(new DNSServer("pro.shecan.ir", R.string.server_shecan_pro_primary, 5353));
        add(new DNSServer("pro.shecan.ir", R.string.server_shecan_pro_secondary, 53));
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
    private final Handler handler = new Handler();

    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        Logger.init();

        initData();
//        initOneSignal();
        initPushPole();
        initCheckIP();

        updateLocale();
    }

    private void initCheckIP() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ShecanVpnService.isActivated() && ShecanVpnService.isProMode()) {
                    callCheckCurrentIP(Shecan.this);

                    handler.postDelayed(this, 20000); // 20 seconds
                }
            }
        }, 20000);
    }

    private void initPushPole(){
        PushPole.initialize(this,true);

        PushPole.setNotificationListener(new PushPole.NotificationListener() {
            @Override
            public void onNotificationReceived(@NonNull NotificationData notificationData) {
                Log.d("NOTIFzzz", notificationData.toString());
            }

            @Override
            public void onNotificationClicked(@NonNull NotificationData notificationData) {

            }

            @Override
            public void onNotificationButtonClicked(@NonNull NotificationData notificationData, @NonNull NotificationButtonData notificationButtonData) {

            }

            @Override
            public void onCustomContentReceived(@NonNull JSONObject jsonObject) {
                Log.d("NOTIFzzz", jsonObject.toString());
            }

            @Override
            public void onNotificationDismissed(@NonNull NotificationData notificationData) {

            }
        });
    }

    private void initOneSignal() {
        // Verbose Logging set to help debug issues, remove before releasing your app.
//        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
//        OneSignal.getDebug().setLogLevel(LogLevel.DEBUG);

        // OneSignal Initialization
//        OneSignal.initWithContext(this);
//        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
//        OneSignal.setAppId(ONESIGNAL_APP_ID);
//        OneSignal.promptLocation();
//        OneSignal.disableGMSMissingPrompt(true);

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
//        OneSignal.getNotifications().requestPermission(false, Continue.none());



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

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        instance = null;
        prefs = null;
        Logger.shutdown();
        handler.removeCallbacksAndMessages(null);
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
            if (ShecanVpnService.isProMode()) {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
            } else {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            }

            context.startService(Shecan.getServiceIntent(context).setAction(ShecanVpnService.ACTION_ACTIVATE));
            return true;
        }
    }

    public void callCheckCurrentIP(final Context context) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String apiUrl = "https://shecan.ir/ip";
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                apiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String result = response;
                        if (!result.trim().equals(ShecanVpnService.getDynamicIp().trim())) {
                            ShecanVpnService.callCoreAPI(context, new ApiResponseListener() {
                                @Override
                                public void onSuccess(String response) {
                                    ShecanVpnService.callConnectionStatusAPI(context, Shecan.this);
                                }

                                @Override
                                public void onError(String errorMessage) {

                                }

                                @Override
                                public void onInvalid() {
                                    ShecanVpnService.callConnectionStatusAPI(context, Shecan.this);
                                }

                                @Override
                                public void onOutOfRange() {

                                }

                                @Override
                                public void onInTheRange() {

                                }
                            });
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // todo: handle error
                    }
                }
        );

        requestQueue.add(stringRequest);
    }

    public static void deactivateService(Context context) {
        context.startService(getServiceIntent(context).setAction(ShecanVpnService.ACTION_DEACTIVATE));
        context.stopService(getServiceIntent(context));
    }

    public static void setFreeMode() {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_PRO_MODE, false)
                .apply();
    }

    public static void setProMode() {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_PRO_MODE, true)
                .apply();
    }

    public static void setDynamicIPMode() {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_DYNAMIC_IP_MODE, true)
                .apply();
    }

    public static void setStaticIPMode() {
        getPrefs().edit()
                .putBoolean(ShecanVpnService.IS_DYNAMIC_IP_MODE, false)
                .apply();
    }

    public static void setUpdaterLink(String link) {
        getPrefs().edit()
                .putString(ShecanVpnService.UPDATER_LINK, link)
                .apply();
    }

    public static void setDynamicIP(String ip) {
        getPrefs().edit()
                .putString(ShecanVpnService.DYNAMIC_IP, ip)
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

    @Override
    public void onConnected() {

    }

    @Override
    public void onRetry() {
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                ShecanVpnService.callConnectionStatusAPI(Shecan.this, Shecan.this);
            }
        }, 20, TimeUnit.SECONDS);
    }
}

