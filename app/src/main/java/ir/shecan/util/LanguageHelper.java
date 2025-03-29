package ir.shecan.util;

import ir.shecan.Shecan;

import java.util.Locale;

public class LanguageHelper {

    private static final String[] ids = {"fa", "en"};
    private static final String[] names = {"فارسی", "English"};

    public static String[] getIds() {
        return ids;
    }

    public static String[] getNames() {
        return names;
    }

    public static String getDescription(String key) {
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            if (id.equals(key))
                return names[i];
        }
        return "";
    }

    public static String getLanguage() {
        // comment it now (maybe in the future we need it
//        return Shecan.getPrefs().getString("settings_language", "fa");
        return "fa";
    }
}
