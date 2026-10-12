package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import ir.shecan.R;
import ir.shecan.core.billing.BillingStore;
import ir.shecan.core.billing.MarketplaceBillingController;
import ir.shecan.core.billing.BillingHost;
import ir.shecan.core.billing.BillingPurchaseObserver;
import ir.shecan.databinding.ActivityBillingPlansBinding;
import ir.shecan.ui.fragment.refactor.BillingPlansFragment;

public class BillingPlansActivity extends AppCompatActivity implements BillingHost {

    public static final String EXTRA_PREFILL_SLA = "prefill_sla";
    public static final String EXTRA_PREFILL_PERIOD = "prefill_period";
    public static final String EXTRA_RENEWAL_ORDER_ID = "renewal_order_id";

    private ActivityBillingPlansBinding binding;

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
        getMarketplaceBilling().start();

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

    private MarketplaceBillingController marketplaceBilling;

    private MarketplaceBillingController getMarketplaceBilling() {
        if (marketplaceBilling == null) marketplaceBilling = new MarketplaceBillingController(this);
        return marketplaceBilling;
    }

    @Override
    public void setBillingPurchaseObserver(BillingPurchaseObserver observer) {
        getMarketplaceBilling().setBillingPurchaseObserver(observer);
    }

    @Override
    public boolean isBillingReadyForStore(BillingStore store) {
        return getMarketplaceBilling().isBillingReadyForStore(store);
    }

    @Override
    public void refreshMarketplacePrices(BillingStore store) {
        getMarketplaceBilling().refreshMarketplacePrices(store);
    }

    @Override
    public void launchMyketPurchase(String sku, Long renewalOrderId) {
        getMarketplaceBilling().launchMyketPurchase(sku, renewalOrderId);
    }

    @Override
    public void consumeMyketPurchase(Object purchase) {
        getMarketplaceBilling().consumeMyketPurchase(purchase);
    }

    @Override
    public void launchCafeBazaarPurchase(String sku, Long renewalOrderId) {
        getMarketplaceBilling().launchCafeBazaarPurchase(sku, renewalOrderId);
    }

    public void releaseMarketplaceBilling() {
        if (marketplaceBilling != null) marketplaceBilling.releaseMarketplaceBilling();
    }

    @Override
    protected void onDestroy() {
        releaseMarketplaceBilling();
        binding = null;
        super.onDestroy();
    }
}
