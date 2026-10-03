package ir.shecan.core.service;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import javax.net.ssl.SSLSocketFactory;

public class VolleyHelper {

    private static volatile RequestQueue secureRequestQueue;

    public static RequestQueue getSecureRequestQueue(Context context) {
        RequestQueue queue = secureRequestQueue;
        if (queue == null) {
            synchronized (VolleyHelper.class) {
                queue = secureRequestQueue;
                if (queue == null) {
                    Context applicationContext = context.getApplicationContext();
                    SSLSocketFactory sslSocketFactory =
                            CustomSSLSocketFactory.getSSLSocketFactory(applicationContext);
                    queue = Volley.newRequestQueue(
                            applicationContext,
                            new HurlStack(null, sslSocketFactory)
                    );
                    secureRequestQueue = queue;
                }
            }
        }
        return queue;
    }
}
