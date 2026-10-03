package ir.shecan.core.monitoring;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicInteger;

public final class MonitoringAppState {
    private static final AtomicInteger STARTED_ACTIVITIES = new AtomicInteger(0);
    private static boolean initialized;

    private MonitoringAppState() {
    }

    public static synchronized void initialize(Application application) {
        if (initialized) return;
        initialized = true;

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                STARTED_ACTIVITIES.incrementAndGet();
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                STARTED_ACTIVITIES.updateAndGet(value -> Math.max(0, value - 1));
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    static boolean isAppActive() {
        return STARTED_ACTIVITIES.get() > 0;
    }
}
