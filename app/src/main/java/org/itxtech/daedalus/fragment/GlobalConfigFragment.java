package org.itxtech.daedalus.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.itxtech.daedalus.Daedalus;
import org.itxtech.daedalus.R;
import org.itxtech.daedalus.activity.MainActivity;
import org.itxtech.daedalus.util.LanguageHelper;
import org.itxtech.daedalus.util.server.DNSServerHelper;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class GlobalConfigFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Daedalus.getPrefs().edit()
                .putString("primary_server", DNSServerHelper.getPrimary())
                .putString("secondary_server", DNSServerHelper.getSecondary())
                .putString("settings_language", LanguageHelper.getLanguage())
                .apply();

        addPreferencesFromResource(R.xml.perf_settings);

        ListPreference primaryServer = (ListPreference) findPreference("primary_server");
        primaryServer.setEntries(DNSServerHelper.getNames(Daedalus.getInstance()));
        primaryServer.setEntryValues(DNSServerHelper.getIds());
        primaryServer.setSummary(DNSServerHelper.getDescription(primaryServer.getValue(), Daedalus.getInstance()));
        primaryServer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(DNSServerHelper.getDescription((String) newValue, Daedalus.getInstance()));
                /*Snackbar.make(getView(), R.string.notice_need_restart, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                return true;
            }
        });

        ListPreference secondaryServer = (ListPreference) findPreference("secondary_server");
        secondaryServer.setEntries(DNSServerHelper.getNames(Daedalus.getInstance()));
        secondaryServer.setEntryValues(DNSServerHelper.getIds());
        secondaryServer.setSummary(DNSServerHelper.getDescription(secondaryServer.getValue(), Daedalus.getInstance()));
        secondaryServer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(DNSServerHelper.getDescription((String) newValue, Daedalus.getInstance()));
                /*Snackbar.make(getView(), R.string.notice_need_restart, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                return true;
            }
        });

        EditTextPreference testDNSServers = (EditTextPreference) findPreference("dns_test_servers");
        testDNSServers.setSummary(testDNSServers.getText());
        testDNSServers.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });

        ListPreference language = (ListPreference) findPreference("settings_language");
        language.setEntries(LanguageHelper.getNames());
        language.setEntryValues(LanguageHelper.getIds());
        language.setSummary(LanguageHelper.getDescription(language.getValue()));
        language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(LanguageHelper.getDescription((String) newValue));
                getActivity().getIntent().putExtra(MainActivity.LAUNCH_FRAGMENT, MainActivity.FRAGMENT_SETTINGS);
                getActivity().recreate();
//                getActivity().startActivity(new Intent(Daedalus.getInstance(), MainActivity.class)
//                        .putExtra(MainActivity.LAUNCH_FRAGMENT, MainActivity.FRAGMENT_SETTINGS)
//                        .putExtra(MainActivity.LAUNCH_NEED_RECREATE, true));
                return true;
            }
        });

        EditTextPreference logSize = (EditTextPreference) findPreference("settings_log_size");
        logSize.setSummary(logSize.getText());
        logSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });

        SwitchPreference darkTheme = (SwitchPreference) findPreference("settings_dark_theme");
        darkTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                getActivity().startActivity(new Intent(Daedalus.getInstance(), MainActivity.class)
                        .putExtra(MainActivity.LAUNCH_FRAGMENT, MainActivity.FRAGMENT_SETTINGS)
                        .putExtra(MainActivity.LAUNCH_NEED_RECREATE, true));
                return true;
            }
        });
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
