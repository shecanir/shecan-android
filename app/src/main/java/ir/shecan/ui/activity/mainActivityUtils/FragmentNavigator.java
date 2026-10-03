package ir.shecan.ui.activity.mainActivityUtils;
import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.core.util.Logger;


/**
 * Simple helper to manage fragment transactions used by MainActivityNew.
 * Responsibility: create / add / replace fragments and return the created instance.
 *
 * Note: Caller is responsible for window/ui adjustments (status bar, appbar behaviour).
 */
public final class FragmentNavigator {

    private static final String TAG = "FragmentNavigator";

    private FragmentNavigator() {
        // utility
    }

    /**
     * Create and place the fragment.
     *
     * @param fm            FragmentManager from the activity
     * @param containerId   id of the container (e.g. R.id.id_content)
     * @param fragmentClass fragment class extending ToolbarFragment
     * @param isAdd         if true -> add (and hide existing), else -> replace
     * @return the created ToolbarFragment instance or null on failure
     */
    public static ToolbarFragment switchFragment(FragmentManager fm, int containerId,
                                                 Class<? extends ToolbarFragment> fragmentClass,
                                                 boolean isAdd) {
        if (fm == null || fragmentClass == null) {
            Log.w(TAG, "FragmentManager or fragmentClass is null");
            return null;
        }

        FragmentTransaction ft = fm.beginTransaction();

        try {
            ToolbarFragment fragment = fragmentClass.newInstance();

            if (isAdd) {
                // hide currently visible fragment if exists
                // Note: the activity is responsible for keeping a reference to currentFragment and hiding it.
                ft.add(containerId, fragment);
            } else {
                ft.replace(containerId, fragment);
            }

            ft.commitAllowingStateLoss();
            return fragment;

        } catch (Exception e) {
            Logger.logException(e);
            return null;
        }
    }
}