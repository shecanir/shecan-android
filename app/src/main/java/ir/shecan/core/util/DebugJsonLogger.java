package ir.shecan.core.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ir.shecan.BuildConfig;

public final class DebugJsonLogger {

    private static final int LOG_CHUNK_SIZE = 3000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DebugJsonLogger() {
    }

    public static void log(String tag, Object value) {
        if (!BuildConfig.DEBUG) return;

        String json = GSON.toJson(value);
        int chunkCount = Math.max(1, (json.length() + LOG_CHUNK_SIZE - 1) / LOG_CHUNK_SIZE);

        for (int index = 0; index < chunkCount; index++) {
            int start = index * LOG_CHUNK_SIZE;
            int end = Math.min(json.length(), start + LOG_CHUNK_SIZE);
            Log.d(tag, "[" + (index + 1) + "/" + chunkCount + "] " + json.substring(start, end));
        }
    }
}
