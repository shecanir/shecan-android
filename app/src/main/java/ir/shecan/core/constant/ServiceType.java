package ir.shecan.core.constant;

public enum ServiceType {
    UNKNOWN(0, "رایگان"),
    SAL_E2(47, "برنزی"),
    MAHE_6(48, "نقره‌ای"),
    MAHE_9(49, "طلایی"),
    COMMERCIAL(83, "تجاری");

    private final int id;
    private final String title;

    ServiceType(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    // تبدیل ID به Enum
    public static ServiceType fromId(int id) {
        for (ServiceType d : values()) {
            if (d.id == id) return d;
        }
        return UNKNOWN;
    }
}
