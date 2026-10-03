package ir.shecan.data.modelDio;

public class DiscountApiInput {
    private final String apiKey;
    private final long planPrice;
    private final int planId;
    private final String phone;
    private final String code;
    private final int durationId;

    public DiscountApiInput(String apiKey, long planPrice, int planId, String phone, String code, int durationId) {
        this.apiKey = apiKey;
        this.planPrice = planPrice;
        this.planId = planId;
        this.phone = phone;
        this.code = code;
        this.durationId = durationId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public long getPlanPrice() {
        return planPrice;
    }

    public int getPlanId() {
        return planId;
    }

    public String getPhone() {
        return phone;
    }

    public String getCode() {
        return code;
    }

    public int getDurationId() {
        return durationId;
    }
}
