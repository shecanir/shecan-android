package ir.shecan.data.api;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CustomJsonArrayRequest extends Request<JSONArray> {

    private final Response.Listener<JSONArray> listener;
    private final JSONObject requestBody;
    private final Map<String, String> headers;

    public CustomJsonArrayRequest(
            int method,
            String url,
            JSONObject requestBody,
            Map<String, String> headers,
            Response.Listener<JSONArray> listener,
            Response.ErrorListener errorListener
    ) {
        super(method, url, errorListener);
        this.listener = listener;
        this.requestBody = requestBody;
        this.headers = headers != null ? headers : new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public byte[] getBody() {
        if (requestBody == null) return null;
        return requestBody.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getBodyContentType() {
        return "application/json; charset=utf-8";
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, StandardCharsets.UTF_8);
            return Response.success(new JSONArray(json), HttpHeaderParser.parseCacheHeaders(response));

        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(JSONArray response) {
        listener.onResponse(response);
    }
}
