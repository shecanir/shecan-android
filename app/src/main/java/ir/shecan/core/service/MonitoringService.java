package ir.shecan.core.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.monitoring.MonitoringManager;
import ir.shecan.core.receiver.MonitoringControlReceiver;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.activity.MainActivityNew;

public class MonitoringService extends Service {
    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "shecan_control_v2";
    private static final String LEGACY_MONITORING_CHANNEL_ID = "shecan_service";
    private static final String LEGACY_DEFAULT_CHANNEL_ID = "defaultchannel";
    private static final String LEGACY_HIDDEN_CHANNEL_ID = "noIconChannel";
    private static final int LEGACY_VPN_NOTIFICATION_ID = 0;
    static final int NOTIFICATION_ID = 2001;
    private static final String ACTION_REFRESH =
            "ir.shecan.core.service.MonitoringService.ACTION_REFRESH";

    private MonitoringManager monitoringManager;

    public static void start(Context context) {
        start(context, null);
    }

    public static void refresh(Context context) {
        start(context, ACTION_REFRESH);
    }

    private static void start(Context context, String action) {
        try {
            Context applicationContext = context.getApplicationContext();
            Intent intent = new Intent(applicationContext, MonitoringService.class);
            if (action != null) intent.setAction(action);
            ContextCompat.startForegroundService(applicationContext, intent);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to start monitoring service", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
        cancelLegacyVpnNotification();
        startForeground(NOTIFICATION_ID, createNotification(this));
        monitoringManager = new MonitoringManager(getApplicationContext());
        monitoringManager.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(this));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (monitoringManager != null) {
            monitoringManager.stop();
            monitoringManager = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static Notification createNotification(Context context) {
        createNotificationChannel(context);
        Shecan app = Shecan.getInstance();
        int connectionState = app != null
                ? app.getCurrentVpnState()
                : (ShecanVpnService.isActivated()
                ? Shecan.VPN_STATE_CONNECTING
                : Shecan.VPN_STATE_DISCONNECTED);
        boolean connected = connectionState == Shecan.VPN_STATE_CONNECTED;
        boolean connecting = connectionState == Shecan.VPN_STATE_CONNECTING;
        boolean canStop = connected || connecting;
        Intent mainIntent = new Intent(context, MainActivityNew.class)
                .putExtra(MainActivityNew.LAUNCH_FRAGMENT, MainActivityNew.FRAGMENT_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingIntentFlags = immutablePendingIntentFlags();

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                mainIntent,
                pendingIntentFlags
        );

        PendingIntent togglePendingIntent = createTogglePendingIntent(
                context,
                canStop,
                pendingIntentFlags
        );

        int title = connected
                ? R.string.notification_active_title
                : connecting
                ? R.string.notification_connecting_title
                : R.string.notification_inactive_title;
        int text = connected
                ? R.string.monitoring_notification_active_text
                : connecting
                ? R.string.notification_connecting_subtitle
                : R.string.monitoring_notification_inactive_text;
        int notificationColor = connected
                ? R.color.connectionIsActiveColor
                : R.color.connectionIsReadyColor;

        RemoteViews compactView = createCompactView(
                context,
                connectionState,
                contentIntent,
                togglePendingIntent
        );
        RemoteViews expandedView = createExpandedView(
                context,
                connectionState,
                contentIntent,
                togglePendingIntent
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(title))
                .setContentText(context.getString(text))
                .setContentIntent(contentIntent)
                .setCustomContentView(compactView)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setColor(ContextCompat.getColor(context, notificationColor))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private static RemoteViews createCompactView(
            Context context,
            int connectionState,
            PendingIntent contentIntent,
            PendingIntent togglePendingIntent
    ) {
        boolean connected = connectionState == Shecan.VPN_STATE_CONNECTED;
        boolean connecting = connectionState == Shecan.VPN_STATE_CONNECTING;
        boolean canStop = connected || connecting;
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.notification_shecan_compact
        );
        views.setTextViewText(
                R.id.notification_compact_title,
                context.getString(connected
                        ? R.string.notification_active_title
                        : connecting
                        ? R.string.notification_connecting_title
                        : R.string.notification_inactive_title)
        );
        views.setTextViewText(
                R.id.notification_compact_subtitle,
                context.getString(connected
                        ? R.string.notification_active_subtitle
                        : connecting
                        ? R.string.notification_connecting_subtitle
                        : R.string.notification_inactive_subtitle)
        );
        views.setImageViewResource(
                R.id.notification_compact_mascot,
                connected ? R.drawable.ic_shecan : R.drawable.ic_shecan_normal
        );
        views.setViewVisibility(
                R.id.notification_compact_turn_on,
                canStop ? View.GONE : View.VISIBLE
        );
        views.setViewVisibility(
                R.id.notification_compact_turn_off,
                canStop ? View.VISIBLE : View.GONE
        );
        views.setOnClickPendingIntent(R.id.notification_compact_root, contentIntent);
        views.setOnClickPendingIntent(R.id.notification_compact_turn_on, togglePendingIntent);
        views.setOnClickPendingIntent(R.id.notification_compact_turn_off, togglePendingIntent);
        return views;
    }

    private static RemoteViews createExpandedView(
            Context context,
            int connectionState,
            PendingIntent contentIntent,
            PendingIntent togglePendingIntent
    ) {
        boolean connected = connectionState == Shecan.VPN_STATE_CONNECTED;
        boolean connecting = connectionState == Shecan.VPN_STATE_CONNECTING;
        boolean canStop = connected || connecting;
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.notification_shecan_expanded
        );
        views.setTextViewText(
                R.id.notification_expanded_title,
                context.getString(connected
                        ? R.string.notification_active_title
                        : connecting
                        ? R.string.notification_connecting_title
                        : R.string.notification_inactive_title)
        );
        views.setTextViewText(
                R.id.notification_expanded_subtitle,
                context.getString(connected
                        ? R.string.notification_active_subtitle
                        : connecting
                        ? R.string.notification_connecting_subtitle
                        : R.string.notification_inactive_subtitle)
        );
        views.setImageViewResource(
                R.id.notification_expanded_mascot,
                connected ? R.drawable.ic_shecan : R.drawable.ic_shecan_normal
        );
        views.setViewVisibility(
                R.id.notification_status_active,
                connected ? View.VISIBLE : View.GONE
        );
        views.setViewVisibility(
                R.id.notification_status_inactive,
                connected || connecting ? View.GONE : View.VISIBLE
        );
        views.setViewVisibility(
                R.id.notification_expanded_turn_on,
                canStop ? View.GONE : View.VISIBLE
        );
        views.setViewVisibility(
                R.id.notification_expanded_turn_off,
                canStop ? View.VISIBLE : View.GONE
        );
        views.setTextViewText(
                R.id.notification_service_name,
                getSelectedServiceName(context)
        );
        views.setOnClickPendingIntent(R.id.notification_expanded_root, contentIntent);
        views.setOnClickPendingIntent(R.id.notification_expanded_turn_on, togglePendingIntent);
        views.setOnClickPendingIntent(R.id.notification_expanded_turn_off, togglePendingIntent);
        return views;
    }

    private static String getSelectedServiceName(Context context) {
        ServiceItem selectedService = new AppStorage(context.getApplicationContext())
                .getServiceStatus(ServiceItem.class);
        if (selectedService == null) return context.getString(R.string.free);

        String serviceType = selectedService.getServiceType();
        if (serviceType == null || serviceType.trim().isEmpty()) {
            return context.getString(R.string.free);
        }
        return serviceType.trim();
    }

    private static int immutablePendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    private static PendingIntent createTogglePendingIntent(
            Context context,
            boolean canStop,
            int flags
    ) {
        if (!canStop && VpnService.prepare(context) != null) {
            Intent permissionIntent = new Intent(context, MainActivityNew.class)
                    .putExtra(
                            MainActivityNew.LAUNCH_ACTION,
                            MainActivityNew.LAUNCH_ACTION_ACTIVATE
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID + 1,
                    permissionIntent,
                    flags
            );
        }

        Intent toggleIntent = new Intent(context, MonitoringControlReceiver.class)
                .setAction(MonitoringControlReceiver.ACTION_TOGGLE);
        return PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID + 1,
                toggleIntent,
                flags
        );
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.monitoring_notification_channel),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.monitoring_notification_channel_description));
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);

        localizeLegacyChannel(
                context,
                manager,
                LEGACY_MONITORING_CHANNEL_ID,
                R.string.monitoring_notification_channel,
                R.string.monitoring_notification_channel_description
        );
        localizeLegacyChannel(
                context,
                manager,
                LEGACY_DEFAULT_CHANNEL_ID,
                R.string.notification_channel_default,
                R.string.notification_channel_default_description
        );
        localizeLegacyChannel(
                context,
                manager,
                LEGACY_HIDDEN_CHANNEL_ID,
                R.string.notification_channel_hiddenicon,
                R.string.notification_channel_hiddenicon_description
        );
    }

    private static void localizeLegacyChannel(
            Context context,
            NotificationManager manager,
            String channelId,
            int nameRes,
            int descriptionRes
    ) {
        NotificationChannel channel = manager.getNotificationChannel(channelId);
        if (channel == null) return;

        channel.setName(context.getString(nameRes));
        channel.setDescription(context.getString(descriptionRes));
        manager.createNotificationChannel(channel);
    }

    private void cancelLegacyVpnNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(LEGACY_VPN_NOTIFICATION_ID);
        }
    }
}
