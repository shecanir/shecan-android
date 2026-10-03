package ir.shecan.ui.fragment;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.util.LanguageHelper;
import ir.shecan.core.util.server.DNSServerHelper;

public class GlobalConfigFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.perf_settings, rootKey);

        Shecan.getPrefs().edit()
                .putString("primary_server", DNSServerHelper.getPrimary())
                .putString("secondary_server", DNSServerHelper.getSecondary())
                .putString("pro_primary_server", DNSServerHelper.getProPrimary())
                .putString("pro_secondary_server", DNSServerHelper.getProSecondary())
                .putString("settings_language", LanguageHelper.getLanguage())
                .apply();

        EditTextPreference logSize = findPreference("settings_log_size");
        if (logSize != null) {
            logSize.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            logSize.setOnPreferenceChangeListener((preference, newValue) -> {
                return true;
            });
        }

        /*
        // اگر خواستی زبان را هم فعال کنی:
        ListPreference language = findPreference("settings_language");
        if (language != null) {
            language.setEntries(LanguageHelper.getNames());
            language.setEntryValues(LanguageHelper.getIds());
            language.setSummary(LanguageHelper.getDescription(language.getValue()));
            language.setOnPreferenceChangeListener((preference, newValue) -> {
                preference.setSummary(LanguageHelper.getDescription((String) newValue));
                Shecan.changeLanguageType((String) newValue);
                requireActivity().recreate();
                return true;
            });
        }
        */
    }
}
