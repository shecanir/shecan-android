package ir.shecan.data.modelDto;

public class DiscountViewModel {
    private Boolean status;
    private String message;
    private Long price;
    private Long planPrice;
    private Long finalPrice;
    private Long payable;
    private Long discountedPrice;
    private Long discount;
    private Long discountAmount;

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(status);
    }

    public Boolean getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Long getFinalPrice() {
        return firstNonNull(finalPrice, payable, discountedPrice, price, planPrice);
    }

    public Long getDiscount() {
        return firstNonNull(discount, discountAmount);
    }

    private Long firstNonNull(Long... values) {
        for (Long value : values) {
            if (value != null) return value;
        }
        return null;
    }
}
