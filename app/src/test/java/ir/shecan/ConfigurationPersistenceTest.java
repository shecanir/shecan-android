package ir.shecan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.ui.fragment.GlobalConfigFragment;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = ConfigurationPersistenceTest.TestApplication.class, sdk = 28)
public class ConfigurationPersistenceTest {
    public static class TestApplication extends Shecan {
        @Override public void onCreate() {
            ReflectionHelpers.setStaticField(Shecan.class, "instance", this);
            ReflectionHelpers.setField(this, "prefs", getSharedPreferences("app_preferences", MODE_PRIVATE));
        }
    }

    public static class SettingsActivity extends FragmentActivity {
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.AppTheme);
            super.onCreate(state);
        }
    }

    @Test
    public void typedHomePageRetainsForceUpdateAndConnectionConfiguration() {
        HomePage home = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create().fromJson("{\"version\":{\"android\":{\"current_version\":\"9.0.0\","
                        + "\"min_version\":\"8.0.0\"}},\"update_link\":\"https://example.com/update\","
                        + "\"dynamic_ip_guide_link\":\"https://example.com/guide\","
                        + "\"monitoring\":{\"target\":\"https://example.com/targets\"}}", HomePage.class);
        Shecan.ShecanInfo.saveHomePageConfig(home);
        assertEquals("9.0.0", Shecan.ShecanInfo.getCurrentVersion());
        assertEquals("8.0.0", Shecan.ShecanInfo.getMinVersion());
        assertEquals("https://example.com/update", Shecan.ShecanInfo.getUpdateLink());
        assertEquals("https://example.com/guide", Shecan.ShecanInfo.getDynamicIpGuideLink());
        assertEquals("https://example.com/targets", Shecan.ShecanInfo.getMonitoringTargetUrl());
    }

    @Test
    public void bootSwitchUsesPreferencesReadByBootReceiver() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        GlobalConfigFragment settings = new GlobalConfigFragment();
        activity.getSupportFragmentManager().beginTransaction().add(settings, "settings").commitNow();
        androidx.preference.SwitchPreferenceCompat boot = settings.findPreference("settings_boot");
        boot.setChecked(true);
        assertTrue(Shecan.getPrefs().getBoolean("settings_boot", false));
        activity.getSupportFragmentManager().beginTransaction().remove(settings).commitNow();
        activity.finish();
    }
}
