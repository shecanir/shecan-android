package ir.shecan.core.service;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import ir.shecan.R;

@RunWith(RobolectricTestRunner.class)
public class ConnectionErrorResolverTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void invalidUpdaterResponseUsesConfigurationMessage() {
        VolleyError error = httpError(403, "invalid");

        assertEquals(
                context.getString(R.string.connection_error_config_invalid),
                ConnectionErrorResolver.resolve(context, error)
        );
    }

    @Test
    public void serverFailureUsesServerMessage() {
        VolleyError error = httpError(503, "temporarily unavailable");

        assertEquals(
                context.getString(R.string.connection_error_server),
                ConnectionErrorResolver.resolve(context, error)
        );
    }

    @Test
    public void forbiddenWithoutInvalidBodyUsesActivationDeniedMessage() {
        VolleyError error = httpError(403, "forbidden");

        assertEquals(
                context.getString(R.string.connection_error_activation_denied),
                ConnectionErrorResolver.resolve(context, error)
        );
    }

    @Test
    public void timeoutUsesTimeoutMessage() {
        assertEquals(
                context.getString(R.string.connection_error_timeout),
                ConnectionErrorResolver.resolve(context, new TimeoutError())
        );
    }

    @Test
    public void unknownHostUsesDnsLookupMessage() {
        NoConnectionError error = new NoConnectionError(new UnknownHostException("ddns.shecan.ir"));

        assertEquals(
                context.getString(R.string.connection_error_dns_lookup),
                ConnectionErrorResolver.resolve(context, error)
        );
    }

    private VolleyError httpError(int statusCode, String body) {
        NetworkResponse response = new NetworkResponse(
                statusCode,
                body.getBytes(StandardCharsets.UTF_8),
                Collections.emptyMap(),
                false,
                0L
        );
        return new VolleyError(response);
    }
}
