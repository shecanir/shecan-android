package ir.shecan.core.util;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import ir.shecan.Shecan;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class Logger {
    private static StringBuffer buffer = null;

    public static void init() {
        if (buffer != null) {
            buffer.setLength(0);
        } else {
            buffer = new StringBuffer();
        }
    }

    public static void shutdown() {
        buffer = null;
    }

    public static String getLog() {
        return buffer.toString();
    }

    public static void error(String message) {
        send("[ERROR] " + message);
        createBreadcrumb("error", message, SentryLevel.ERROR);
    }

    public static void warning(String message) {
        send("[WARNING] " + message);
        createBreadcrumb("warning", message, SentryLevel.WARNING);
    }

    public static void info(String message) {
        send("[INFO] " + message);
        createBreadcrumb("info", message, SentryLevel.INFO);
    }

    public static void debug(String message) {
        send("[DEBUG] " + message);
        createBreadcrumb("debug", message, SentryLevel.DEBUG);
    }

    public static void logException(Throwable e) {
        error(getExceptionMessage(e));
//        Sentry.captureException(e);
        createBreadcrumb("logException", e.getMessage(), SentryLevel.FATAL);
    }

    private static void createBreadcrumb(String category, String message, SentryLevel level) {
        Breadcrumb breadcrumb = new Breadcrumb();
        breadcrumb.setCategory("CustomLog " + category);
        breadcrumb.setMessage(message);
        breadcrumb.setLevel(level);
        Sentry.addBreadcrumb(breadcrumb);
    }

    public static String getExceptionMessage(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private static int getLogSizeLimit() {
        return Integer.parseInt(Shecan.getPrefs().getString("settings_log_size", "10000"));
    }

    private static boolean checkBufferSize() {
        int limit = getLogSizeLimit();
        if (limit == 0) {//DISABLED!
            return false;
        }
        if (limit == -1) {//N0 limit
            return true;
        }
        if (buffer.length() > limit) {//LET's clean it up!
            buffer.setLength(limit);
        }
        return true;
    }

    private static void send(String message) {
        try {
            if (checkBufferSize()) {
                String fileDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date());
                buffer.insert(0, "\n").insert(0, message).insert(0, fileDateFormat);
            }
            Log.d("Shecan", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
