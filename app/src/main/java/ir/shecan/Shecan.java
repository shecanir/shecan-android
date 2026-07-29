package ir.shecan;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.sentry.Sentry;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.User;
import ir.shecan.core.service.BaseApiResponseListener;
import ir.shecan.core.service.ConnectionStatusApiListener;
import ir.shecan.core.service.CoreApiResponseListener;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.monitoring.MonitoringAppState;
import ir.shecan.core.service.VolleyHelper;
import ir.shecan.core.util.Configurations;
import ir.shecan.core.util.LanguageHelper;
import ir.shecan.core.util.Logger;
import ir.shecan.core.util.Rule;
import ir.shecan.core.util.ToastManager;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.core.util.server.DNSServer;
import ir.shecan.core.util.server.DNSServerHelper;
import ir.shecan.core.util.server.LocaleHelper;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.activity.MainActivityNew;

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
//    static {
//        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
    /// /                FirebaseCrashlytics.getInstance().recordException(e);
//        });
//    }

    private static final String SHORTCUT_ID_ACTIVATE = "shortcut_activate";

    public static final List<DNSServer> DNS_SERVERS = new ArrayList<DNSServer>() {{
        add(new DNSServer("free.shecan.ir", R.string.server_shecan_primary, 5353));
        add(new DNSServer("free.shecan.ir", R.string.server_shecan_secondary, 53));
        // Pro DNS
        add(new DNSServer("pro.shecan.ir", R.string.server_shecan_pro_primary, 5353));
        add(new DNSServer("pro.shecan.ir", R.string.server_shecan_pro_secondary, 53));
    }};

    public static final List<Rule> RULES = new ArrayList<Rule>() {
    };

    public static final String[] DEFAULT_TEST_DOMAINS = new String[]{
            "check.shecan.ir"
    };

    public static Configurations configurations;

    public static String rulePath = null;
    public static String logPath = null;

    private static Shecan instance = null;
    private SharedPreferences prefs;
    private long appStartedElapsedMs;

    private final Handler handler = new Handler();

    private ScheduledExecutorService scheduler;

    private final MutableLiveData<Integer> vpnState = new MutableLiveData<>();
    private final MutableLiveData<String> vpnStatus = new MutableLiveData<>();

    public MutableLiveData<Integer> getVpnState() {
        return vpnState;
    }
    public MutableLiveData<String> getVpnStatus() {
        return vpnStatus;
    }

    public void reportVpnFailure(String message) {
        String userMessage = message == null || message.trim().isEmpty()
                ? getString(R.string.connection_error_generic)
                : message.trim();
        vpnHandler.post(() -> {
            vpnStatus.setValue("");
            vpnState.setValue(0);
            ToastManager.show(this, userMessage);
        });
    }

    public static long getAppStartedElapsedMs() {
        return instance != null ? instance.appStartedElapsedMs : 0L;
    }

    private final MutableLiveData<Boolean> proActivatedEvent = new MutableLiveData<>();

    public MutableLiveData<Boolean> getProActivatedEvent() {
        return proActivatedEvent;
    }

    private Handler vpnHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        appStartedElapsedMs = SystemClock.elapsedRealtime();
        MonitoringAppState.initialize(this);
        Fresco.initialize(this);

        Logger.init();

        addSentry();
        initData();
//        initPushPole();
        initCheckIP();

        updateLocale();
    }

    public void waitForDeactivateThenReconnect(Context context) {

        if (!pendingReconnect) return;

        if (!ShecanVpnService.isActivated()) {
            pendingReconnect = false;
            connectVpn(context, null);
            return;
        }

        vpnHandler.postDelayed(
                () -> waitForDeactivateThenReconnect(context),
                1000
        );
    }

    public void connectVpn(Context context, CoreApiResponseListener listener) {

        AppStorage storage = new AppStorage(context);
        ServiceItem serviceItem = storage.getServiceStatus(ServiceItem.class);

        getVpnState().postValue(1);

        boolean isPaidService = serviceItem != null
                && serviceItem.getOrderCode() != null
                && !serviceItem.getOrderCode().trim().isEmpty()
                && !"0".equals(serviceItem.getOrderCode());
        String updateLink = serviceItem != null && serviceItem.getUpdateLink() != null
                ? serviceItem.getUpdateLink().trim()
                : "";

        if (isPaidService && updateLink.isEmpty()) {
            Logger.error("Paid service " + serviceItem.getOrderCode()
                    + " has no update link (custom field 95)");
            reportVpnFailure(context.getString(R.string.pro_service_config_missing));
            return;
        }

        if (isPaidService) {

            // UpdateLink Mode
            setProMode();

            String updaterUrl = Uri.parse("https://ddns.shecan.ir/update")
                    .buildUpon()
                    .appendQueryParameter("password", updateLink)
                    .build()
                    .toString();
            setUpdaterLink(updaterUrl);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ShecanVpnService.callCoreAPI(context, listener);
            }, 1000);

        } else {
            // Free mode
            setFreeMode();

            Intent intent = new Intent(context, MainActivityNew.class);
            intent.putExtra(
                    MainActivityNew.LAUNCH_ACTION,
                    MainActivityNew.LAUNCH_ACTION_ACTIVATE
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                context.startActivity(intent);
            }, 1000);
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

//    private void initPushPole() {
//        try {
//            new Handler(getMainLooper()).post(() -> {
//                try {
//                    PushPole.initialize(this, true);
//
//                    PushPole.setNotificationListener(new PushPole.NotificationListener() {
//                        @Override
//                        public void onNotificationReceived(@NonNull NotificationData notificationData) {
//                        }
//
//                        @Override
//                        public void onNotificationClicked(@NonNull NotificationData notificationData) {
//                        }
//
//                        @Override
//                        public void onNotificationButtonClicked(@NonNull NotificationData notificationData, @NonNull NotificationButtonData notificationButtonData) {
//                        }
//
//                        @Override
//                        public void onCustomContentReceived(@NonNull JSONObject jsonObject) {
//                        }
//
//                        @Override
//                        public void onNotificationDismissed(@NonNull NotificationData notificationData) {
//                        }
//                    });
//                } catch (Exception e) {
//                    Logger.logException(e);
//                }
//            });
//        } catch (Exception e) {
//            Logger.logException(e);
//        }
//    }


    private void initDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) {
            Logger.warning(dir + " is not a directory. Delete result: " + directory.delete());
        }
        if (!directory.exists()) {
            Logger.debug(dir + " does not exist. Create result: " + directory.mkdirs());
        }
    }

    private void initData() {
        prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE);

        String path;
        if (getExternalFilesDir(null) != null) {
            path = Objects.requireNonNull(getExternalFilesDir(null)).getPath();
        } else {
            path = getFilesDir().getPath();
        }

        rulePath = path + "/rules/";
        logPath = path + "/logs/";
        String configPath = path + "/config.json";

        initDirectory(rulePath);
        initDirectory(logPath);

        File configFile = new File(configPath);
        if (configFile.exists()) {
            configurations = Configurations.load(configFile);
        } else {
            configurations = new Configurations();
        }
    }

    public static <T> T parseJson(Class<T> beanClass, JsonReader reader) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
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

    public static void activateService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent == null) {
            if (ShecanVpnService.isProMode()) {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
            } else {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            }

            Intent serviceIntent = Shecan.getServiceIntent(context).setAction(ShecanVpnService.ACTION_ACTIVATE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }

    public void callCheckCurrentIP(final Context context) {
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);
        String apiUrl = "https://shecan.ir/ip";
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                apiUrl,
                response -> {
                    if (!response.trim().equals(ShecanVpnService.getDynamicIp().trim())) {
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
                },
                error -> {
                    // todo: handle error
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
                    .setIntent(new Intent(context, MainActivityNew.class).setAction(Intent.ACTION_VIEW)
                            .putExtra(MainActivityNew.LAUNCH_ACTION, activate ? MainActivityNew.LAUNCH_ACTION_DEACTIVATE : MainActivityNew.LAUNCH_ACTION_ACTIVATE))
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

    public void addSentry() {
        SentryAndroid.init(this, options -> {
            options.setDsn("https://38ff2bbf9f30b0896c9b311ab4e647b8@sentry.hamravesh.com/9149");
            options.setEnvironment("production");
            options.setTracesSampleRate(0.0);
            options.setEnableExternalConfiguration(true);
            options.setDebug(false);
        });

        AppStorage storage = new AppStorage(this);
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null) {
            User user = new User();
            user.setUsername(token.getLogin());
            user.setEmail(token.getMail());
            Sentry.setUser(user);
        }

//        try {
//            throw new Exception("Test crash for Sentry!");
//        } catch (Exception e) {
//            Sentry.captureException(e);
//        }
    }

    private void setLocale(String lang) {
        LocaleHelper.setLocale(this, new Locale(lang));
    }

    public static Shecan getInstance() {
        return instance;
    }

//    public static void changeLanguageType(String locale) {
//        getInstance().setLocale(locale);
//    }

//    public static Locale getLanguageType(Context context) {
//
//        if (instance != null) { // Use currently edited context instance to get locale
//            context = instance;
//        }
//
//        Resources resources = context.getResources();
//        Configuration config = resources.getConfiguration();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            return config.getLocales().get(0);
//        } else {
//            return config.locale;
//        }
//    }

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
                if (ShecanVpnService.isActivated())
                    ShecanVpnService.callConnectionStatusAPI(Shecan.this, Shecan.this, null);
            }
        }, 20, TimeUnit.SECONDS);
    }

    public boolean pendingReconnect;

    public static class ShecanInfo {

        private static final String CURRENT_VERSION = "CURRENT_VERSION";
        private static final String MIN_VERSION = "MIN_VERSION";
        private static final String UPDATE_LINK = "UPDATE_LINK";
        private static final String BANNER_IMAGE_URL = "BANNER_IMAGE_URL";
        private static final String BANNER_LINK = "BANNER_LINK";
        private static final String DYNAMIC_IP_GUIDE_LINK = "DYNAMIC_IP_GUIDE_LINK";
        private static final String TICKETING_LINK = "TICKETING_LINK";
        private static final String PURCHASE_LINK = "PURCHASE_LINK";
        private static final String DYNAMIC_BANNER_URL = "DYNAMIC_BANNER_URL";
        private static final String DIALOG_MATCH_URL = "DIALOG_MATCH_URL";
        private static final String DIALOG_DISMISS_URL = "DIALOG_DISMISS_URL";
        private static final String DIALOG_ACTION_URL = "DIALOG_ACTION_URL";
        private static final String MONITORING_TARGET_URL = "MONITORING_TARGET_URL";
        private static final String MONITORING_LOGS_URL = "MONITORING_LOGS_URL";

        private static final String DEFAULT_DYNAMIC_BANNER_URL = "https://my.shecan.ir/api/banner/match";
        private static final String DEFAULT_DIALOG_MATCH_URL = "https://n8n.coolify.shcn.ir/webhook/api/dialog/match";
        private static final String DEFAULT_DIALOG_DISMISS_URL = "https://n8n.coolify.shcn.ir/webhook/api/dialog/dismiss";
        private static final String DEFAULT_DIALOG_ACTION_URL = "https://n8n.coolify.shcn.ir/webhook/api/dialog/action";
        private static final String DEFAULT_MONITORING_TARGET_URL = "https://my.shecan.ir/monitoring/targets";
        private static final String DEFAULT_MONITORING_LOGS_URL = "https://my.shecan.ir/monitoring/logs";

        public static void fetchData(Context context, BaseApiResponseListener listener) {
            RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);
            String apiUrl = "https://shecan.ir/app/home-page";
            StringRequest stringRequest = new StringRequest(
                    Request.Method.GET,
                    apiUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonObject = new JSONObject(response);

                                JSONObject versionObject = jsonObject.getJSONObject("version");
                                JSONObject androidVersion = versionObject.getJSONObject("android");

                                setCurrentVersion(androidVersion.getString("current_version"));
                                setMinVersion(androidVersion.getString("min_version"));

                                setUpdateLink(jsonObject.getString("update_link"));
                                setBannerImageUrl(jsonObject.getString("banner_image_url"));
                                setBannerLink(jsonObject.getString("banner_link"));
                                setDynamicIpGuideLink(jsonObject.getString("dynamic_ip_guide_link"));
                                setTicketingLink(jsonObject.getString("ticketing_link"));
                                setPurchaseLink(jsonObject.getString("purchase_link"));
                                saveDynamicData(jsonObject.optJSONObject("dynamic_data"));
                                saveMonitoring(jsonObject.optJSONObject("monitoring"));

                                listener.onSuccess();
                            } catch (JSONException e) {
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

        private static void setDynamicBannerUrl(String url) {
            setString(DYNAMIC_BANNER_URL, url);
        }

        private static void setDialogMatchUrl(String url) {
            setString(DIALOG_MATCH_URL, url);
        }

        private static void setDialogDismissUrl(String url) {
            setString(DIALOG_DISMISS_URL, url);
        }

        private static void setDialogActionUrl(String url) {
            setString(DIALOG_ACTION_URL, url);
        }

        private static void setMonitoringTargetUrl(String url) {
            setString(MONITORING_TARGET_URL, url);
        }

        private static void setMonitoringLogsUrl(String url) {
            setString(MONITORING_LOGS_URL, url);
        }

        private static void setString(String key, String value) {
            if (isBlank(value)) return;
            getPrefs().edit()
                    .putString(key, value.trim())
                    .apply();
        }

        private static void saveDynamicData(JSONObject dynamicData) {
            if (dynamicData == null) return;
            setDynamicBannerUrl(dynamicData.optString("banner", null));

            JSONObject dialog = dynamicData.optJSONObject("dialog");
            if (dialog == null) return;
            setDialogMatchUrl(dialog.optString("match", null));
            setDialogDismissUrl(dialog.optString("dismiss", null));
            setDialogActionUrl(dialog.optString("action", null));
        }

        private static void saveMonitoring(JSONObject monitoring) {
            if (monitoring == null) return;
            setMonitoringTargetUrl(monitoring.optString("target", null));
            setMonitoringLogsUrl(monitoring.optString("logs", null));
        }

        public static void saveHomePageConfig(HomePage homePage) {
            if (homePage == null) return;

            HomePage.DynamicDataDTO dynamicData = homePage.getDynamicData();
            if (dynamicData != null) {
                setDynamicBannerUrl(dynamicData.getBanner());

                HomePage.DynamicDataDTO.DialogDTO dialog = dynamicData.getDialog();
                if (dialog != null) {
                    setDialogMatchUrl(dialog.getMatch());
                    setDialogDismissUrl(dialog.getDismiss());
                    setDialogActionUrl(dialog.getAction());
                }
            }

            HomePage.MonitoringDTO monitoring = homePage.getMonitoring();
            if (monitoring != null) {
                setMonitoringTargetUrl(monitoring.getTarget());
                setMonitoringLogsUrl(monitoring.getLogs());
            }
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
            return Shecan.getPrefs().getString(PURCHASE_LINK, "https://shecan.ir/order?order=9");
        }

        public static String getDynamicBannerUrl() {
            return Shecan.getPrefs().getString(DYNAMIC_BANNER_URL, DEFAULT_DYNAMIC_BANNER_URL);
        }

        public static String getDialogMatchUrl() {
            return Shecan.getPrefs().getString(DIALOG_MATCH_URL, DEFAULT_DIALOG_MATCH_URL);
        }

        public static String getDialogDismissUrl() {
            return Shecan.getPrefs().getString(DIALOG_DISMISS_URL, DEFAULT_DIALOG_DISMISS_URL);
        }

        public static String getDialogActionUrl() {
            return Shecan.getPrefs().getString(DIALOG_ACTION_URL, DEFAULT_DIALOG_ACTION_URL);
        }

        public static String getMonitoringTargetUrl() {
            return Shecan.getPrefs().getString(MONITORING_TARGET_URL, DEFAULT_MONITORING_TARGET_URL);
        }

        public static String getMonitoringLogsUrl() {
            return Shecan.getPrefs().getString(MONITORING_LOGS_URL, DEFAULT_MONITORING_LOGS_URL);
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
