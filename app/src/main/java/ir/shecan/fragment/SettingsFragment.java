package ir.shecan.fragment;

import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ir.shecan.R;

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
public class SettingsFragment extends ToolbarFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm;
        fm = getChildFragmentManager();
        fm.beginTransaction().replace(R.id.settings_content, new GlobalConfigFragment()).commit();
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_settings).setChecked(true);
        toolbar.setTitle(R.string.action_settings);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
