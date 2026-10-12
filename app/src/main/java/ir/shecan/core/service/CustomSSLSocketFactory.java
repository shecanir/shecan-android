package ir.shecan.core.service;


import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import ir.shecan.R;

public class CustomSSLSocketFactory {

    public static SSLSocketFactory getSSLSocketFactory(Context context) {
        try {
            // Keep the platform trust store, including roots other than Let's Encrypt.
            TrustManagerFactory systemFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            systemFactory.init((KeyStore) null);

            // Add ISRG Root X1 for older Android versions that do not bundle it.
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
            TrustManager[] trustManagers = {new SystemAndBundledTrustManager(
                    findX509TrustManager(systemFactory), findX509TrustManager(tmf))};

            // Create an SSLContext with the TrustManagers
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
            return sslContext.getSocketFactory();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL socket factory", e);
        }
    }

    private static X509TrustManager findX509TrustManager(TrustManagerFactory factory) {
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager) return (X509TrustManager) manager;
        }
        throw new IllegalStateException("No X509 trust manager available");
    }

    static final class SystemAndBundledTrustManager implements X509TrustManager {
        private final X509TrustManager system;
        private final X509TrustManager bundled;

        SystemAndBundledTrustManager(X509TrustManager system, X509TrustManager bundled) {
            this.system = system;
            this.bundled = bundled;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            system.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                system.checkServerTrusted(chain, authType);
            } catch (CertificateException systemFailure) {
                try {
                    bundled.checkServerTrusted(chain, authType);
                } catch (CertificateException bundledFailure) {
                    systemFailure.addSuppressed(bundledFailure);
                    throw systemFailure;
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] systemIssuers = system.getAcceptedIssuers();
            X509Certificate[] bundledIssuers = bundled.getAcceptedIssuers();
            X509Certificate[] issuers = Arrays.copyOf(systemIssuers,
                    systemIssuers.length + bundledIssuers.length);
            System.arraycopy(bundledIssuers, 0, issuers, systemIssuers.length,
                    bundledIssuers.length);
            return issuers;
        }
    }
}
