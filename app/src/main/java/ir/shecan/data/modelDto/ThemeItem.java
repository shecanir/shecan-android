package ir.shecan.data.modelDto;

import androidx.annotation.IntDef;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeItem {

    @IntDef({
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    })
    public @interface NightMode {}

    private final String title;

    @NightMode
    private final int mode;

    public ThemeItem(String title, @NightMode int mode) {
        this.title = title;
        this.mode = mode;
    }

    public String getTitle() { return title; }

    @NightMode
    public int getMode() { return mode; }
}

