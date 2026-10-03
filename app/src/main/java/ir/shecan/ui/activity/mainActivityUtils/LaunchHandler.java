package ir.shecan.ui.activity.mainActivityUtils;

import android.content.Intent;

import ir.shecan.Shecan;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.fragment.AboutFragment;
import ir.shecan.ui.fragment.DNSTestFragment;
import ir.shecan.ui.fragment.LogFragment;
import ir.shecan.ui.fragment.SettingsFragment;
import ir.shecan.ui.fragment.refactor.HomeFragment;

public final class LaunchHandler {

    private static final String TAG = "LaunchHandler";

    private LaunchHandler() {
        // Utility class
    }

    /**
     * Main entry to handle all launch logic from shortcuts, notifications, or external calls.
     */
    public static void handle(MainActivityNew activity, Intent intent) {
        if (activity == null || intent == null) return;

        int launchAction = intent.getIntExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_NONE);
        int launchFragment = intent.getIntExtra(MainActivityNew.LAUNCH_FRAGMENT, MainActivityNew.FRAGMENT_NONE);
        int launchTab = intent.getIntExtra(MainActivityNew.LAUNCH_TAB, MainActivityNew.FRAGMENT_NONE);
        boolean needRecreate = intent.getBooleanExtra(MainActivityNew.LAUNCH_NEED_RECREATE, false);

        // ---- Step 1: Handle Action ----
        handleLaunchAction(activity, launchAction);

        if (launchTab != MainActivityNew.FRAGMENT_NONE) {
            activity.selectMainTab(launchTab, false);
            return;
        }

        // ---- Step 2: handle recreate request ----
        if (needRecreate) {
            if (launchFragment != MainActivityNew.FRAGMENT_NONE) {
                // Keep the fragment for after recreate
                activity.getIntent().putExtra(MainActivityNew.LAUNCH_FRAGMENT, launchFragment);
            }
            activity.recreate();
            return;
        }

        // ---- Step 3: Handle fragment selection ----
        handleLaunchFragment(activity, launchFragment);
    }



    private static void handleLaunchAction(MainActivityNew activity, int action) {

        switch (action) {

            case MainActivityNew.LAUNCH_ACTION_ACTIVATE:
                activity.activateService();
                break;

            case MainActivityNew.LAUNCH_ACTION_DEACTIVATE:
                Shecan.deactivateService(activity.getApplicationContext());
                break;

            case MainActivityNew.LAUNCH_ACTION_SERVICE_DONE:
                Shecan.updateShortcut(activity.getApplicationContext());
                break;

            case MainActivityNew.LAUNCH_ACTION_NONE:
            default:
                // nothing
                break;
        }
    }


    private static void handleLaunchFragment(MainActivityNew activity, int fragment) {

        switch (fragment) {
            case MainActivityNew.FRAGMENT_ABOUT:
                activity.switchFragment(AboutFragment.class, true);
                break;

            case MainActivityNew.FRAGMENT_DNS_TEST:
                activity.switchFragment(DNSTestFragment.class, true);
                break;

            case MainActivityNew.FRAGMENT_HOME:
                activity.switchFragment(HomeFragment.class, true);
                break;

            case MainActivityNew.FRAGMENT_SETTINGS:
                activity.switchFragment(SettingsFragment.class, true);
                break;

            case MainActivityNew.FRAGMENT_LOG:
                activity.switchFragment(LogFragment.class, true);
                break;

            case MainActivityNew.FRAGMENT_NONE:
            default:
                break;
        }

        // Default fallback if for any reason no fragment is active
        if (activity.getCurrentFragment() == null) {
            activity.switchFragment(HomeFragment.class, true);
        }
    }
}
