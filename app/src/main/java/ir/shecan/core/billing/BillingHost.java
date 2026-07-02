package ir.shecan.core.billing;

public interface BillingHost {
    void setBillingPurchaseObserver(BillingPurchaseObserver billingPurchaseObserver);

    boolean isBillingReadyForStore(BillingStore store);

    void refreshMarketplacePrices(BillingStore store);

    void launchMyketPurchase(String sku, Long renewalOrderId);

    void consumeMyketPurchase(Object purchase);

    void launchCafeBazaarPurchase(String sku, Long renewalOrderId);
}
