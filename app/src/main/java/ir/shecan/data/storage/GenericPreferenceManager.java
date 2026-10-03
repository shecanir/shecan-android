package ir.shecan.data.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GenericPreferenceManager {

    private static final String PREF_NAME = "APP_STORAGE";
    private static GenericPreferenceManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private GenericPreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized GenericPreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new GenericPreferenceManager(context.getApplicationContext());
        }
        return instance;
    }

    // ذخیره هر مدل
    public <T> void saveModel(String key, T model) {
        String json = gson.toJson(model);
        prefs.edit().putString(key, json).apply();
    }

    // گرفتن مدل
    public <T> T getModel(String key, Class<T> clazz) {
        String json = prefs.getString(key, null);
        if (json == null) return null;
        return gson.fromJson(json, clazz);
    }

    // گرفتن مدل با TypeToken (برای لیست و آرایه‌ها)
    public <T> T getModel(String key, TypeToken<T> typeToken) {
        String json = prefs.getString(key, null);
        if (json == null) return null;
        return gson.fromJson(json, typeToken.getType());
    }

    // حذف مدل
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    // پاک کردن همه چیز
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
