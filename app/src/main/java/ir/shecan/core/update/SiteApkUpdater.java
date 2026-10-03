package ir.shecan.core.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class SiteApkUpdater {
    private static final String TAG = "SiteApkUpdater";
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_REDIRECTS = 5;

    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private File downloadedApkFile;

    public interface Listener {
        void onMissingUrl();

        void onInstallPermissionRequired();

        void onDownloadStarted();

        void onProgress(int progress);

        void onDownloaded();

        void onFailed();
    }

    public SiteApkUpdater(Activity activity) {
        this.activity = activity;
    }

    public void download(String url, Listener listener) {
        if (url == null || url.trim().isEmpty()) {
            dispatch(() -> {
                if (listener != null) listener.onMissingUrl();
            });
            return;
        }

        if (!canInstallPackages()) {
            requestInstallPermission();
            dispatch(() -> {
                if (listener != null) listener.onInstallPermissionRequired();
            });
            return;
        }

        downloadedApkFile = null;
        File apkFile = createApkFile();
        dispatch(() -> {
            if (listener != null) listener.onDownloadStarted();
        });

        new Thread(() -> downloadInternal(url.trim(), apkFile, listener), "site-apk-update-download").start();
    }

    private void downloadInternal(String url, File apkFile, Listener listener) {
        HttpURLConnection connection = null;
        try {
            CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            connection = openConnection(url, 0, cookieManager);
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                Log.e(TAG, "Download failed. HTTP " + responseCode + " for " + url);
                fail(listener);
                return;
            }

            int totalSize = connection.getContentLength();
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(apkFile, false)) {
                byte[] buffer = new byte[16 * 1024];
                long downloaded = 0L;
                int lastProgress = -1;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    downloaded += read;

                    if (totalSize > 0) {
                        int progress = Math.max(0, Math.min(100, Math.round(downloaded * 100f / totalSize)));
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            int finalProgress = progress;
                            dispatch(() -> {
                                if (listener != null) listener.onProgress(finalProgress);
                            });
                        }
                    }
                }
                output.flush();
            }

            if (!apkFile.exists() || apkFile.length() <= 0L) {
                Log.e(TAG, "Download failed. Empty APK file.");
                fail(listener);
                return;
            }

            downloadedApkFile = apkFile;
            dispatch(() -> {
                if (listener != null) listener.onProgress(100);
                if (listener != null) listener.onDownloaded();
                installDownloadedApk();
            });
        } catch (Exception e) {
            Log.e(TAG, "Download failed.", e);
            fail(listener);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String url, int redirects, CookieManager cookieManager) throws Exception {
        if (redirects > MAX_REDIRECTS) {
            throw new IllegalStateException("Too many redirects");
        }

        URL parsedUrl = new URL(url);
        URI uri = parsedUrl.toURI();
        HttpURLConnection connection = (HttpURLConnection) parsedUrl.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Shecan");
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive,application/octet-stream,*/*");
        addCookies(connection, uri, cookieManager);

        int responseCode = connection.getResponseCode();
        storeCookies(connection, uri, cookieManager);
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                || responseCode == 307
                || responseCode == 308) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.trim().isEmpty()) {
                throw new IllegalStateException("Redirect without Location");
            }
            URL nextUrl = new URL(new URL(url), location);
            return openConnection(nextUrl.toString(), redirects + 1, cookieManager);
        }

        return connection;
    }

    private void addCookies(HttpURLConnection connection, URI uri, CookieManager cookieManager) throws Exception {
        Map<String, List<String>> cookieHeaders = cookieManager.get(uri, connection.getRequestProperties());
        for (Map.Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            connection.setRequestProperty(entry.getKey(), String.join("; ", entry.getValue()));
        }
    }

    private void storeCookies(HttpURLConnection connection, URI uri, CookieManager cookieManager) throws Exception {
        cookieManager.put(uri, connection.getHeaderFields());
    }

    private boolean canInstallPackages() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls();
    }

    private void requestInstallPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    private File createApkFile() {
        File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = activity.getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        File apkFile = new File(dir, "shecan-site-update.apk");
        if (apkFile.exists()) apkFile.delete();
        return apkFile;
    }

    public void installDownloadedApk() {
        if (downloadedApkFile == null || !downloadedApkFile.exists()) return;
        Uri apkUri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".site_update_provider",
                downloadedApkFile
        );
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private void fail(Listener listener) {
        dispatch(() -> {
            if (listener != null) listener.onFailed();
        });
    }

    private void dispatch(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
