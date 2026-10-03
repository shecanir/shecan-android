package ir.shecan.core.billing;

public interface BillingPurchaseObserver {
    void onMarketplacePurchaseReady(BillingStore store, Object purchase, boolean restoredFromInventory);

    void onMarketplacePurchaseCanceled(BillingStore store, String sku);

    void onMarketplaceBillingError(BillingStore store, String message);
}
