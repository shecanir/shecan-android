package ir.shecan.core.billing;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import ir.shecan.core.constant.Constant;

/** Owns marketplace connections and purchase delivery for a single activity. */
public final class MarketplaceBillingController implements BillingHost {
    private final AppCompatActivity activity;
    private MyketBillingManager myketBillingManager;
    private CafeBazaarBillingManager cafeBazaarBillingManager;
    private BillingPurchaseObserver billingPurchaseObserver;

    public MarketplaceBillingController(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void start() {
        if (myketBillingManager == null) setupMyketBilling();
        if (cafeBazaarBillingManager == null) setupCafeBazaarBilling();
    }

    public void setBillingPurchaseObserver(BillingPurchaseObserver billingPurchaseObserver) {
        this.billingPurchaseObserver = billingPurchaseObserver;
        if (billingPurchaseObserver != null && myketBillingManager != null) {
            myketBillingManager.redeliverOwnedPurchases();
        }
    }

    public boolean isBillingReadyForStore(BillingStore store) {
        switch (store) {
            case CAFE_BAZAAR:
                return cafeBazaarBillingManager != null && cafeBazaarBillingManager.isReady();
            case MYKET:
                return myketBillingManager != null && myketBillingManager.isReady();
            case SITE:
            default:
                return true;
        }
    }

    @Override
    public void refreshMarketplacePrices(BillingStore store) {
        MarketplacePriceCatalog.clear(store);
        if (store == BillingStore.CAFE_BAZAAR && cafeBazaarBillingManager != null) {
            cafeBazaarBillingManager.querySkuDetails();
        } else if (store == BillingStore.MYKET && myketBillingManager != null) {
            myketBillingManager.querySkuDetails();
        }
    }

    public void releaseMarketplaceBilling() {
        billingPurchaseObserver = null;
        if (myketBillingManager != null) {
            myketBillingManager.dispose();
            myketBillingManager = null;
        }
        if (cafeBazaarBillingManager != null) {
            cafeBazaarBillingManager.dispose();
            cafeBazaarBillingManager = null;
        }
    }


    private void setupMyketBilling() {
        if (!Constant.IsMyketMode || !MyketBillingProducts.hasAnySku()) return;

        myketBillingManager = new MyketBillingManager(activity);
        myketBillingManager.startSetup(
                MyketBillingProducts.consumableSkus(),
                MyketBillingProducts.nonConsumableSkus(),
                new MyketBillingManager.Listener() {
                    @Override
                    public void onBillingReady() {
                        Log.d("MyketBilling", "Myket billing is ready.");
                    }

                    @Override
                    public void onBillingUnavailable(String message) {
                        Log.w("MyketBilling", message);
                    }

                    @Override
                    public void onSkuDetailsLoaded(List<?> skuDetails) {
                        Log.d("MyketBilling", "Loaded Myket sku details: " + skuDetails.size());
                        for (Object detail : skuDetails) {
                            if (detail instanceof ir.myket.billingclient.util.SkuDetails) {
                                ir.myket.billingclient.util.SkuDetails sku =
                                        (ir.myket.billingclient.util.SkuDetails) detail;
                                MarketplacePriceCatalog.putPrice(
                                        BillingStore.MYKET,
                                        sku.getSku(),
                                        sku.getPrice()
                                );
                            }
                        }
                    }

                    @Override
                    public void onConsumablePurchaseReady(Object purchase, boolean restoredFromInventory) {
                        Log.d("MyketBilling", "Consumable purchase is ready: ");
                        // Deliver on your backend first if needed, then call consumeMyketPurchase(purchase).
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.MYKET, purchase, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onNonConsumablePurchaseReady(Object purchase, boolean restoredFromInventory) {
                        Log.d("MyketBilling", "Non-consumable purchase is ready: ");
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.MYKET, purchase, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onPurchaseConsumed(Object purchase) {
                        Log.d("MyketBilling", "Purchase consumed: ");
                    }

                    @Override
                    public void onBillingError(String message) {
                        Log.e("MyketBilling", message);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplaceBillingError(BillingStore.MYKET, message);
                        }
                    }
                }
        );
    }

    private void setupCafeBazaarBilling() {
        if (!Constant.IsCafeBazaarMode || !CafeBazaarBillingProducts.hasAnySku()) return;

        cafeBazaarBillingManager = new CafeBazaarBillingManager(activity);
        cafeBazaarBillingManager.startSetup(
                CafeBazaarBillingProducts.consumableSkus(),
                CafeBazaarBillingProducts.nonConsumableSkus(),
                new CafeBazaarBillingManager.Listener() {
                    @Override
                    public void onBillingReady() {
                        Log.d("CafeBazaarBilling", "Cafe Bazaar billing is ready.");
                    }

                    @Override
                    public void onBillingUnavailable(@NonNull String message) {
                        Log.w("CafeBazaarBilling", message);
                    }

                    @Override
                    public void onBillingDisconnected() {
                        Log.d("CafeBazaarBilling", "Cafe Bazaar billing disconnected.");
                    }

                    @Override
                    public void onInAppSkuDetailsLoaded(@NonNull List<?> skuDetails) {
                        Log.d("CafeBazaarBilling", "Loaded Cafe Bazaar in-app sku details: " + skuDetails.size());
                        for (Object detail : skuDetails) {
                            if (detail instanceof ir.cafebazaar.poolakey.entity.SkuDetails) {
                                ir.cafebazaar.poolakey.entity.SkuDetails sku =
                                        (ir.cafebazaar.poolakey.entity.SkuDetails) detail;
                                MarketplacePriceCatalog.putPrice(
                                        BillingStore.CAFE_BAZAAR,
                                        sku.getSku(),
                                        sku.getPrice()
                                );
                            }
                        }
                    }

                    @Override
                    public void onPurchaseFlowBegan(@NonNull String sku) {
                        Log.d("CafeBazaarBilling", "Purchase flow began: " + sku);
                    }

                    @Override
                    public void onConsumablePurchaseReady(@NonNull Object purchaseInfo, boolean restoredFromInventory) {
                        Log.d("CafeBazaarBilling", "Consumable purchase is ready: ");
                        // Deliver on your backend first if needed, then call consumeCafeBazaarPurchase(token).
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.CAFE_BAZAAR, purchaseInfo, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onNonConsumablePurchaseReady(@NonNull Object purchaseInfo, boolean restoredFromInventory) {
                        Log.d("CafeBazaarBilling", "Non-consumable purchase is ready: ");
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.CAFE_BAZAAR, purchaseInfo, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onPurchaseCanceled(@NonNull String sku) {
                        Log.d("CafeBazaarBilling", "Purchase canceled: " + sku);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseCanceled(BillingStore.CAFE_BAZAAR, sku);
                        }
                    }

                    @Override
                    public void onPurchaseConsumed(@NonNull String purchaseToken) {
                        Log.d("CafeBazaarBilling", "Purchase consumed: ");
                    }

                    @Override
                    public void onBillingError(@NonNull String message) {
                        Log.e("CafeBazaarBilling", message);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplaceBillingError(BillingStore.CAFE_BAZAAR, message);
                        }
                    }
                }
        );
    }

    @Override
    public void launchMyketPurchase(String sku, Long renewalOrderId) {
        if (myketBillingManager != null) {
            myketBillingManager.launchPurchaseFlow(activity, sku, renewalOrderId);
        }
    }

    @Override
    public void consumeMyketPurchase(Object purchase) {
        if (myketBillingManager != null) {
            myketBillingManager.consumePurchase(purchase);
        }
    }

    @Override
    public void launchCafeBazaarPurchase(String sku, Long renewalOrderId) {
        if (cafeBazaarBillingManager != null) {
            cafeBazaarBillingManager.launchPurchaseFlow(activity.getActivityResultRegistry(), sku, renewalOrderId);
        }
    }

    public void consumeCafeBazaarPurchase(String purchaseToken) {
        if (cafeBazaarBillingManager != null) {
            cafeBazaarBillingManager.consumePurchase(purchaseToken);
        }
    }

}
