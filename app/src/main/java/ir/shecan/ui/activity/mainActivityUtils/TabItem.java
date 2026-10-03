package ir.shecan.ui.activity.mainActivityUtils;

import ir.shecan.ui.fragment.refactor.ConfigListFragment;
import ir.shecan.ui.fragment.refactor.HomeFragment;
import ir.shecan.ui.fragment.refactor.ProfileFragment;

public enum TabItem {
    CONFIG(0, "سرویس‌ها", ConfigListFragment.class),
    HOME(1, null, HomeFragment.class),
    PROFILE(2, "تنظیمات", ProfileFragment.class);

    private final int index;
    private final String title;
    private final Class<? extends ir.shecan.ui.fragment.ToolbarFragment> fragmentClass;

    TabItem(int index, String title, Class<? extends ir.shecan.ui.fragment.ToolbarFragment> fragmentClass) {
        this.index = index;
        this.title = title;
        this.fragmentClass = fragmentClass;
    }

    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    public Class<? extends ir.shecan.ui.fragment.ToolbarFragment> getFragmentClass() {
        return fragmentClass;
    }

    public static TabItem fromIndex(int index) {
        for (TabItem item : values()) {
            if (item.getIndex() == index) return item;
        }
        return HOME; // default
    }
}
