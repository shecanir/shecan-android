package ir.shecan.ui.activity.mainActivityUtils;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import ir.shecan.data.modelDto.AppConfig;
import ir.shecan.data.storage.AppStorage;

/**
 * Helper class to manage App theme based on AppConfig.
 */
public class ThemeManager {

    private final Context context;
    private final AppStorage appStorage;
    private int currentMode = AppCompatDelegate.MODE_NIGHT_NO;
    private boolean recreatedForUiModeSync;

    public ThemeManager(@NonNull Context context) {
        this.context = context;
        this.appStorage = new AppStorage(context.getApplicationContext());
        loadTheme();
    }

    private void loadTheme() {
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if (appConfig != null) {
            currentMode = appConfig.getMode();
            AppCompatDelegate.setDefaultNightMode(currentMode);
        } else {
            appStorage.saveAppConfig(new AppConfig(currentMode));
        }
    }

    /**
     * Apply theme (used on recreate or startup)
     */
    public void applyTheme() {
        loadTheme();
    }

    /**
     * Call this in Activity's onResume to check if theme has changed.
     * Returns true if recreate is needed
     */
    public boolean handleOnResume() {
        AppConfig appConfig = appStorage.getAppConfig(AppConfig.class);
        if (appConfig != null) {
            int mode = appConfig.getMode();
            boolean delegateDrifted = AppCompatDelegate.getDefaultNightMode() != mode;
            if (delegateDrifted) {
                AppCompatDelegate.setDefaultNightMode(mode);
            }

            boolean modeChanged = currentMode != mode;
            boolean uiModeDrifted = isForcedModeOutOfSync(mode);
            if (modeChanged || (uiModeDrifted && !recreatedForUiModeSync)) {
                currentMode = mode;
                recreatedForUiModeSync = uiModeDrifted;
                return true; // needs recreate
            }

            if (!uiModeDrifted) {
                recreatedForUiModeSync = false;
            }
        }
        return false;
    }

    private boolean isForcedModeOutOfSync(int mode) {
        if (mode != AppCompatDelegate.MODE_NIGHT_NO && mode != AppCompatDelegate.MODE_NIGHT_YES) {
            return false;
        }

        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = nightMode == Configuration.UI_MODE_NIGHT_YES;
        return mode == AppCompatDelegate.MODE_NIGHT_NO ? isNight : !isNight;
    }
}
