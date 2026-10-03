package ir.shecan.core.billing;

public enum BillingPeriod {
    ONE_MONTH("1m", "یک ماهه", 17),
    THREE_MONTHS("3m", "سه ماهه", 50),
    SIX_MONTHS("6m", "شش ماهه", 51),
    ONE_YEAR("1y", "سالانه", 18);

    private final String apiValue;
    private final String title;
    private final int durationId;

    BillingPeriod(String apiValue, String title, int durationId) {
        this.apiValue = apiValue;
        this.title = title;
        this.durationId = durationId;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationId() {
        return durationId;
    }

    public static BillingPeriod fromApiValue(String value) {
        if (value == null) return null;
        for (BillingPeriod period : values()) {
            if (period.apiValue.equalsIgnoreCase(value.trim())) {
                return period;
            }
        }
        return null;
    }

    public static BillingPeriod fromDurationId(int durationId) {
        for (BillingPeriod period : values()) {
            if (period.durationId == durationId) {
                return period;
            }
        }
        return null;
    }
}
