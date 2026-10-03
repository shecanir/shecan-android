package ir.shecan.core.billing;

public enum BillingSla {
    BRONZE("bronze", "برنزی", 47),
    SILVER("silver", "نقره‌ای", 48),
    GOLD("gold", "طلایی", 49),
    COMMERCIAL("commercial", "تجاری", 83);

    private final String apiValue;
    private final String title;
    private final int planId;

    BillingSla(String apiValue, String title, int planId) {
        this.apiValue = apiValue;
        this.title = title;
        this.planId = planId;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getTitle() {
        return title;
    }

    public int getPlanId() {
        return planId;
    }

    public static BillingSla fromApiValue(String value) {
        if (value == null) return null;
        for (BillingSla sla : values()) {
            if (sla.apiValue.equalsIgnoreCase(value.trim())) {
                return sla;
            }
        }
        return null;
    }

    public static BillingSla fromPlanId(int planId) {
        for (BillingSla sla : values()) {
            if (sla.planId == planId) {
                return sla;
            }
        }
        return null;
    }
}
