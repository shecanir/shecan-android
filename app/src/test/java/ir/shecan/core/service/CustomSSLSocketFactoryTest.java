package ir.shecan.core.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.Test;

public class CustomSSLSocketFactoryTest {
    private X509TrustManager manager(KeyStore store) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager) return (X509TrustManager) manager;
        }
        throw new AssertionError("No X509 trust manager");
    }

    private X509Certificate bundledRoot() throws Exception {
        try (InputStream input = new FileInputStream("src/main/res/raw/cert1.pem")) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    private X509TrustManager bundledManager() throws Exception {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, null);
        store.setCertificateEntry("isrg", bundledRoot());
        return manager(store);
    }

    @Test
    public void acceptsPlatformRootOutsideBundledTrustStore() throws Exception {
        X509TrustManager system = manager(null);
        X509TrustManager bundled = bundledManager();
        X509Certificate root = Arrays.stream(system.getAcceptedIssuers())
                .filter(cert -> cert.getSubjectX500Principal().getName().contains("DigiCert Global Root"))
                .findFirst().orElseThrow(() -> new AssertionError("Expected JDK platform root"));
        X509Certificate[] chain = {root};
        try {
            bundled.checkServerTrusted(chain, "RSA");
            fail("The original bundled-only trust store should reject this root");
        } catch (CertificateException expected) {
            // Reproduce the original restriction before verifying the fix.
        }
        new CustomSSLSocketFactory.SystemAndBundledTrustManager(system, bundled)
                .checkServerTrusted(chain, "RSA");
    }

    @Test
    public void preservesBundledRootOnLegacyPlatform() throws Exception {
        X509TrustManager legacyPlatform = new X509TrustManager() {
            public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
                throw new CertificateException("Platform does not contain ISRG Root X1");
            }
            public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
                throw new CertificateException("Untrusted client");
            }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        };
        new CustomSSLSocketFactory.SystemAndBundledTrustManager(legacyPlatform, bundledManager())
                .checkServerTrusted(new X509Certificate[]{bundledRoot()}, "RSA");
    }

    @Test
    public void rejectsUntrustedSelfSignedCertificate() throws Exception {
        X509Certificate certificate;
        try (InputStream input = getClass().getResourceAsStream("/tls/untrusted.pem")) {
            certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
        try {
            new CustomSSLSocketFactory.SystemAndBundledTrustManager(manager(null), bundledManager())
                    .checkServerTrusted(new X509Certificate[]{certificate}, "RSA");
            fail("Untrusted certificate must be rejected");
        } catch (CertificateException expected) {
            assertTrue(expected.getSuppressed().length > 0);
        }
    }
}
