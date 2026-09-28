package ir.shecan.core.monitoring;

import android.content.Context;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.MonitoringApi;
import ir.shecan.data.modelDto.monitoring.MonitoringLog;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsRequest;
import ir.shecan.data.modelDto.monitoring.MonitoringLogsResponse;
import ir.shecan.data.modelDto.monitoring.MonitoringTarget;
import ir.shecan.data.modelDto.monitoring.MonitoringTargetsResponse;

public class MonitoringManager {
    private static final String TAG = "MonitoringManager";
    private static final int DEFAULT_INTERVAL_SECONDS = 300;
    private static final int TARGET_FETCH_RETRY_SECONDS = 60;
    private static final int LOG_UPLOAD_RETRY_SECONDS = 60;
    private static final int MAX_PENDING_LOG_BATCHES = 12;
    private static final long TARGET_CACHE_TTL_MS = 15 * 60 * 1000L;

    private static volatile List<MonitoringTarget> cachedTargets = new ArrayList<>();
    private static volatile int cachedIntervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private static volatile long cachedTargetsAtMs = 0L;

    private final Context context;
    private final MonitoringApi api;
    private final MonitoringConnectivity connectivity;
    private final MonitoringIdentity identity;
    private final MonitoringChecks checks;
    private final AtomicBoolean runningChecks = new AtomicBoolean(false);
    private final Deque<List<MonitoringLog>> pendingLogBatches = new ArrayDeque<>();

    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService logUploadRetryScheduler;
    private volatile List<MonitoringTarget> targets = new ArrayList<>();
    private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private volatile boolean active;
    private boolean uploadingLogs;

    public MonitoringManager(Context context) {
        this.context = context.getApplicationContext();
        this.api = new MonitoringApi(this.context);
        this.connectivity = new MonitoringConnectivity(this.context);
        this.identity = new MonitoringIdentity(this.context);
        this.checks = new MonitoringChecks(connectivity, identity);
    }

    public synchronized void start() {
        if (active) return;

        active = true;
        if (hasFreshCachedTargets()) {
            targets = cachedTargets;
            intervalSeconds = cachedIntervalSeconds;
            scheduleLoop();
            return;
        }
        fetchTargets();
    }

    public synchronized void stop() {
        active = false;
        runningChecks.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (logUploadRetryScheduler != null) {
            logUploadRetryScheduler.shutdownNow();
            logUploadRetryScheduler = null;
        }
        pendingLogBatches.clear();
        uploadingLogs = false;
    }

    private void fetchTargets() {
        if (!active) return;
        if (!connectivity.isOnline()) {
            scheduleTargetFetchRetry();
            return;
        }

        api.targets(new ApiCallback<MonitoringTargetsResponse>() {
            @Override
            public void onSuccess(MonitoringTargetsResponse response, boolean fromCache) {
                if (!active || response == null) return;
                if (!identity.isInSample(response.getSamplingPercent())) {
                    disableBySampling();
                    return;
                }
                targets = response.getTargets();
                intervalSeconds = Math.max(60, response.getIntervalSeconds());
                cachedTargets = targets;
                cachedIntervalSeconds = intervalSeconds;
                cachedTargetsAtMs = System.currentTimeMillis();
                scheduleLoop();
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.d(TAG, "Monitoring targets unavailable: " + statusCode + " " + message);
                scheduleTargetFetchRetry();
            }
        });
    }

    private synchronized void disableBySampling() {
        active = false;
        targets = new ArrayList<>();
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        Log.d(TAG, "Monitoring disabled for this device by sampling");
    }

    private boolean hasFreshCachedTargets() {
        return !cachedTargets.isEmpty()
                && System.currentTimeMillis() - cachedTargetsAtMs < TARGET_CACHE_TTL_MS;
    }

    private synchronized void scheduleLoop() {
        if (!active || targets.isEmpty()) return;

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(
                this::runOnceSafely,
                // Targets are ready, so run the first monitoring batch immediately.
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    private synchronized void scheduleTargetFetchRetry() {
        if (!active) return;

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(this::fetchTargets, TARGET_FETCH_RETRY_SECONDS, TimeUnit.SECONDS);
    }

    private void runOnceSafely() {
        if (!active || !connectivity.isOnline()) return;
        if (!runningChecks.compareAndSet(false, true)) return;

        try {
            if (!hasFreshCachedTargets()) {
                fetchTargets();
                return;
            }
            List<MonitoringLog> logs = new ArrayList<>();
            for (MonitoringTarget target : targets) {
                if (!active) return;
                if (target == null || target.getType() == null) continue;
                logs.add(checks.run(target));
            }
            if (!logs.isEmpty()) enqueueBatch(logs);
        } catch (Exception e) {
            Log.e(TAG, "Monitoring run failed", e);
        } finally {
            runningChecks.set(false);
        }
    }

    private synchronized void enqueueBatch(List<MonitoringLog> logs) {
        if (pendingLogBatches.size() >= MAX_PENDING_LOG_BATCHES) {
            pendingLogBatches.removeFirst();
        }
        pendingLogBatches.addLast(new ArrayList<>(logs));
        flushPendingLogs();
    }

    private synchronized void flushPendingLogs() {
        if (!active || uploadingLogs || pendingLogBatches.isEmpty()) return;
        if (!connectivity.isOnline()) {
            scheduleLogUploadRetry();
            return;
        }

        uploadingLogs = true;
        List<MonitoringLog> logs = pendingLogBatches.peekFirst();
        MonitoringLogsRequest request = new MonitoringLogsRequest(
                identity.hashedDeviceId(),
                identity.appVersion(),
                "android",
                connectivity.networkInfo(),
                logs
        );

        api.sendLogs(request, new ApiCallback<MonitoringLogsResponse>() {
            @Override
            public void onSuccess(MonitoringLogsResponse response, boolean fromCache) {
                Log.d(TAG, "Monitoring logs sent");
                synchronized (MonitoringManager.this) {
                    pendingLogBatches.pollFirst();
                    uploadingLogs = false;
                    cancelLogUploadRetry();
                    flushPendingLogs();
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.d(TAG, "Monitoring logs rejected: " + statusCode + " " + message);
                synchronized (MonitoringManager.this) {
                    uploadingLogs = false;
                    scheduleLogUploadRetry();
                }
            }
        });
    }

    private synchronized void scheduleLogUploadRetry() {
        if (!active || pendingLogBatches.isEmpty()) return;
        if (logUploadRetryScheduler != null && !logUploadRetryScheduler.isShutdown()) return;

        logUploadRetryScheduler = Executors.newSingleThreadScheduledExecutor();
        logUploadRetryScheduler.schedule(() -> {
            synchronized (MonitoringManager.this) {
                logUploadRetryScheduler = null;
                flushPendingLogs();
            }
        }, LOG_UPLOAD_RETRY_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void cancelLogUploadRetry() {
        if (logUploadRetryScheduler != null) {
            logUploadRetryScheduler.shutdownNow();
            logUploadRetryScheduler = null;
        }
    }
}
