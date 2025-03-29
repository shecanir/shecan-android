package ir.shecan.service;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.HurlStack;
import javax.net.ssl.SSLSocketFactory;

public class VolleyHelper {

    public static RequestQueue getSecureRequestQueue(Context context) {
        SSLSocketFactory sslSocketFactory = CustomSSLSocketFactory.getSSLSocketFactory(context);
        return Volley.newRequestQueue(context, new HurlStack(null, sslSocketFactory));
    }
}