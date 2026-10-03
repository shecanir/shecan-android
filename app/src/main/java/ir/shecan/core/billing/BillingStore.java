package ir.shecan.core.billing;

import ir.shecan.core.constant.Constant;

public enum BillingStore {
    CAFE_BAZAAR("CafeBazaar", "کافه‌بازار"),
    MYKET("Myket", "مایکت"),
    SITE("Site", "سایت");

    private final String value;
    private final String title;

    BillingStore(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static BillingStore current() {
        if (Constant.IsCafeBazaarMode) return CAFE_BAZAAR;
        if (Constant.IsMyketMode) return MYKET;
        return SITE;
    }
}
