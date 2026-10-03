package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

import ir.shecan.R;
import ir.shecan.core.billing.BillingStore;
import ir.shecan.core.billing.BillingHost;
import ir.shecan.core.billing.BillingPurchaseObserver;
import ir.shecan.core.billing.CafeBazaarBillingManager;
import ir.shecan.core.billing.CafeBazaarBillingProducts;
import ir.shecan.core.billing.MyketBillingManager;
import ir.shecan.core.billing.MyketBillingProducts;
import ir.shecan.core.billing.MarketplacePriceCatalog;
import ir.shecan.core.constant.Constant;
import ir.shecan.databinding.ActivityBillingPlansBinding;
import ir.shecan.ui.fragment.refactor.BillingPlansFragment;

public class BillingPlansActivity extends AppCompatActivity implements BillingHost {

    public static final String EXTRA_PREFILL_SLA = "prefill_sla";
    public static final String EXTRA_PREFILL_PERIOD = "prefill_period";
    public static final String EXTRA_RENEWAL_ORDER_ID = "renewal_order_id";

    private ActivityBillingPlansBinding binding;
    private MyketBillingManager myketBillingManager;
    private CafeBazaarBillingManager cafeBazaarBillingManager;
    private BillingPurchaseObserver billingPurchaseObserver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBillingPlansBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adjustUi();
        MainActivityNew mainActivity = MainActivityNew.getInstance();
        if (mainActivity != null) {
            mainActivity.releaseMarketplaceBilling();
        }
        setupMyketBilling();
        setupCafeBazaarBilling();

        if (savedInstanceState == null) {
            BillingPlansFragment fragment = new BillingPlansFragment();
            Bundle args = new Bundle();
            args.putString(BillingPlansFragment.ARG_PREFILL_SLA, getIntent().getStringExtra(EXTRA_PREFILL_SLA));
            args.putString(BillingPlansFragment.ARG_PREFILL_PERIOD, getIntent().getStringExtra(EXTRA_PREFILL_PERIOD));
            args.putLong(BillingPlansFragment.ARG_RENEWAL_ORDER_ID, getIntent().getLongExtra(EXTRA_RENEWAL_ORDER_ID, 0L));
            fragment.setArguments(args);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.billingContainer, fragment)
                    .commit();
        }
    }

    private void adjustUi() {
        binding.toolbar.back.setVisibility(VISIBLE);
        binding.toolbar.back.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        adjustUIForFragment(this, R.color.profileBackground, R.color.mainBack);
        binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));

        binding.toolbar.toolbarLogo.setVisibility(GONE);
        binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
        binding.toolbar.toolbarTitle.setText(R.string.title_billing_plans);

        binding.toolbar.vip.setVisibility(GONE);
    }

    private void setupMyketBilling() {
        if (!Constant.IsMyketMode || !MyketBillingProducts.hasAnySku()) return;

        myketBillingManager = new MyketBillingManager(this);
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
                        Log.d("MyketBilling", "Consumable purchase is ready: " + purchase);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.MYKET, purchase, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onNonConsumablePurchaseReady(Object purchase, boolean restoredFromInventory) {
                        Log.d("MyketBilling", "Non-consumable purchase is ready: " + purchase);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.MYKET, purchase, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onPurchaseConsumed(Object purchase) {
                        Log.d("MyketBilling", "Purchase consumed: " + purchase);
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

        cafeBazaarBillingManager = new CafeBazaarBillingManager(this);
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
                        Log.d("CafeBazaarBilling", "Consumable purchase is ready: " + purchaseInfo);
                        if (billingPurchaseObserver != null) {
                            billingPurchaseObserver.onMarketplacePurchaseReady(BillingStore.CAFE_BAZAAR, purchaseInfo, restoredFromInventory);
                        }
                    }

                    @Override
                    public void onNonConsumablePurchaseReady(@NonNull Object purchaseInfo, boolean restoredFromInventory) {
                        Log.d("CafeBazaarBilling", "Non-consumable purchase is ready: " + purchaseInfo);
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
                        Log.d("CafeBazaarBilling", "Purchase consumed: " + purchaseToken);
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
    public void setBillingPurchaseObserver(BillingPurchaseObserver billingPurchaseObserver) {
        this.billingPurchaseObserver = billingPurchaseObserver;
        if (billingPurchaseObserver != null && myketBillingManager != null) {
            myketBillingManager.redeliverOwnedPurchases();
        }
    }

    @Override
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

    @Override
    public void launchMyketPurchase(String sku, Long renewalOrderId) {
        if (myketBillingManager != null) {
            myketBillingManager.launchPurchaseFlow(this, sku, renewalOrderId);
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
            cafeBazaarBillingManager.launchPurchaseFlow(getActivityResultRegistry(), sku, renewalOrderId);
        }
    }

    @Override
    protected void onDestroy() {
        if (myketBillingManager != null) myketBillingManager.dispose();
        if (cafeBazaarBillingManager != null) cafeBazaarBillingManager.dispose();
        billingPurchaseObserver = null;
        binding = null;
        super.onDestroy();
    }
}
