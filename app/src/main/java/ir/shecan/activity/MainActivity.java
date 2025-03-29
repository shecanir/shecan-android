package ir.shecan.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;

import ir.shecan.Shecan;
import ir.shecan.R;

import ir.shecan.fragment.AboutFragment;
import ir.shecan.fragment.DNSTestFragment;
import ir.shecan.fragment.HomeFragment;
import ir.shecan.fragment.LogFragment;
import ir.shecan.fragment.SettingsFragment;
import ir.shecan.fragment.ToolbarFragment;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.Logger;
import ir.shecan.util.server.DNSServerHelper;
import ir.shecan.util.server.LocaleHelper;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "DMainActivity";

    public static final String LAUNCH_ACTION = "ir.shecan.activity.MainActivity.LAUNCH_ACTION";
    public static final int LAUNCH_ACTION_NONE = 0;
    public static final int LAUNCH_ACTION_ACTIVATE = 1;
    public static final int LAUNCH_ACTION_DEACTIVATE = 2;
    public static final int LAUNCH_ACTION_SERVICE_DONE = 3;

    public static final String LAUNCH_FRAGMENT = "ir.shecan.activity.MainActivity.LAUNCH_FRAGMENT";
    public static final int FRAGMENT_NONE = -1;
    public static final int FRAGMENT_HOME = 0;
    public static final int FRAGMENT_DNS_TEST = 1;
    public static final int FRAGMENT_SETTINGS = 2;
    public static final int FRAGMENT_ABOUT = 3;

    public static final int FRAGMENT_LOG = 6;

    public static final String LAUNCH_NEED_RECREATE = "ir.shecan.activity.MainActivity.LAUNCH_NEED_RECREATE";

    private static MainActivity instance = null;

    private ToolbarFragment currentFragment;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Shecan.getInstance().updateLocale();
        applyTheme();
        super.onCreate(savedInstanceState);

        instance = this;

        setContentView(R.layout.activity_main);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);

        Toolbar toolbar = findViewById(R.id.toolbar);

        //setSupportActionBar(toolbar); //causes toolbar issues

        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        handleIntent(getIntent());

    }


    private void switchFragment(Class fragmentClass, boolean isHome) {
        if (currentFragment == null || fragmentClass != currentFragment.getClass()) {
            try {
                ToolbarFragment fragment = (ToolbarFragment) fragmentClass.newInstance();
                FragmentManager fm = getFragmentManager();
                fm.beginTransaction().replace(R.id.id_content, fragment).commit();

                currentFragment = fragment;
            } catch (Exception e) {
                Logger.logException(e);
            }
        }

        Window window = getWindow();
        CoordinatorLayout coordinatorLayout = findViewById(R.id.id_content);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                coordinatorLayout.getLayoutParams();
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);

        if (isHome) {
            window.getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (!hasPermanentMenuKey()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            appBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);
            params.setBehavior(null);

        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(fetchPrimaryDarkColor());
            appBarLayout.setPadding(0, 0, 0, 0);
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            window.getDecorView()
                    .setFitsSystemWindows(true);
            window.getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        coordinatorLayout.requestLayout();
    }

    private boolean hasPermanentMenuKey() {
        return ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private int fetchPrimaryDarkColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!(currentFragment instanceof HomeFragment)) {
            switchFragment(HomeFragment.class, true);
            recreate();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        currentFragment = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    public void activateService() {
        Intent intent = VpnService.prepare(Shecan.getInstance());
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, Activity.RESULT_OK, null);
        }

        long activateCounter = Shecan.configurations.getActivateCounter();
        if (activateCounter == -1) {
            return;
        }
        activateCounter++;
        Shecan.configurations.setActivateCounter(activateCounter);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == Activity.RESULT_OK) {
            if (ShecanVpnService.isProMode()) {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
            } else {
                ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
                ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
            }

            Shecan.getInstance().startService(Shecan.getServiceIntent(getApplicationContext()).setAction(ShecanVpnService.ACTION_ACTIVATE));
            Shecan.updateShortcut(getApplicationContext());
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
    }


    private void handleIntent(Intent intent) {
        int launchAction = intent.getIntExtra(LAUNCH_ACTION, LAUNCH_ACTION_NONE);
        Log.d(TAG, "Updating user interface with Launch Action " + String.valueOf(launchAction));
        if (launchAction == LAUNCH_ACTION_ACTIVATE) {
            this.activateService();
        } else if (launchAction == LAUNCH_ACTION_DEACTIVATE) {
            Shecan.deactivateService(getApplicationContext());
        } else if (launchAction == LAUNCH_ACTION_SERVICE_DONE) {
            Shecan.updateShortcut(getApplicationContext());
            applyTheme();
            this.recreate();

        }

        int fragment = intent.getIntExtra(LAUNCH_FRAGMENT, FRAGMENT_NONE);

        if (intent.getBooleanExtra(LAUNCH_NEED_RECREATE, false)) {
            if (fragment != FRAGMENT_NONE)
                getIntent().putExtra(MainActivity.LAUNCH_FRAGMENT, fragment);
            recreate();
            return;
        }

        switch (fragment) {
            case FRAGMENT_ABOUT:
                switchFragment(AboutFragment.class, false);
                break;
            case FRAGMENT_DNS_TEST:
                switchFragment(DNSTestFragment.class, false);
                break;
            case FRAGMENT_HOME:
                switchFragment(HomeFragment.class, true);
                break;
            case FRAGMENT_SETTINGS:
                switchFragment(SettingsFragment.class, false);
                break;
            case FRAGMENT_LOG:
                switchFragment(LogFragment.class, false);
                break;
        }
        if (currentFragment == null) {
            switchFragment(HomeFragment.class, true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_about:
                switchFragment(AboutFragment.class, false);
                break;
            case R.id.nav_dns_test:
                switchFragment(DNSTestFragment.class, false);
                break;
            case R.id.nav_home:
                switchFragment(HomeFragment.class, true);
                break;
            case R.id.nav_settings:
                switchFragment(SettingsFragment.class, false);
                break;
            case R.id.nav_log:
                switchFragment(LogFragment.class, false);
                break;
        }

        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        InputMethodManager imm = (InputMethodManager) Shecan.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.id_content).getWindowToken(), 0);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Shecan.getInstance().updateLocale();
    }


    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    private void applyTheme() {
        int themeId = ShecanVpnService.isActivated() ? R.style.AppTheme : R.style.AppTheme_Dark;
        setTheme(themeId);
        getApplicationContext().setTheme(themeId);
    }

}
