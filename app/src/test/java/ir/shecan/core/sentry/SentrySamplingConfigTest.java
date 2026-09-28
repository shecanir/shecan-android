package ir.shecan.core.sentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.sentry.SentryOptions;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SentrySamplingConfigTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(SentrySamplingConfig.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void clampsRemotePercentToSupportedRange() {
        assertEquals(0, SentrySamplingConfig.clampPercent(-1));
        assertEquals(25, SentrySamplingConfig.clampPercent(25));
        assertEquals(100, SentrySamplingConfig.clampPercent(101));
    }

    @Test
    public void convertsPercentToSentryRate() {
        assertEquals(0.0, SentrySamplingConfig.toSdkRate(0), 0.0);
        assertEquals(0.01, SentrySamplingConfig.toSdkRate(1), 0.0);
        assertEquals(1.0, SentrySamplingConfig.toSdkRate(100), 0.0);
    }

    @Test
    public void readsAndroidSampleRateBeforeLegacyValue() throws Exception {
        JSONObject homePage = new JSONObject("{"
                + "\"sentry\":{\"android\":{\"sample_rate\":100}},"
                + "\"sentry_sample_rate\":1"
                + "}");

        assertEquals(Integer.valueOf(100), SentrySamplingConfig.readAndroidSampleRate(homePage));
    }

    @Test
    public void readsLegacySampleRateWhenAndroidConfigIsMissing() throws Exception {
        JSONObject homePage = new JSONObject("{\"sentry_sample_rate\":\"25\"}");

        assertEquals(Integer.valueOf(25), SentrySamplingConfig.readAndroidSampleRate(homePage));
    }

    @Test
    public void keepsManifestRateWhenNoRemoteValueIsCached() {
        SentryOptions options = new SentryOptions();
        options.setTracesSampleRate(0.01);

        SentrySamplingConfig.applyCached(context, options);

        assertEquals(0.01, options.getTracesSampleRate(), 0.0);
    }

    @Test
    public void cachedRemotePercentOverridesManifestRate() {
        context.getSharedPreferences(SentrySamplingConfig.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(SentrySamplingConfig.SAMPLE_RATE_PERCENT_KEY, 100)
                .commit();
        SentryOptions options = new SentryOptions();
        options.setTracesSampleRate(0.01);

        SentrySamplingConfig.applyCached(context, options);

        assertEquals(1.0, options.getTracesSampleRate(), 0.0);
    }

    @Test
    public void missingRemoteValueClearsCachedOverride() {
        SentryOptions options = new SentryOptions();
        options.setTracesSampleRate(0.01);
        SentrySamplingConfig.applyCached(context, options);
        SentrySamplingConfig.updateFromRemote(context, 100);

        SentrySamplingConfig.updateFromRemote(context, null);

        assertFalse(context.getSharedPreferences(
                SentrySamplingConfig.PREFERENCES_NAME,
                Context.MODE_PRIVATE
        ).contains(SentrySamplingConfig.SAMPLE_RATE_PERCENT_KEY));
    }
}
