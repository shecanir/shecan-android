package ir.shecan.service;


import android.content.Context;


import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocketFactory;

import ir.shecan.R;

public class CustomSSLSocketFactory {

    public static SSLSocketFactory getSSLSocketFactory(Context context) {
        try {
            // Load CAs from raw resources
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            // Load the certificate from raw folder
            InputStream caInput = context.getResources().openRawResource(R.raw.cert1);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // Create a KeyStore with the trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            // Create an SSLContext with the TrustManagers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL socket factory", e);
        }
    }
}