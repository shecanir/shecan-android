package ir.shecan.data.api;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ir.shecan.BuildConfig;
import ir.shecan.data.modelDto.EmptyResponse;

public class APIManager {

    private static final String TAG = "APIManager";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String CONTENT_TYPE_RAW_FORM = "application/x-www-form-urlencoded";
    private static final String REDACTED = "******";
    private static final int IAP_VERIFY_TIMEOUT_MS = 60000;
    private static final Pattern META_REFRESH_URL_PATTERN = Pattern.compile("(?is)<meta[^>]+http-equiv\\s*=\\s*['\"]?refresh['\"]?[^>]+content\\s*=\\s*['\"][^'\"]*url\\s*=\\s*([^'\"]+)['\"]");
    private static final Pattern FORM_ACTION_PATTERN = Pattern.compile("(?is)<form[^>]+action\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern WINDOW_LOCATION_PATTERN = Pattern.compile("(?is)(?:window\\.location\\.replace|window\\.location\\.href)\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)");

    private static APIManager instance;
    private final RequestQueue requestQueue;
    private final Gson defaultGson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final Map<String, Object> cache = new HashMap<>();

    private String cookie = "";
    private String authToken = "";
    private String apiKey = "";
    private String secretKey = "";
    private boolean loggingEnabled = BuildConfig.DEBUG;
    private boolean curlLoggingEnabled = BuildConfig.DEBUG;
    private boolean redactSensitiveLogs = true;

    private APIManager(Context context) {
        requestQueue = Volley.newRequestQueue(
                context,
                new HurlStack() {
                    @Override
                    protected HttpURLConnection createConnection(URL url) throws IOException {
                        HttpURLConnection conn = super.createConnection(url);
                        conn.setInstanceFollowRedirects(false); // 👈 KEY LINE
                        return conn;
                    }
                }
        );
    }

    public static synchronized APIManager getInstance(Context context) {
        if (instance == null) instance = new APIManager(context);
        return instance;
    }

    // -------------------------------------
    // Headers
    // -------------------------------------

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }

    public void setCurlLoggingEnabled(boolean enabled) {
        this.curlLoggingEnabled = enabled;
    }

    public void setLogRedactionEnabled(boolean enabled) {
        this.redactSensitiveLogs = enabled;
    }

    private Map<String, String> buildHeaders() {
        return buildHeaders(null);
    }

    private Map<String, String> buildHeaders(Map<String, String> extraHeaders) {
        Map<String, String> headers = new HashMap<>();

        if (!authToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + authToken);
        }

        if (!cookie.isEmpty()) {
            headers.put("Cookie", cookie);
        }

        if (!apiKey.isEmpty()) {
            headers.put("x-redmine-api-key", apiKey);
        }

        if (!secretKey.isEmpty()) {
            headers.put("x-api-secret", secretKey);
        }

        headers.put("X-App-Market", getAppMarketHeaderValue());
        headers.put("X-App-Version", BuildConfig.VERSION_NAME);
        headers.put("X-App-Build", String.valueOf(BuildConfig.VERSION_CODE));
        headers.put("X-App-Platform", "android");

        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }

        return headers;
    }

    public static String getStoreHeaderValue() {
        String store = BuildConfig.STORE != null ? BuildConfig.STORE : "";
        switch (store.toLowerCase(Locale.US)) {
            case "cafebazaar":
                return "cafebazaar";
            case "myket":
                return "myket";
            case "site":
                return "site";
            default:
                return store.toLowerCase(Locale.US);
        }
    }

    public static String getAppMarketHeaderValue() {
        String store = BuildConfig.STORE != null ? BuildConfig.STORE : "";
        switch (store.toLowerCase(Locale.US)) {
            case "cafebazaar":
                return "bazaar";
            case "myket":
                return "myket";
            case "site":
                return "site";
            default:
                return store.toLowerCase(Locale.US);
        }
    }

    // -------------------------------------
    // Object Request
    // -------------------------------------

    public <T, P> void requestObject(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {

        requestObjectInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                callback,
                clazz,
                false, // 👈 retry نشده
                null
        );
    }

    public <T, P> void requestObject(
            String cacheKey,
            P payloadModel,
            Map<String, String> extraHeaders,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {

        requestObjectInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                callback,
                clazz,
                false,
                extraHeaders
        );
    }

    public <T> void requestFormObject(
            String cacheKey,
            Map<String, String> formParams,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        requestFormObjectInternal(cacheKey, formParams, url, useCache, callback, clazz, false, null);
    }

    public <T> void requestFormObject(
            String cacheKey,
            Map<String, String> formParams,
            Map<String, String> extraHeaders,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        requestFormObjectInternal(cacheKey, formParams, url, useCache, callback, clazz, false, extraHeaders);
    }

    public <T> void requestRawFormObject(
            String cacheKey,
            String rawBody,
            Map<String, String> extraHeaders,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz
    ) {
        requestRawFormObjectInternal(cacheKey, rawBody, extraHeaders, url, useCache, callback, clazz, false);
    }

    public void requestText(
            String cacheKey,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<String> callback
    ) {
        requestTextInternal(cacheKey, url, method, useCache, callback, false);
    }

    private void requestTextInternal(
            String cacheKey,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<String> callback,
            boolean retried
    ) {
        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((String) cache.get(cacheKey), true);
                return;
            }

            StringRequest request = new StringRequest(
                    convertMethod(method),
                    url,
                    response -> {
                        logResponse(url, response);
                        cache.put(cacheKey, response);
                        callback.onSuccess(response, false);
                    },
                    error -> {
                        NetworkResponse nr = error.networkResponse;
                        logFailure(url, nr, error);

                        if (nr != null && isRedirect(nr.statusCode) && !retried) {
                            String setCookie = getHeaderIgnoreCase(nr.headers, "Set-Cookie");
                            if (setCookie != null && !setCookie.isEmpty()) {
                                cookie = mergeCookies(cookie, setCookie);
                            }

                            String location = getHeaderIgnoreCase(nr.headers, "Location");
                            requestTextInternal(
                                    cacheKey,
                                    location != null && !location.isEmpty() ? location : url,
                                    method,
                                    useCache,
                                    callback,
                                    true
                            );
                            return;
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseTraceableError(url, nr, error)
                        );
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return buildHeaders();
                }
            };

            logRequest(method, url, buildHeaders(), null, null);
            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    private <T> void requestFormObjectInternal(
            String cacheKey,
            Map<String, String> formParams,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz,
            boolean retried,
            Map<String, String> extraHeaders
    ) {

        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((T) cache.get(cacheKey), true);
                return;
            }

            Gson gson = defaultGson;
            Map<String, String> headers = buildHeaders(extraHeaders);
            String formBody = encodeFormParams(formParams);

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        logResponse(url, response);
                        try {
                            T model = parseStringResponse(response, gson, clazz);
                            cache.put(cacheKey, model);
                            callback.onSuccess(model, false);
                        } catch (Exception e) {
                            callback.onError(-2, e.getMessage());
                        }
                    },
                    error -> {
                        NetworkResponse nr = error.networkResponse;
                        logFailure(url, nr, error);

                        if (nr != null && isRedirect(nr.statusCode)) {
                            String location = getHeaderIgnoreCase(nr.headers, "Location");
                            boolean isSameLocation = location == null || location.isEmpty() || location.equals(url);

                            if (isSameLocation && !retried) {
                                String setCookie = getHeaderIgnoreCase(nr.headers, "Set-Cookie");
                                if (setCookie != null && !setCookie.isEmpty()) {
                                    cookie = mergeCookies(cookie, setCookie);
                                }

                                requestFormObjectInternal(
                                        cacheKey,
                                        formParams,
                                        url,
                                        useCache,
                                        callback,
                                        clazz,
                                        true,
                                        extraHeaders
                                );
                                return;
                            }

                            if (!isSameLocation) {
                                try {
                                    T model = parseStringResponse(new JSONObject().put("url", location).toString(), gson, clazz);
                                    callback.onSuccess(model, false);
                                    return;
                                } catch (Exception ignored) {
                                }
                            }
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseTraceableError(url, nr, error)
                        );
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    return formParams != null ? formParams : new HashMap<>();
                }

                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }

                @Override
                public String getBodyContentType() {
                    return CONTENT_TYPE_FORM;
                }
            };

            logRequest(HttpMethod.POST, url, headers, formBody, CONTENT_TYPE_FORM);
            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    private <T> void requestRawFormObjectInternal(
            String cacheKey,
            String rawBody,
            Map<String, String> extraHeaders,
            String url,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz,
            boolean retried
    ) {
        try {
            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((T) cache.get(cacheKey), true);
                return;
            }

            Gson gson = defaultGson;
            Map<String, String> headers = buildHeaders(extraHeaders);

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        logResponse(url, response);
                        try {
                            T model = parseStringResponse(response, gson, clazz);
                            cache.put(cacheKey, model);
                            callback.onSuccess(model, false);
                        } catch (Exception e) {
                            callback.onError(-2, e.getMessage());
                        }
                    },
                    error -> {
                        NetworkResponse nr = error.networkResponse;
                        logFailure(url, nr, error);

                        if (nr != null && isRedirect(nr.statusCode)) {
                            String location = getHeaderIgnoreCase(nr.headers, "Location");
                            boolean isSameLocation = location == null || location.isEmpty() || location.equals(url);

                            if (isSameLocation && !retried) {
                                String setCookie = getHeaderIgnoreCase(nr.headers, "Set-Cookie");
                                if (setCookie != null && !setCookie.isEmpty()) {
                                    cookie = mergeCookies(cookie, setCookie);
                                }

                                requestRawFormObjectInternal(
                                        cacheKey,
                                        rawBody,
                                        extraHeaders,
                                        url,
                                        useCache,
                                        callback,
                                        clazz,
                                        true
                                );
                                return;
                            }

                            if (!isSameLocation) {
                                try {
                                    T model = parseStringResponse(new JSONObject().put("url", location).toString(), gson, clazz);
                                    callback.onSuccess(model, false);
                                    return;
                                } catch (Exception ignored) {
                                }
                            }
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseTraceableError(url, nr, error)
                        );
                    }
            ) {
                @Override
                public byte[] getBody() {
                    return rawBody != null ? rawBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
                }

                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }

                @Override
                public String getBodyContentType() {
                    return CONTENT_TYPE_RAW_FORM;
                }
            };

            logRequest(HttpMethod.POST, url, headers, rawBody, CONTENT_TYPE_RAW_FORM);
            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    public <T, P> void requestList(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz
    ) {

        requestListInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                isPublicApi,
                callback,
                clazz,
                false, // 👈 هنوز retry نشده
                null
        );
    }

    public <T, P> void requestList(
            String cacheKey,
            P payloadModel,
            Map<String, String> extraHeaders,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz
    ) {

        requestListInternal(
                cacheKey,
                payloadModel,
                url,
                method,
                useCache,
                isPublicApi,
                callback,
                clazz,
                false,
                extraHeaders
        );
    }

    private <T, P> void requestObjectInternal(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            ApiCallback<T> callback,
            Class<T> clazz,
            boolean retried,
            Map<String, String> extraHeaders
    ) {

        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((T) cache.get(cacheKey), true);
                return;
            }

            Gson gson = defaultGson;

            JSONObject payload = payloadModel != null
                    ? new JSONObject(gson.toJson(payloadModel))
                    : null;
            Map<String, String> headers = buildHeaders(extraHeaders);

            JsonObjectRequest request = new JsonObjectRequest(
                    convertMethod(method),
                    url,
                    payload,
                    response -> {
                        logResponse(url, response);
                        try {

                            if (response == null || response.toString().equals("null")) {
                                if (clazz.equals(EmptyResponse.class)) {
                                    T model = clazz.getDeclaredConstructor().newInstance();
                                    callback.onSuccess(model, false);
                                    return;
                                }
                                response = new JSONObject();
                            }

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) data = response;

                            T model = gson.fromJson(data.toString(), clazz);
                            cache.put(cacheKey, model);
                            callback.onSuccess(model, false);

                        } catch (Exception e) {
                            callback.onError(-2, e.getMessage());
                        }
                    },
                    error -> {

                        NetworkResponse nr = error.networkResponse;
                        logFailure(url, nr, error);

                        if (nr != null && isRedirect(nr.statusCode)) {
                            String location = getHeaderIgnoreCase(nr.headers, "Location");
                            boolean isSameLocation = location == null || location.isEmpty() || location.equals(url);

                            String setCookie = getHeaderIgnoreCase(nr.headers, "Set-Cookie");
                            if (setCookie != null && !setCookie.isEmpty()) {
                                cookie = mergeCookies(cookie, setCookie);
                            }

                            if (retried) {
                                callback.onError(nr.statusCode, parseTraceableError(url, nr, error));
                                return;
                            }

                            requestObjectInternal(
                                    cacheKey,
                                    payloadModel,
                                    isSameLocation ? url : resolveRedirectUrl(url, location),
                                    method,
                                    useCache,
                                    callback,
                                    clazz,
                                    true,
                                    extraHeaders
                            );
                            return;
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseTraceableError(url, nr, error)
                        );
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }

                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    if (clazz.equals(EmptyResponse.class)) {
                        return Response.success(
                                new JSONObject(),
                                HttpHeaderParser.parseCacheHeaders(response)
                        );
                    }
                    return super.parseNetworkResponse(response);
                }
            };

            applyEndpointRetryPolicy(request, url);
            logRequest(method, url, headers, payload != null ? payload.toString() : null, CONTENT_TYPE_JSON);
            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }

    private <T, P> void requestListInternal(
            String cacheKey,
            P payloadModel,
            String url,
            HttpMethod method,
            boolean useCache,
            boolean isPublicApi,
            ApiCallback<List<T>> callback,
            Class<T> clazz,
            boolean retried,
            Map<String, String> extraHeaders
    ) {

        try {

            if (useCache && cache.containsKey(cacheKey)) {
                callback.onSuccess((List<T>) cache.get(cacheKey), true);
                return;
            }

            Gson gson = isPublicApi
                    ? new GsonBuilder().create()
                    : defaultGson;

            JSONObject payload = payloadModel != null
                    ? new JSONObject(gson.toJson(payloadModel))
                    : null;
            Map<String, String> headers = buildHeaders(extraHeaders);

            CustomJsonArrayRequest request = new CustomJsonArrayRequest(
                    convertMethod(method),
                    url,
                    payload,
                    headers,
                    jsonArray -> {
                        logResponse(url, jsonArray);

                        List<T> list = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject item = jsonArray.optJSONObject(i);
                            if (item != null) {
                                list.add(gson.fromJson(item.toString(), clazz));
                            }
                        }

                        cache.put(cacheKey, list);
                        callback.onSuccess(list, false);
                    },
                    error -> {

                        NetworkResponse nr = error.networkResponse;
                        logFailure(url, nr, error);

                        // 🔥 HANDLE 307 HERE
                        if (nr != null && nr.statusCode == 307 && !retried) {

                            if (nr.headers != null && nr.headers.containsKey("Set-Cookie")) {
                                cookie = mergeCookies(cookie, nr.headers.get("Set-Cookie"));
                            }

                            // 🔁 retry once with new cookie
                            requestListInternal(
                                    cacheKey,
                                    payloadModel,
                                    url,
                                    method,
                                    useCache,
                                    isPublicApi,
                                    callback,
                                    clazz,
                                    true,
                                    extraHeaders
                            );
                            return;
                        }

                        callback.onError(
                                nr != null ? nr.statusCode : -1,
                                parseVolleyError(nr, error)
                        );
                    }
            );

            logRequest(method, url, headers, payload != null ? payload.toString() : null, CONTENT_TYPE_JSON);
            requestQueue.add(request);

        } catch (Exception e) {
            callback.onError(-1, e.getMessage());
        }
    }


    // -------------------------------------
    // Utils
    // -------------------------------------

    private void logRequest(HttpMethod method, String url, Map<String, String> headers, String body, String contentType) {
        if (loggingEnabled) {
            Log.d(TAG, "--> " + method.name() + " " + url);
        }

        if (curlLoggingEnabled) {
            Log.d(TAG, buildCurl(method, url, headers, body, contentType));
        }
    }

    private void logResponse(String url, Object response) {
        if (!loggingEnabled) return;

        String raw = response != null ? response.toString() : "";
        Log.d(TAG, "<-- " + url + " (" + raw.length() + " chars)");
    }

    private void logFailure(String url, NetworkResponse response, Throwable error) {
        if (!loggingEnabled) return;

        int statusCode = response != null ? response.statusCode : -1;
        String message = error != null ? error.getClass().getSimpleName() : "Unknown";
        String requestId = response != null ? getHeaderIgnoreCase(response.headers, "x-request-id") : null;
        String correlationId = response != null ? getHeaderIgnoreCase(response.headers, "x-correlation-id") : null;
        StringBuilder log = new StringBuilder("<-- ERROR ")
                .append(statusCode)
                .append(" ")
                .append(url)
                .append(" (")
                .append(message)
                .append(")");
        if (requestId != null && !requestId.isEmpty()) {
            log.append(" requestId=").append(requestId);
        }
        if (correlationId != null && !correlationId.isEmpty() && !correlationId.equals(requestId)) {
            log.append(" correlationId=").append(correlationId);
        }
        Log.e(TAG, log.toString());

        if (response != null && response.data != null && response.data.length > 0) {
            String body = sanitizeBody(new String(response.data, StandardCharsets.UTF_8));
            if (body.length() > 1500) {
                body = body.substring(0, 1500) + "...";
            }
            Log.e(TAG, "<-- ERROR BODY " + body);
        }
    }

    private String buildCurl(HttpMethod method, String url, Map<String, String> headers, String body, String contentType) {
        StringBuilder curl = new StringBuilder("curl --request ")
                .append(method.name())
                .append(" --url ")
                .append(shellQuote(url));

        Map<String, String> logHeaders = sanitizeHeaders(headers);
        if (contentType != null && !contentType.isEmpty() && !hasHeader(logHeaders, "Content-Type")) {
            logHeaders.put("Content-Type", contentType);
        }

        for (Map.Entry<String, String> entry : logHeaders.entrySet()) {
            curl.append(" --header ")
                    .append(shellQuote(entry.getKey() + ": " + entry.getValue()));
        }

        if (body != null && !body.isEmpty()) {
            for (String dataPart : buildCurlDataParts(body, contentType)) {
                curl.append(" --data ")
                        .append(shellQuote(sanitizeBody(dataPart)));
            }
        }

        return curl.toString();
    }

    private List<String> buildCurlDataParts(String body, String contentType) {
        List<String> parts = new ArrayList<>();
        if (contentType == null || !contentType.toLowerCase(Locale.US).startsWith("application/x-www-form-urlencoded")) {
            parts.add(body);
            return parts;
        }

        String[] knownKeys = new String[]{"api_key", "amount", "sla", "period", "discount", "payload", "user"};
        int cursor = 0;
        for (int i = 0; i < knownKeys.length; i++) {
            String keyPrefix = knownKeys[i] + "=";
            int start = body.indexOf(keyPrefix, cursor);
            if (start < 0) continue;

            int end = body.length();
            for (int j = i + 1; j < knownKeys.length; j++) {
                int next = body.indexOf("&" + knownKeys[j] + "=", start + keyPrefix.length());
                if (next >= 0) {
                    end = next;
                    break;
                }
            }

            parts.add(body.substring(start, end));
            cursor = end + 1;
        }

        if (parts.isEmpty()) {
            parts.add(body);
        }
        return parts;
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> safeHeaders = new HashMap<>();
        if (headers == null) return safeHeaders;

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            safeHeaders.put(key, shouldRedactHeader(key) ? REDACTED : safeValue(value));
        }
        return safeHeaders;
    }

    private boolean shouldRedactHeader(String key) {
        if (!redactSensitiveLogs || key == null) return false;
        String normalized = key.toLowerCase(Locale.US);
        return normalized.equals("authorization")
                || normalized.equals("cookie")
                || normalized.equals("x-redmine-api-key")
                || normalized.equals("x-api-secret");
    }

    private String sanitizeBody(String body) {
        if (!redactSensitiveLogs || body == null) return safeValue(body);

        String sanitized = body;
        sanitized = sanitized.replaceAll("(?i)(api_key=)[^&\\s]+", "$1" + REDACTED);
        sanitized = sanitized.replaceAll("(?i)(password=)[^&\\s]+", "$1" + REDACTED);
        sanitized = sanitized.replaceAll("(?i)(purchase_token=)[^&\\s]+", "$1" + REDACTED);
        sanitized = sanitized.replaceAll("(?i)(\"api_key\"\\s*:\\s*\")[^\"]+\"", "$1" + REDACTED + "\"");
        sanitized = sanitized.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]+\"", "$1" + REDACTED + "\"");
        sanitized = sanitized.replaceAll("(?i)(\"identifier\"\\s*:\\s*\")[^\"]+\"", "$1" + REDACTED + "\"");
        sanitized = sanitized.replaceAll("(?i)(\"code\"\\s*:\\s*\")[^\"]+\"", "$1" + REDACTED + "\"");
        sanitized = sanitized.replaceAll("(?i)(\"purchase_token\"\\s*:\\s*\")[^\"]+\"", "$1" + REDACTED + "\"");
        return sanitized;
    }

    private String encodeFormParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (body.length() > 0) body.append("&");
            body.append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(entry.getValue()));
        }
        return body.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(safeValue(value), "UTF-8");
        } catch (Exception ignored) {
            return safeValue(value);
        }
    }

    private boolean hasHeader(Map<String, String> headers, String key) {
        return getHeaderIgnoreCase(headers, key) != null;
    }

    private String shellQuote(String value) {
        return "'" + safeValue(value).replace("'", "'\\''") + "'";
    }

    private String safeValue(String value) {
        return value != null ? value : "";
    }

    private String mergeCookies(String oldCookie, String setCookieHeader) {

        StringBuilder result = new StringBuilder(oldCookie == null ? "" : oldCookie);

        String[] cookies = setCookieHeader.split(",");
        for (String cookie : cookies) {
            String clean = cookie.split(";", 2)[0].trim();
            if (result.length() > 0) result.append("; ");
            result.append(clean);
        }

        return result.toString();
    }

    private int convertMethod(HttpMethod method) {
        switch (method) {
            case POST:
                return Request.Method.POST;
            case PUT:
                return Request.Method.PUT;
            case DELETE:
                return Request.Method.DELETE;
            default:
                return Request.Method.GET;
        }
    }

    private boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private String resolveRedirectUrl(String requestUrl, String location) {
        if (location == null || location.trim().isEmpty()) return requestUrl;
        try {
            return new URL(new URL(requestUrl), location.trim()).toString();
        } catch (Exception ignored) {
            return location.trim();
        }
    }

    private String getHeaderIgnoreCase(Map<String, String> headers, String key) {
        if (headers == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private <T> T parseStringResponse(String rawResponse, Gson gson, Class<T> clazz) throws Exception {
        String response = rawResponse == null ? "" : rawResponse.trim();
        JSONObject json;
        if (response.startsWith("{")) {
            json = new JSONObject(response);
        } else if (isDiscountResponse(clazz) && response.matches("-?\\d+")) {
            json = new JSONObject()
                    .put("status", true)
                    .put("discount", Long.parseLong(response));
        } else {
            String redirectUrl = extractRedirectUrl(response);
            json = new JSONObject().put("url", redirectUrl != null ? redirectUrl : response);
        }

        if (isDiscountResponse(clazz) && json.has("data") && !(json.opt("data") instanceof JSONObject)) {
            Object rawData = json.opt("data");
            if (rawData instanceof Number || String.valueOf(rawData).matches("-?\\d+")) {
                json = new JSONObject()
                        .put("status", true)
                        .put("discount", Long.parseLong(String.valueOf(rawData)));
            }
        }

        JSONObject data = json.optJSONObject("data");
        if (data == null) data = json;

        return gson.fromJson(data.toString(), clazz);
    }

    private boolean isDiscountResponse(Class<?> clazz) {
        return clazz != null && "ir.shecan.data.modelDto.DiscountViewModel".equals(clazz.getName());
    }

    private String extractRedirectUrl(String response) {
        if (response == null || response.isEmpty()) return null;

        String url = findFirstGroup(META_REFRESH_URL_PATTERN, response);
        if (url == null) {
            url = findFirstGroup(FORM_ACTION_PATTERN, response);
        }
        if (url == null) {
            url = findFirstGroup(WINDOW_LOCATION_PATTERN, response);
        }
        return url != null ? url.trim() : null;
    }

    private String findFirstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String parseVolleyError(NetworkResponse response, Throwable error) {

        if (response != null && response.data != null) {
            try {
                String json = new String(response.data, StandardCharsets.UTF_8);
                JSONObject obj = new JSONObject(json);

                if (obj.has("message")) {
                    return appendErrorDetail(obj.optString("message"), obj.optString("detail"));
                }

                if (obj.has("err")) {
                    Object errObj = obj.get("err");
                    if (errObj instanceof JSONObject) {
                        JSONObject err = (JSONObject) errObj;
                        if (err.has("body")) {
                            return appendErrorDetail(err.optString("body"), obj.optString("detail"));
                        }
                        if (err.has("context")) {
                            return appendErrorDetail(err.optString("context"), obj.optString("detail"));
                        }
                        if (err.has("errors")) {
                            Object errorsObj = err.get("errors");
                            if (errorsObj instanceof JSONArray) {
                                JSONArray errors = (JSONArray) errorsObj;
                                if (errors.length() > 0) {
                                    return appendErrorDetail(errors.getString(0), obj.optString("detail"));
                                }
                            }
                            return appendErrorDetail(err.optString("errors"), obj.optString("detail"));
                        }
                        return appendErrorDetail(err.toString(), obj.optString("detail"));
                    }
                    return appendErrorDetail(String.valueOf(errObj), obj.optString("detail"));
                }

                if (obj.has("error")) {
                    return appendErrorDetail(obj.optString("error"), obj.optString("detail"));
                }

                if (obj.has("detail"))
                    return obj.getString("detail");

                return json;

            } catch (Exception ignored) {
            }
        }

        // Network errors
        if (error instanceof AuthFailureError)
            return "خطا در احراز هویت";

        if (error instanceof ParseError)
            return "خطا در پردازش پاسخ سرور";

        if (error instanceof com.android.volley.TimeoutError)
            return "ارتباط با سرور برقرار نشد";

//        if (error instanceof com.android.volley.ServerError)
//            return "ارتباط با سرور برقرار نشد";

        if (error instanceof java.net.UnknownHostException)
            return "اتصال به اینترنت برقرار نیست یا سرور در دسترس نیست";

        if (error instanceof java.net.ConnectException)
            return "عدم دسترسی به سرور";

        if (error instanceof java.net.SocketTimeoutException)
            return "زمان اتصال به سرور به پایان رسید";

        if (error instanceof NoConnectionError)
            return "اتصال به اینترنت برقرار نیست";

        if (error.getCause() instanceof UnknownHostException)
            return "سرور یافت نشد (مشکل DNS)";

        return "خطایی رخ داده است";
    }

    private String parseTraceableError(String url, NetworkResponse response, Throwable error) {
        String message = parseVolleyError(response, error);
        if (!isBillingEndpoint(url)) {
            return message;
        }

        String requestId = response != null ? getHeaderIgnoreCase(response.headers, "x-request-id") : null;
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = response != null ? getHeaderIgnoreCase(response.headers, "x-correlation-id") : null;
        }
        if (requestId == null || requestId.trim().isEmpty()) {
            return message;
        }
        return message + " | کد پیگیری: " + requestId;
    }

    private boolean isBillingEndpoint(String url) {
        return url != null && (url.contains("/price")
                || url.contains("/payment")
                || url.contains("/iap/verify")
                || url.contains("/use-discount"));
    }

    private void applyEndpointRetryPolicy(Request<?> request, String url) {
        if (url == null || !url.contains("/api/iap/verify")) return;

        request.setRetryPolicy(new DefaultRetryPolicy(
                IAP_VERIFY_TIMEOUT_MS,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
    }

    private String appendErrorDetail(String message, String detail) {
        String safeMessage = message != null ? message.trim() : "";
        String safeDetail = detail != null ? detail.trim() : "";
        if (safeDetail.isEmpty() || safeDetail.equals(safeMessage)) {
            return safeMessage;
        }
        if (safeMessage.isEmpty()) {
            return safeDetail;
        }
        return safeMessage + ": " + safeDetail;
    }

}
