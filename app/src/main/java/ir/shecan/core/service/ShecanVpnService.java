package ir.shecan.core.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.monitoring.MonitoringManager;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.fragment.DNSQuery;
import ir.shecan.core.provider.Provider;
import ir.shecan.core.provider.TcpProvider;
import ir.shecan.core.provider.UdpProvider;
import ir.shecan.core.receiver.StatusBarBroadcastReceiver;
import ir.shecan.core.util.Logger;
import ir.shecan.core.util.server.AbstractDNSServer;
import ir.shecan.core.util.server.OkHttpLogger;

/**
 * Fixed and hardened ShecanVpnService
 */
public class ShecanVpnService extends VpnService implements Runnable {
    public static final String ACTION_ACTIVATE = "ir.shecan.core.service.ShecanVpnService.ACTION_ACTIVATE";
    public static final String ACTION_DEACTIVATE = "ir.shecan.core.service.ShecanVpnService.ACTION_DEACTIVATE";

    public static final String IS_PRO_MODE = "IS_PRO_MODE";
    public static final String IS_DYNAMIC_IP_MODE = "IS_DYNAMIC_IP_MODE";
    public static final String UPDATER_LINK = "UPDATER_LINK";
    public static final String DYNAMIC_IP = "DYNAMIC_IP";

    private static final String ConnectionStatusRequest = "connection_status_request";
    private static final String CoreApiRequest = "core_api_request";

    private static final int NOTIFICATION_ACTIVATED = 0;

    private static final String TAG = "ShecanVpnService";

    public static AbstractDNSServer primaryServer;
    public static AbstractDNSServer secondaryServer;

    private NotificationCompat.Builder notification = null;

    private volatile boolean running = false;
    private long lastUpdate = 0;
    private boolean statisticQuery;
    private Provider provider;
    private ParcelFileDescriptor descriptor;
    private MonitoringManager monitoringManager;

    private Thread mThread = null;

    public HashMap<String, Pair<String, Integer>> dnsServers;

    private long sessionStartTime = 0L;

    private static boolean activated = false;

    public static boolean isActivated() {
        return activated;
    }

    public static boolean isProMode() {
        return Shecan.getPrefs().getBoolean(IS_PRO_MODE, false);
    }

    public static boolean isDynamicIPMode() {
        return Shecan.getPrefs().getBoolean(IS_DYNAMIC_IP_MODE, true);
    }

    public static String getUpdaterLink() {
        return Shecan.getPrefs().getString(UPDATER_LINK, "");
    }

    public static String getDynamicIp() {
        return Shecan.getPrefs().getString(DYNAMIC_IP, "");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        monitoringManager = new MonitoringManager(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_ACTIVATE:
                    activated = true;

                    Context applicationContext = getApplicationContext();
                    if (Shecan.getPrefs().getBoolean("settings_notification", true)) {

                        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

                        String channelId = createNotificationChannel(false);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

                        Intent mainIntent = new Intent(this, MainActivityNew.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        int pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            pendingIntentFlag |= PendingIntent.FLAG_IMMUTABLE;
                        }

                        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, pendingIntentFlag);

                        Intent deactivateIntent = new Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION);
                        Intent settingIntent = new Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION);


                        deactivateIntent.setClass(applicationContext, StatusBarBroadcastReceiver.class);
                        settingIntent.setClass(applicationContext, StatusBarBroadcastReceiver.class);

                        int broadcastFlag = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            broadcastFlag = PendingIntent.FLAG_MUTABLE;
                        }

                        builder.setWhen(0)
                                .setContentTitle(getResources().getString(R.string.notice_activated))
                                .setSmallIcon(R.drawable.ic_notification)
                                .setColor(getResources().getColor(R.color.colorPrimary)) // backward compatibility
                                .setAutoCancel(false)
                                .setOngoing(true)
                                .setTicker(getResources().getString(R.string.notice_activated))
                                .setContentIntent(pendingIntent)
                                .addAction(R.drawable.ic_clear, getResources().getString(R.string.button_text_deactivate),
                                        PendingIntent.getBroadcast(this, 0, deactivateIntent, broadcastFlag))
                                .addAction(R.drawable.ic_settings, getResources().getString(R.string.action_settings),
                                        PendingIntent.getBroadcast(this, 0, settingIntent, broadcastFlag));

                        Notification notificationBuilt = builder.build();

                        manager.notify(NOTIFICATION_ACTIVATED, notificationBuilt);

                        this.notification = builder;
                    }

                    if (this.mThread == null) {
                        this.mThread = new Thread(this, "ShecanVpn");
                        this.running = true;
                        this.mThread.start();
                    }
                    Shecan.updateShortcut(applicationContext);

                    return START_STICKY;
                case ACTION_DEACTIVATE:
                    stopThread();
                    return START_NOT_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    private List<Pair<String, Integer>> getResolvedDNS(AbstractDNSServer dnsServer) {
        List<Pair<String, Integer>> resolvedDNSServers = new ArrayList<>();
        if (dnsServer == null || dnsServer.getAddress() == null || dnsServer.getAddress().trim().isEmpty()) {
            return resolvedDNSServers;
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(dnsServer.getAddress());
            for (InetAddress address : addresses) {
                if (checkDNSServer(address, dnsServer.getPort()))
                    resolvedDNSServers.add(new Pair<>(address.getHostAddress(), dnsServer.getPort()));
            }
        } catch (UnknownHostException e) {
            Log.w(TAG, "Unable to resolve DNS server " + dnsServer.getAddress());
        }

        return resolvedDNSServers;
    }

    private boolean checkDNSServer(InetAddress address, int port) {

        DNSMessage.Builder message = DNSMessage.builder()
                .addQuestion(new Question(Shecan.DEFAULT_TEST_DOMAINS[0], Record.TYPE.A))
                .setId((new Random()).nextInt())
                .setRecursionDesired(true)
                .setOpcode(DNSMessage.OPCODE.QUERY)
                .setResponseCode(DNSMessage.RESPONSE_CODE.NO_ERROR)
                .setQrFlag(false);
        try {
            // add small retry loop to be resilient to transient network failures
            for (int i = 0; i < 3; i++) {
                try {
                    DNSMessage response = new DNSQuery().query(message.build(), address, port);
                    if (response != null && response.answerSection != null && !response.answerSection.isEmpty()) {
                        return true;
                    }
                } catch (IOException ignored) {
                    // retry
                    if (i == 2) return false;
                    Thread.sleep(100);
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void onDestroy() {
        stopThread();
    }

    private void stopThread() {
        Log.d(TAG, "stopThread");
        activated = false;
        if (monitoringManager != null) {
            monitoringManager.stop();
        }

        // ===== SAVE SESSION DURATION =====
        if (sessionStartTime > 0) {
            long duration = System.currentTimeMillis() - sessionStartTime;

            // save to SharedPreferences
            long current = Shecan.getPrefs().getLong("weekly_connection_time", 0L);
            Shecan.getPrefs().edit()
                    .putLong("weekly_connection_time", current + duration)
                    .apply();

            sessionStartTime = 0L;
        }
        // ===== END SAVE =====

        boolean shouldRefresh = false;
        try {
            // Take a snapshot to avoid races (mThread might change concurrently)
            Thread t = mThread;
            if (t != null) {
                // mark running false so provider loops see it
                running = false;
                shouldRefresh = true;

                // stop provider first (if exists)
                if (provider != null) {
                    try {
                        provider.stop();
                    } catch (Exception ex) {
                        Logger.logException(ex);
                    }
                    try {
                        provider.shutdown();
                    } catch (Exception ex) {
                        Logger.logException(ex);
                    }
                    // optional: set provider = null; // if you want to free reference
                }

                // Only interrupt/join if we're NOT the same thread (avoid self-join deadlock)
                if (t != Thread.currentThread()) {
                    try {
                        if (t.isAlive()) {
                            t.interrupt();
                            try {
                                t.join(2000);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    } catch (Exception ex) {
                        Logger.logException(ex);
                    }
                } else {
                    // We're being called from inside the worker thread itself (no join)
                    Log.d(TAG, "stopThread called from the worker thread; skipping join to avoid deadlock");
                }

                // Clear the reference AFTER we've handled the thread (use compare-and-set style)
                if (mThread == t) {
                    mThread = null;
                } else {
                    // another thread replaced it meanwhile; still safe to set to null to avoid leaks
                    mThread = null;
                }
            }

            if (this.descriptor != null) {
                try {
                    this.descriptor.close();
                } catch (IOException ignored) {
                }
                this.descriptor = null;
            }

            if (notification != null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ACTIVATED);
                notification = null;
            }
            dnsServers = null;
        } catch (Exception e) {
            Logger.logException(e);
        }
        // stop the service (safe to call from background thread)
        try {
            stopSelf();
        } catch (Exception ignored) {
        }

        if (shouldRefresh) {
            ((Shecan) getApplicationContext()).getVpnState().postValue(0);
            Shecan.updateShortcut(getApplicationContext());
            Logger.info("shecan service has stopped");
        }
    }

    @Override
    public void onRevoke() {
        stopThread();
    }

    private InetAddress addDnsServer(Builder builder, String format, byte[] ipv6Template, Pair<String, Integer> destination) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(destination.first);
        int size = (dnsServers != null) ? dnsServers.size() : 0;
        size++;
        if (address instanceof Inet6Address && ipv6Template == null) {
            Log.i(TAG, "addDnsServer: Ignoring DNS server " + address);
        } else if (address instanceof Inet4Address) {
            String alias = String.format(Locale.US, format, size + 1);
            dnsServers.put(alias, destination);
            // Do NOT add route per-dns alias here — keep routing simple. The alias is used only as a local virtual address.
            // builder.addRoute(alias, 32); // removed to avoid incorrect routing
            return InetAddress.getByName(alias);
        } else if (address instanceof Inet6Address) {
            ipv6Template[ipv6Template.length - 1] = (byte) (size + 1);
            InetAddress i6addr = Inet6Address.getByAddress(ipv6Template);
            dnsServers.put(i6addr.getHostAddress(), destination);
            return i6addr;
        }
        return null;
    }

    @Override
    public void run() {
        try {
            List<Pair<String, Integer>> resolvedDNS = new ArrayList<>();
            if (primaryServer != null) resolvedDNS.addAll(getResolvedDNS(primaryServer));
            if (secondaryServer != null) resolvedDNS.addAll(getResolvedDNS(secondaryServer));

            if (resolvedDNS.isEmpty()) {
                Log.d(TAG, "No DNS server is reachable.");
                ((Shecan) getApplicationContext()).getVpnStatus().postValue("اتصال ناموفق بود، مجددا تلاش کنید.");
                stopThread();
                return;
            }

            Builder builder = new Builder()
                    .setSession("shecan");

            // Configure intent: use proper flags depending on API
            Intent configIntent = new Intent(this, MainActivityNew.class).putExtra(MainActivityNew.LAUNCH_FRAGMENT, MainActivityNew.FRAGMENT_SETTINGS);
            int configPendingFlags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                configPendingFlags |= PendingIntent.FLAG_IMMUTABLE;

            builder.setConfigureIntent(PendingIntent.getActivity(this, 0, configIntent, configPendingFlags));

            String format = null;
            for (String prefix : new String[]{"10.0.0", "192.0.2", "198.51.100", "203.0.113", "192.168.50"}) {
                try {
                    builder.addAddress(prefix + ".1", 24);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                format = prefix + ".%d";
                break;
            }

            boolean advanced = true; // feature flag - kept true for advanced behavior

            statisticQuery = Shecan.getPrefs().getBoolean("settings_count_query_times", false);
            byte[] ipv6Template = new byte[]{32, 1, 13, (byte) (184 & 0xFF), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            boolean hasIPv6 = false;

            for (Pair<String, Integer> pair : resolvedDNS) {
                if (pair.first.contains(":")) {
                    hasIPv6 = true;
                    break;
                }
            }

            if (hasIPv6) {//IPv6
                try {
                    InetAddress intentAddress = Inet6Address.getByAddress(ipv6Template);
                    Log.d(TAG, "configure: Adding IPv6 address" + intentAddress);
                    builder.addAddress(intentAddress, 120);
                } catch (Exception e) {
                    Logger.logException(e);
                    ipv6Template = null;
                }
            } else {
                ipv6Template = null;
            }

            InetAddress alias;

            dnsServers = new HashMap<>();

            for (Pair<String, Integer> pair : resolvedDNS) {
                alias = addDnsServer(builder, format, ipv6Template, pair);

                Logger.info("shecan is listening on " + pair.first + ":" + pair.second + " as " + alias);
                if (alias != null) builder.addDnsServer(alias);
            }

            builder.setBlocking(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.allowFamily(OsConstants.AF_INET);
                builder.allowFamily(OsConstants.AF_INET6);
            }

            descriptor = builder.establish();
            if (descriptor == null) {
                Log.e(TAG, "Failed to establish VPN interface (user likely denied permission)");
                stopThread();
                return;
            }

            Logger.info("shecan service is started");
            ((Shecan) getApplicationContext()).getVpnState().postValue(2);
            if (monitoringManager != null) {
                monitoringManager.start();
            }

            // ===== START SESSION TRACKING =====
            sessionStartTime = System.currentTimeMillis();
            // ===== END SESSION TRACKING =====

            if (Shecan.getPrefs().getBoolean("settings_dns_over_tcp", false)) {
                provider = new TcpProvider(descriptor, this);
            } else {
                provider = new UdpProvider(descriptor, this);
            }
            provider.start();
            provider.process();
        } catch (Exception e) {
            Logger.logException(e);
        } finally {
            Log.d(TAG, "quit");
            stopThread();
        }
    }

    public void providerLoopCallback() {
        if (statisticQuery) {
            updateUserInterface();
        }
    }

    private void updateUserInterface() {
        long time = System.currentTimeMillis();
        if (time - lastUpdate >= 1000) {
            lastUpdate = time;
            if (notification != null && provider != null) {
                try {
                    long queries = provider.getDnsQueryTimes();
                    notification.setContentTitle(getResources().getString(R.string.notice_queries) + " " + queries);
                    NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null)
                        manager.notify(NOTIFICATION_ACTIVATED, notification.build());
                } catch (Exception e) {
                    Logger.logException(e);
                }
            }
        }
    }


    public static class VpnNetworkException extends Exception {
        public VpnNetworkException(String s) {
            super(s);
        }

        public VpnNetworkException(String s, Throwable t) {
            super(s, t);
        }

    }

    public String createNotificationChannel(boolean allowHiding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return "defaultchannel";

            if (allowHiding && Shecan.getPrefs().getBoolean("hide_notification_icon", false)) {
                String id = "noIconChannel";
                if (notificationManager.getNotificationChannel(id) == null) {
                    NotificationChannel channel = new NotificationChannel(id, getString(R.string.notification_channel_hiddenicon), NotificationManager.IMPORTANCE_MIN);
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setDescription(getString(R.string.notification_channel_hiddenicon_description));
                    notificationManager.createNotificationChannel(channel);
                }
                return id;
            } else {
                String id = "defaultchannel";
                if (notificationManager.getNotificationChannel(id) == null) {
                    NotificationChannel channel = new NotificationChannel(id, getString(R.string.notification_channel_default), NotificationManager.IMPORTANCE_LOW);
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setDescription(getString(R.string.notification_channel_default_description));
                    notificationManager.createNotificationChannel(channel);
                }
                return id;
            }
        } else {
            return "defaultchannel";
        }
    }

    public static void callCoreAPI(final Context context, final CoreApiResponseListener listener) {
        String apiUrl = ShecanVpnService.getUpdaterLink();
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);

        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                apiUrl,
                response -> {
                    String result = (response != null) ? response.trim() : "";
                    switch (result) {
                        case "invalid":
                            if (listener != null) listener.onInvalid();
                            break;

                        case "in the range":
                            if (listener != null) listener.onInTheRange();
                            break;

                        case "out of the range":
                            if (listener != null) listener.onOutOfRange();
                            break;

                        default:
                            Shecan.setDynamicIP(result.trim());
                            ((Shecan) context.getApplicationContext())
                                    .getProActivatedEvent()
                                    .postValue(true);

                            break;
                    }
                },
                error -> {
                    if (listener != null) {
                        Log.d("Apizzz", error.toString());
                        listener.onError(error.toString());
                    }
                }
        );

        stringRequest.setTag(CoreApiRequest);
        requestQueue.add(stringRequest);
    }

    public static void callConnectionStatusAPI(Context context, final ConnectionStatusApiListener listener, Integer timeoutMs) {
        OkHttpLogger.requestWithIPLogging("https://check.shecan.ir");
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);
        StringRequest stringRequest = getStringRequest(listener);

        int finalTimeout = (timeoutMs != null) ? timeoutMs : DefaultRetryPolicy.DEFAULT_TIMEOUT_MS;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                finalTimeout,  // Timeout in milliseconds
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,  // Number of retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT  // Backoff multiplier
        ));

        stringRequest.setTag(ConnectionStatusRequest);
        requestQueue.add(stringRequest);
    }

    @NonNull
    private static StringRequest getStringRequest(ConnectionStatusApiListener listener) {
        String apiUrl = "https://check.shecan.ir";
        // show the cached connected IP connected before the api call, when gets error
        return new StringRequest(
                Request.Method.GET,
                apiUrl,
                response -> {
                    String result = (response != null) ? response.trim() : "";
                    if (result.equals("2")) {
                        listener.onConnected();
                    } else {
                        listener.onRetry();
                    }
                },
                error -> {
                    if (ShecanVpnService.isActivated()) {
                        Logger.error("Connecting to: " + apiUrl + " Resolved IP: " + OkHttpLogger.resolvedIp + " is Failed");
                    }
                    listener.onRetry();
                }
        );
    }

    public static void cancelConnectionStatusAPI(Context context) {
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);
        requestQueue.cancelAll(ConnectionStatusRequest);
    }

    public static void cancelCoreAPI(Context context) {
        RequestQueue requestQueue = VolleyHelper.getSecureRequestQueue(context);
        requestQueue.cancelAll(CoreApiRequest);
    }
}
