package ir.shecan.data.modelDio;

public class UseDiscountApiInput {
    private final String phone;
    private final String code;
    private final long orderId;
    private final long planPrice;

    public UseDiscountApiInput(String phone, String code, long orderId, long planPrice) {
        this.phone = phone;
        this.code = code;
        this.orderId = orderId;
        this.planPrice = planPrice;
    }

    public String getPhone() {
        return phone;
    }

    public String getCode() {
        return code;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getPlanPrice() {
        return planPrice;
    }
}
