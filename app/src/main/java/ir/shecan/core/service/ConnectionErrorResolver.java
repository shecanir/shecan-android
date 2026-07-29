package ir.shecan.core.service;

import android.content.Context;

import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.net.ssl.SSLException;

import ir.shecan.R;

/** Converts low-level updater errors into safe, actionable user messages. */
final class ConnectionErrorResolver {

    private ConnectionErrorResolver() {
    }

    static String resolve(Context context, VolleyError error) {
        if (context == null) return "";
        if (error == null) return context.getString(R.string.connection_error_generic);

        NetworkResponse response = error.networkResponse;
        int statusCode = response != null ? response.statusCode : -1;
        String responseBody = responseBody(response).toLowerCase(Locale.US);

        if (responseBody.contains("invalid")
                || responseBody.contains("authentication failed")) {
            return context.getString(R.string.connection_error_config_invalid);
        }
        if (statusCode == 401 || statusCode == 403) {
            return context.getString(R.string.connection_error_activation_denied);
        }
        if (statusCode == 408 || error instanceof TimeoutError) {
            return context.getString(R.string.connection_error_timeout);
        }
        if (statusCode == 429) {
            return context.getString(R.string.connection_error_too_many_requests);
        }
        if (statusCode >= 500) {
            return context.getString(R.string.connection_error_server);
        }
        if (statusCode >= 400) {
            return context.getString(R.string.connection_error_request_rejected, statusCode);
        }

        Throwable cause = rootCause(error);
        if (cause instanceof SSLException) {
            return context.getString(R.string.connection_error_secure_connection);
        }
        if (cause instanceof UnknownHostException) {
            return context.getString(R.string.connection_error_dns_lookup);
        }
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return context.getString(R.string.connection_error_no_internet);
        }
        if (error instanceof ParseError) {
            return context.getString(R.string.connection_error_invalid_response);
        }
        return context.getString(R.string.connection_error_generic);
    }

    private static String responseBody(NetworkResponse response) {
        if (response == null || response.data == null || response.data.length == 0) return "";
        return new String(response.data, StandardCharsets.UTF_8).trim();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
