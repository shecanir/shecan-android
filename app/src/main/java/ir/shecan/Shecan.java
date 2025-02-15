package ir.shecan;

import android.annotation.SuppressLint;
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
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.pushpole.sdk.NotificationButtonData;
import com.pushpole.sdk.NotificationData;
import com.pushpole.sdk.PushPole;

import org.json.JSONException;
import org.json.JSONObject;

import ir.shecan.activity.MainActivity;
import ir.shecan.service.CoreApiResponseListener;
import ir.shecan.service.BaseApiResponseListener;
import ir.shecan.service.ConnectionStatusApiListener;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.Configurations;
import ir.shecan.util.LanguageHelper;
import ir.shecan.util.Logger;
import ir.shecan.util.Rule;
import ir.shecan.util.server.DNSServer;
import ir.shecan.util.server.DNSServerHelper;
import ir.shecan.util.server.LocaleHelper;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
//import javax.security.cert.X509Certificate;

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
public class Shecan extends Application implements ConnectionStatusApiListener {
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

    private final Handler handler = new Handler();

    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        Fresco.initialize(this);

        Logger.init();
        handleSSLHandshake();

        initData();
        initPushPole();
        initCheckIP();

        updateLocale();
    }

    /**
     * Enables https connections
     */
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void initCheckIP() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ShecanVpnService.isActivated() && ShecanVpnService.isProMode() && ShecanVpnService.isDynamicIPMode()) {
                    callCheckCurrentIP(Shecan.this);

                    handler.postDelayed(this, 20000); // 20 seconds
                }
            }
        }, 20000);
    }

    private void initPushPole() {
        PushPole.initialize(this, true);

        PushPole.setNotificationListener(new PushPole.NotificationListener() {
            @Override
            public void onNotificationReceived(@NonNull NotificationData notificationData) {
            }

            @Override
            public void onNotificationClicked(@NonNull NotificationData notificationData) {

            }

            @Override
            public void onNotificationButtonClicked(@NonNull NotificationData notificationData, @NonNull NotificationButtonData notificationButtonData) {

            }

            @Override
            public void onCustomContentReceived(@NonNull JSONObject jsonObject) {
            }

            @Override
            public void onNotificationDismissed(@NonNull NotificationData notificationData) {

            }
        });
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
                            ShecanVpnService.callCoreAPI(context, new CoreApiResponseListener() {
                                @Override
                                public void onSuccess(String response) {
                                    ShecanVpnService.callConnectionStatusAPI(context, Shecan.this, null);
                                }

                                @Override
                                public void onError(String errorMessage) {

                                }

                                @Override
                                public void onInvalid() {
                                    ShecanVpnService.callConnectionStatusAPI(context, Shecan.this, null);
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
                ShecanVpnService.callConnectionStatusAPI(Shecan.this, Shecan.this, null);
            }
        }, 20, TimeUnit.SECONDS);
    }

    public static class ShecanInfo {

        private static final String CURRENT_VERSION = "CURRENT_VERSION";
        private static final String MIN_VERSION = "MIN_VERSION";
        private static final String UPDATE_LINK = "UPDATE_LINK";
        private static final String BANNER_IMAGE_URL = "BANNER_IMAGE_URL";
        private static final String BANNER_LINK = "BANNER_LINK";
        private static final String DYNAMIC_IP_GUIDE_LINK = "DYNAMIC_IP_GUIDE_LINK";
        private static final String TICKETING_LINK = "TICKETING_LINK";
        private static final String PURCHASE_LINK = "PURCHASE_LINK";

        public static void fetchData(Context context, BaseApiResponseListener listener) {
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            String apiUrl = "https://shecan.ir/app/home-page";
            StringRequest stringRequest = new StringRequest(
                    Request.Method.GET,
                    apiUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try{
                                JSONObject jsonObject = new JSONObject(response);
                                setCurrentVersion(jsonObject.getString("current_version"));

                                setMinVersion(jsonObject.getString("min_version"));
                                setUpdateLink(jsonObject.getString("update_link"));
                                setBannerImageUrl(jsonObject.getString("banner_image_url"));
                                setBannerLink(jsonObject.getString("banner_link"));
                                setDynamicIpGuideLink(jsonObject.getString("dynamic_ip_guide_link"));
                                setTicketingLink(jsonObject.getString("ticketing_link"));
                                setPurchaseLink(jsonObject.getString("purchase_link"));
                                listener.onSuccess();
                            }catch (JSONException e){
                                listener.onError("خطای سرور");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            listener.onError(error.toString());
                        }
                    }
            );

            stringRequest.setShouldCache(false);
            requestQueue.getCache().clear();
            requestQueue.add(stringRequest);
        }

        private static void setCurrentVersion(String version) {
            getPrefs().edit()
                    .putString(CURRENT_VERSION, version)
                    .apply();
        }

        private static void setMinVersion(String version) {
            getPrefs().edit()
                    .putString(MIN_VERSION, version)
                    .apply();
        }

        private static void setUpdateLink(String link) {
            getPrefs().edit()
                    .putString(UPDATE_LINK, link)
                    .apply();
        }

        private static void setBannerImageUrl(String url) {
            getPrefs().edit()
                    .putString(BANNER_IMAGE_URL, url)
                    .apply();
        }

        private static void setBannerLink(String link) {
            getPrefs().edit()
                    .putString(BANNER_LINK, link)
                    .apply();
        }

        private static void setDynamicIpGuideLink(String link) {
            getPrefs().edit()
                    .putString(DYNAMIC_IP_GUIDE_LINK, link)
                    .apply();
        }

        private static void setTicketingLink(String link) {
            getPrefs().edit()
                    .putString(TICKETING_LINK, link)
                    .apply();
        }

        private static void setPurchaseLink(String link) {
            getPrefs().edit()
                    .putString(PURCHASE_LINK, link)
                    .apply();
        }

        public static String getCurrentVersion() {
            return Shecan.getPrefs().getString(CURRENT_VERSION, "1.0.0");
        }

        public static String getMinVersion() {
            return Shecan.getPrefs().getString(MIN_VERSION, "1.0.0");
        }

        public static String getUpdateLink() {
            return Shecan.getPrefs().getString(UPDATE_LINK, "https://shecan.ir/app");
        }

        public static String getBannerImageUrl() {
            return Shecan.getPrefs().getString(BANNER_IMAGE_URL, "");
        }

        public static String getBannerLink() {
            return Shecan.getPrefs().getString(BANNER_LINK, "https://shecan.ir");
        }

        public static String getDynamicIpGuideLink() {
            return Shecan.getPrefs().getString(DYNAMIC_IP_GUIDE_LINK, "https://shecan.ir/tutorials");
        }

        public static String getTicketingLink() {
            return Shecan.getPrefs().getString(TICKETING_LINK, "https://my.shecan.ir");
        }

        public static String getPurchaseLink() {
            return Shecan.getPrefs().getString(TICKETING_LINK, "https://shecan.ir/order?order=9");
        }
    }
}

