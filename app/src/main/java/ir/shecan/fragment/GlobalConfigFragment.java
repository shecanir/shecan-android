package ir.shecan.fragment;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;

import ir.shecan.Shecan;
import ir.shecan.R;

import ir.shecan.util.LanguageHelper;
import ir.shecan.util.server.DNSServerHelper;

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
public class GlobalConfigFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shecan.getPrefs().edit()
                .putString("primary_server", DNSServerHelper.getPrimary())
                .putString("secondary_server", DNSServerHelper.getSecondary())
                .putString("pro_primary_server", DNSServerHelper.getProPrimary())
                .putString("pro_secondary_server", DNSServerHelper.getProSecondary())
                .putString("settings_language", LanguageHelper.getLanguage())
                .apply();

        addPreferencesFromResource(R.xml.perf_settings);

        /*
        // Commented temporary (it can be used later if needed)
        ListPreference language = (ListPreference) findPreference("settings_language");
        language.setEntries(LanguageHelper.getNames());
        language.setEntryValues(LanguageHelper.getIds());
        language.setSummary(LanguageHelper.getDescription(language.getValue()));
        language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(LanguageHelper.getDescription((String) newValue));
                Shecan.changeLanguageType((String) newValue);
                getActivity().recreate();
                return true;
            }
        });
        */

        EditTextPreference logSize = (EditTextPreference) findPreference("settings_log_size");
        logSize.setSummary(logSize.getText());
        logSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
