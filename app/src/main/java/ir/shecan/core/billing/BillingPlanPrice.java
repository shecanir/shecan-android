package ir.shecan.core.billing;

import ir.shecan.data.modelDto.PriceViewModel;

public class BillingPlanPrice {
    private final BillingPlan plan;
    private PriceViewModel price;
    private boolean loading;
    private String errorMessage;
    private Long discountedPrice;
    private Long discountAmount;
    private String discountCode;
    private String discountMessage;
    private boolean discountLoading;
    private Long marketplaceTotalPrice;

    public BillingPlanPrice(BillingPlan plan) {
        this.plan = plan;
    }

    public BillingPlan getPlan() {
        return plan;
    }

    public PriceViewModel getPrice() {
        return price;
    }

    public void setPrice(PriceViewModel price) {
        this.price = price;
        this.errorMessage = null;
        this.discountedPrice = null;
        this.discountAmount = null;
        this.discountCode = null;
        this.discountMessage = null;
    }

    public void setMarketplaceTotalPrice(long marketplaceTotalPrice) {
        this.marketplaceTotalPrice = Math.max(0L, marketplaceTotalPrice);
        setPrice(PriceViewModel.fromPrice(this.marketplaceTotalPrice));
    }

    public Long getMarketplaceTotalPrice() {
        return marketplaceTotalPrice;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDiscountedPrice() {
        return discountedPrice;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public String getDiscountMessage() {
        return discountMessage;
    }

    public boolean isDiscountLoading() {
        return discountLoading;
    }

    public void setDiscountLoading(boolean discountLoading) {
        this.discountLoading = discountLoading;
    }

    public void applyDiscount(String code, Long discountedPrice, Long discountAmount, String message) {
        this.discountCode = code;
        this.discountedPrice = discountedPrice;
        this.discountAmount = discountAmount;
        this.discountMessage = message;
    }

    public void clearDiscount(String message) {
        this.discountCode = null;
        this.discountedPrice = null;
        this.discountAmount = null;
        this.discountMessage = message;
    }

    public long getEffectivePrice() {
        if (discountedPrice != null) return discountedPrice;
        return price != null ? price.getSafePrice() : 0L;
    }

    public long getEffectivePrice(BillingStore store) {
        return getTotalPrice(store);
    }

    public long getServicePrice(BillingStore store) {
        if (store != BillingStore.SITE && marketplaceTotalPrice != null) {
            return Math.round(marketplaceTotalPrice / 1.1d);
        }
        long basePrice = getEffectivePrice();
        double priceWithoutTax = basePrice / 1.1d;
        double adjustedPrice = store == BillingStore.SITE
                ? priceWithoutTax
                : priceWithoutTax * 1.43d;
        return roundUp((long) Math.ceil(adjustedPrice), 10_000L);
    }

    public long getTaxPrice(BillingStore store) {
        if (store != BillingStore.SITE && marketplaceTotalPrice != null) {
            return Math.round(getServicePrice(store) * 0.1d);
        }
        return Math.round(getServicePrice(store) * 0.1d);
    }

    public long getTotalPrice(BillingStore store) {
        if (store != BillingStore.SITE && marketplaceTotalPrice != null) {
            return marketplaceTotalPrice;
        }
        return getServicePrice(store) + getTaxPrice(store);
    }

    private long roundUp(long value, long step) {
        if (value <= 0L || step <= 0L) return Math.max(0L, value);
        return ((value + step - 1L) / step) * step;
    }
}
