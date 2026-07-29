package ir.shecan.ui.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ir.shecan.core.util.AppUtils.adjustUIForFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.billing.CafeBazaarBillingManager;
import ir.shecan.core.billing.CafeBazaarBillingProducts;
import ir.shecan.core.billing.BillingHost;
import ir.shecan.core.billing.BillingPurchaseObserver;
import ir.shecan.core.billing.BillingStore;
import ir.shecan.core.billing.MyketBillingManager;
import ir.shecan.core.billing.MyketBillingProducts;
import ir.shecan.core.billing.MarketplacePriceCatalog;
import ir.shecan.core.billing.SitePaymentCallback;
import ir.shecan.core.constant.Constant;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.core.util.DebugJsonLogger;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.PaymentIssueViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.ActivityMainNewBinding;
import ir.shecan.ui.activity.mainActivityUtils.LaunchHandler;
import ir.shecan.ui.activity.mainActivityUtils.TabItem;
import ir.shecan.ui.activity.mainActivityUtils.ThemeManager;
import ir.shecan.ui.activity.mainActivityUtils.VpnManager;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.widget.CustomBottomBar;
import ir.shecan.ui.widget.rateHelper.RatingDialog;
import ir.shecan.ui.widget.rateHelper.RatingManager;

public class MainActivityNew extends AppCompatActivity implements BillingHost {

    private static final int PAYMENT_STATUS_MAX_ATTEMPTS = 6;
    private static final long PAYMENT_STATUS_RETRY_DELAY_MS = 2_000L;

    // Launch Actions
    public static final int LAUNCH_ACTION_NONE = 0;
    public static final int LAUNCH_ACTION_ACTIVATE = 1;
    public static final int LAUNCH_ACTION_DEACTIVATE = 2;
    public static final int LAUNCH_ACTION_SERVICE_DONE = 3;

    // Fragments
    public static final int FRAGMENT_NONE = -1;
    public static final int FRAGMENT_HOME = 0;
    public static final int FRAGMENT_DNS_TEST = 1;
    public static final int FRAGMENT_SETTINGS = 2;
    public static final int FRAGMENT_ABOUT = 3;
    public static final int FRAGMENT_LOG = 6;
    public static final String LAUNCH_ACTION = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_ACTION";
    public static final String LAUNCH_FRAGMENT = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_FRAGMENT";
    public static final String LAUNCH_TAB = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_TAB";
    public static final String LAUNCH_NEED_RECREATE = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_NEED_RECREATE";
    public static final String LAUNCH_PAYMENT_RESULT = "ir.shecan.ui.activity.MainActivityNew.LAUNCH_PAYMENT_RESULT";
    public static final String PAYMENT_RESULT_SUCCESS = "success";
    public static final String PAYMENT_RESULT_FAILED = "failed";
    public static final String LAST_TAB = "LAST_TAB_KEY";

    public int currentTab = 1;
    public boolean configIsChange = false;
    private final Stack<Integer> tabHistory = new Stack<>();

    private static MainActivityNew instance = null;

    private ToolbarFragment currentFragment;

    public ActivityMainNewBinding binding;

    private VpnManager vpnManager;
    private ThemeManager themeManager;
    private MyketBillingManager myketBillingManager;
    private CafeBazaarBillingManager cafeBazaarBillingManager;
    private BillingPurchaseObserver billingPurchaseObserver;
    private final Handler paymentStatusHandler = new Handler(Looper.getMainLooper());
    private AlertDialog paymentProcessingDialog;
    public List<BannerViewModel> bannerUrl;

    public static MainActivityNew getInstance() {
        return instance;
    }

    public ToolbarFragment getCurrentFragment() {
        return currentFragment;
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


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LAST_TAB, currentTab);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        themeManager = new ThemeManager(this);
        themeManager.applyTheme();
        Shecan.getInstance().updateLocale();

        super.onCreate(savedInstanceState);
        TrackingUtils.logEvent(this, TrackingUtils.EVENT_APP_OPEN);

        int selectedTab = 1;
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(LAST_TAB, 1);
        }

        instance = this;
        binding = ActivityMainNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vpnManager = new VpnManager(this);

        requestNotificationPermission();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            FirebaseMessaging.getInstance().subscribeToTopic("afterPushPoleScenarioTopic");
        });

        updateLoginInformation();
        preloadServiceCatalog();
        updateConfigsIfSignedIn();

        setupCustomBottomBar();

        currentTab = selectedTab;
        updateFragment(selectedTab);
        binding.customBar.select(selectedTab);

        LaunchHandler.handle(this, getIntent());
        handlePaymentResult(getIntent());
        onBackPressedHandler();
        vipClickHandler();
        setupMyketBilling();
        setupCafeBazaarBilling();

//        AppSignatureHelper helper = new AppSignatureHelper(this);
//        ArrayList<String> signatures = helper.getAppSignatures();
//
//        for (String signature : signatures) {
//            Log.d("APP_HASH", signature);
//            Toast.makeText(this, signature, Toast.LENGTH_LONG).show();
//        }
    }

    private void checkUserIsLogin() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token == null) {
            startActivity(new Intent(this, AuthorizeActivity.class)
                    .putExtra("isShowBackButton", false));

//            finish();
        }
    }

    private void vipClickHandler() {
        binding.toolbar.vip.setVisibility(VISIBLE);
        binding.toolbar.vip.setOnClickListener(view -> {
            TrackingUtils.logEvent(this, TrackingUtils.EVENT_VIP_CLICK,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "main_toolbar"));
            startActivity(new Intent(this, BillingPlansActivity.class));
        });
    }

    private void preloadServiceCatalog() {
        new AuthApi(this).services(new ApiCallback<ir.shecan.data.modelDto.ServicesViewModel>() {
            @Override
            public void onSuccess(ir.shecan.data.modelDto.ServicesViewModel res, boolean fromCache) {
                if (res != null) {
                    new AppStorage(getApplicationContext()).saveServiceCatalog(res);
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                Log.w("MainActivityNew", "Failed to preload services catalog: " + message);
            }
        });
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
                        // Deliver on your backend first if needed, then call consumeMyketPurchase(purchase).
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
                        // Deliver on your backend first if needed, then call consumeCafeBazaarPurchase(token).
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

    public void consumeCafeBazaarPurchase(String purchaseToken) {
        if (cafeBazaarBillingManager != null) {
            cafeBazaarBillingManager.consumePurchase(purchaseToken);
        }
    }

    private void onBackPressedHandler() {

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                if (!tabHistory.isEmpty()) {
                    int previousTab = tabHistory.pop();
                    currentTab = previousTab;

                    binding.customBar.select(previousTab);
                    updateFragment(previousTab);
                    return;
                }

                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    public void setupCustomBottomBar() {

        String settingTitle = getString(R.string.setting);
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        if (token == null || token.getApiKey() == null)
            settingTitle = getString(R.string.login);

        CustomBottomBar bar = binding.customBar;
        bar.removeItems();

        bar.addItem(getString(R.string.connections), R.drawable.ic_connection_inactive, R.drawable.ic_config_active);
        bar.addItem(getString(R.string.connect), R.drawable.ic_vpn_inactive, R.drawable.ic_vpn_active);
        bar.addItem(settingTitle, R.drawable.ic_setting_inactive, R.drawable.ic_profile_active);

        bar.setOnItemSelected(index -> {
            selectMainTab(index, true);
        });
    }

    public void selectMainTab(int index, boolean addToHistory) {
        if (binding == null) return;

        if (addToHistory && currentTab != index) {
            tabHistory.push(currentTab);
        }

        currentTab = index;
        binding.customBar.select(index);
        logTabSelected(index);
        updateFragment(index);
        applyTabUi(index);
    }

    private void applyTabUi(int index) {
        switch (index) {
            case 0:
                adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
                break;

            case 1:
                adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lightBack));
                break;

            case 2:
                adjustUIForFragment(this, R.color.profileBackground, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.profileBackground));
                break;
        }
    }

    private void logTabSelected(int index) {
        android.os.Bundle params = new android.os.Bundle();
        TrackingUtils.put(params, TrackingUtils.PARAM_TAB_INDEX, index);
        TrackingUtils.put(params, TrackingUtils.PARAM_TAB_NAME, TabItem.fromIndex(index).name().toLowerCase(java.util.Locale.US));
        TrackingUtils.logEvent(this, TrackingUtils.EVENT_TAB_SELECTED, params);
    }


    public void switchFragment(Class fragmentClass, boolean addToBackStack) {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ToolbarFragment fragment =
                (ToolbarFragment) fm.findFragmentByTag(fragmentClass.getName());

        if (fragment == null) {
            try {
                fragment = (ToolbarFragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        ft.replace(R.id.id_content, fragment, fragmentClass.getName());

        if (addToBackStack) {
            boolean exists = false;
            for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                if (fm.getBackStackEntryAt(i).getName().equals(fragmentClass.getName())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) ft.addToBackStack(fragmentClass.getName());
        }

        ft.commitAllowingStateLoss();

        adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
        currentFragment = fragment;
    }


    public void updateFragment(int index) {

        TabItem tab = TabItem.fromIndex(index);

        if (tab.getTitle() != null) {
            binding.toolbar.toolbarLogo.setVisibility(GONE);
            binding.toolbar.toolbarTitle.setVisibility(VISIBLE);
            binding.toolbar.toolbarTitle.setText(tab.getTitle());
        } else {
            binding.toolbar.toolbarLogo.setVisibility(VISIBLE);
            binding.toolbar.toolbarTitle.setVisibility(GONE);
        }

        switch (index) {
            case 0:
                adjustUIForFragment(this, R.color.mainBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBack));
                break;

            case 1:
                adjustUIForFragment(this, R.color.lightBack, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.lightBack));
                break;

            case 2:
                adjustUIForFragment(this, R.color.profileBackground, R.color.mainBack);
                binding.toolbar.appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.profileBackground));
                break;
        }

        switchFragment(tab.getFragmentClass(), false);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (themeManager != null && themeManager.handleOnResume()) {
            recreate();
            return;
        }
        if (myketBillingManager == null) setupMyketBilling();
        if (cafeBazaarBillingManager == null) setupCafeBazaarBilling();
        checkUserIsLogin();
        updateLoginInformation();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        LaunchHandler.handle(this, intent);
        handlePaymentResult(intent);
    }

    @Override
    protected void onDestroy() {
        paymentStatusHandler.removeCallbacksAndMessages(null);
        dismissPaymentProcessingDialog();
        super.onDestroy();
        releaseMarketplaceBilling();
        instance = null;
        currentFragment = null;
        binding = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Shecan.getInstance().updateLocale();
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(ir.shecan.core.util.server.LocaleHelper.onAttach(base));
    }

    public void updateLoginInformation() {
        AppStorage storage = new AppStorage(getApplicationContext());
        if (storage.shouldDeferProfileSync()) return;

        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null) {
            TrackingUtils.setUserId(getApplicationContext(), String.valueOf(token.getId()));
            AuthApi auth = new AuthApi(getApplicationContext());
            auth.me(
                    token.getApiKey(),
                    new ApiCallback<AccountViewModel>() {
                        @Override
                        public void onSuccess(AccountViewModel res, boolean fromCache) {
                            if (res != null && res.getUser() != null) {
                                token.setFirstname(res.getUser().getFirstname());
                                token.setMail(res.getUser().getMail());
                                token.setLastname(res.getUser().getLastname());
                                token.setLogin(res.getUser().getLogin());
                                storage.saveToken(token);
                            }
                        }

                        @Override
                        public void onError(int statusCode, String message) {

                        }
                    }
            );
        }
        setupCustomBottomBar();
        updateConfigsIfSignedIn();
    }

    public void updateConfigsIfSignedIn() {
        AppStorage storage = new AppStorage(getApplicationContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        if (token != null && token.getApiKey() != null) {
            AuthApi auth = new AuthApi(getApplicationContext());
            auth.issues(
                    token.getApiKey(),
                    0, 1000,
                    new ApiCallback<IssuesViewModel>() {
                        @Override
                        public void onSuccess(IssuesViewModel res, boolean fromCache) {
                            DebugJsonLogger.log("ShecanIssuesJson", res);
                            storage.saveIssues(res);
                            boolean selectionChanged = reconcileSelectedService(storage, res);
                            if (selectionChanged && currentTab == TabItem.HOME.getIndex()) {
                                updateFragment(TabItem.HOME.getIndex());
                            }
                        }

                        @Override
                        public void onError(int statusCode, String message) {

                        }
                    }
            );
        }
    }

    private boolean reconcileSelectedService(AppStorage storage, IssuesViewModel issues) {
        if (issues == null || issues.getIssues() == null) return false;
        ServiceItem savedItem = storage.getServiceStatus(ServiceItem.class);
        ServiceItem preferredItem = null;
        ServiceItem refreshedSelectedItem = null;
        boolean savedItemExists = savedItem != null && "0".equals(savedItem.getOrderCode());

        for (IssuesViewModel.IssuesDTO issue : issues.getIssues()) {
            ServiceItem item = ServiceItemMapper.map(getApplicationContext(), issue);
            if (savedItem != null
                    && !item.isClosed()
                    && item.getOrderCode().equals(savedItem.getOrderCode())) {
                savedItemExists = true;
                refreshedSelectedItem = item;
            }
            RequestStatus status = RequestStatus.fromValue(item.statusId);
            if (preferredItem == null
                    && !item.isClosed()
                    && !"0".equals(item.getOrderCode())
                    && (status == RequestStatus.ACTIVE
                    || status == RequestStatus.IN_USE
                    || status == RequestStatus.EXPIRING)) {
                preferredItem = item;
            }
        }

        if (refreshedSelectedItem != null) {
            boolean detailsChanged = selectedServiceDetailsChanged(savedItem, refreshedSelectedItem);
            storage.saveServiceStatus(refreshedSelectedItem);
            return detailsChanged;
        }

        boolean savedItemMissing = savedItem != null && !savedItemExists;
        boolean shouldChooseDefault = savedItem == null
                || savedItemMissing
                || (!storage.isServiceSelectionExplicit()
                && "0".equals(savedItem.getOrderCode()));
        if (!shouldChooseDefault) return false;

        ServiceItem nextItem = preferredItem != null
                ? preferredItem
                : ServiceItemMapper.map(
                        getApplicationContext(),
                        IssuesViewModel.IssuesDTO.createDefault()
                );
        storage.saveServiceStatus(nextItem);
        if (savedItemMissing) {
            storage.clearServiceSelectionExplicit();
        }
        return savedItem == null
                || !nextItem.getOrderCode().equals(savedItem.getOrderCode());
    }

    private boolean selectedServiceDetailsChanged(ServiceItem savedItem, ServiceItem refreshedItem) {
        if (savedItem == null || refreshedItem == null) return savedItem != refreshedItem;
        return !Objects.equals(savedItem.getOrderCode(), refreshedItem.getOrderCode())
                || !Objects.equals(savedItem.getUpdateLink(), refreshedItem.getUpdateLink())
                || !Objects.equals(savedItem.getServiceType(), refreshedItem.getServiceType())
                || !Objects.equals(savedItem.getDurationTitle(), refreshedItem.getDurationTitle())
                || !Objects.equals(savedItem.dueDate, refreshedItem.dueDate)
                || !Objects.equals(savedItem.closedOn, refreshedItem.closedOn)
                || savedItem.statusId != refreshedItem.statusId;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void activateService() {
        if (vpnManager != null) vpnManager.startVpnActivation();
    }

    private void handlePaymentResult(Intent intent) {
        String result = resolvePaymentResult(intent);
        if (result == null) {
            handlePanelPaymentResult(intent);
            return;
        }
        if (binding == null || isFinishing()) return;

        updateConfigsIfSignedIn();
        showResolvedPaymentResult(result);
        intent.removeExtra(LAUNCH_PAYMENT_RESULT);
        intent.setData(null);
    }

    private void showResolvedPaymentResult(String result) {
        if (binding == null || isFinishing()) return;
        dismissPaymentProcessingDialog();
        if (PAYMENT_RESULT_SUCCESS.equals(result)) {
            showPaymentResultDialog(
                    getString(R.string.billing_payment_success_title),
                    getString(R.string.billing_payment_success_message),
                    getString(R.string.billing_payment_start_using),
                    null,
                    R.color.connectionIsActiveColor
            );
        } else if (PAYMENT_RESULT_FAILED.equals(result)) {
            showPaymentResultDialog(
                    getString(R.string.billing_payment_failed_title),
                    getString(R.string.billing_payment_failed_refund_message),
                    getString(R.string.billing_payment_confirm),
                    null,
                    R.color.orangeMain
            );
        }
    }

    private void handlePanelPaymentResult(Intent intent) {
        long paymentId = resolvePanelPaymentId(intent);
        if (paymentId <= 0 || binding == null || isFinishing()) return;

        intent.setData(null);
        showPaymentProcessingDialog();
        checkPanelPaymentStatus(paymentId, 1);
    }

    private void checkPanelPaymentStatus(long paymentId, int attempt) {
        if (binding == null || isFinishing()) return;
        new AuthApi(getApplicationContext()).paymentIssue(
                paymentId,
                new ApiCallback<PaymentIssueViewModel>() {
                    @Override
                    public void onSuccess(PaymentIssueViewModel data, boolean fromCache) {
                        if (binding == null || isFinishing()) return;
                        int statusId = data != null
                                && data.getIssue() != null
                                && data.getIssue().getStatus() != null
                                ? data.getIssue().getStatus().getId()
                                : 0;
                        Log.d("SitePayment", "Payment " + paymentId
                                + " status=" + statusId + " attempt=" + attempt);
                        if (statusId == 20) {
                            updateConfigsIfSignedIn();
                            showResolvedPaymentResult(PAYMENT_RESULT_SUCCESS);
                        } else {
                            retryOrFinishPanelPayment(paymentId, attempt);
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (binding == null || isFinishing()) return;
                        Log.w("SitePayment", "Payment status request failed. id=" + paymentId
                                + " attempt=" + attempt + " code=" + statusCode
                                + " message=" + message);
                        retryOrFinishPanelPayment(paymentId, attempt);
                    }
                }
        );
    }

    private void retryOrFinishPanelPayment(long paymentId, int attempt) {
        if (attempt < PAYMENT_STATUS_MAX_ATTEMPTS) {
            paymentStatusHandler.postDelayed(
                    () -> checkPanelPaymentStatus(paymentId, attempt + 1),
                    PAYMENT_STATUS_RETRY_DELAY_MS
            );
            return;
        }
        updateConfigsIfSignedIn();
        showResolvedPaymentResult(PAYMENT_RESULT_FAILED);
    }

    private long resolvePanelPaymentId(Intent intent) {
        return intent != null ? SitePaymentCallback.resolvePaymentId(intent.getData()) : 0L;
    }

    private String resolvePaymentResult(Intent intent) {
        if (intent == null) return null;

        String extraResult = intent.getStringExtra(LAUNCH_PAYMENT_RESULT);
        String result = normalizePaymentResult(extraResult);
        if (result != null) return result;

        Uri data = intent.getData();
        if (data == null) return null;

        return SitePaymentCallback.resolveResult(data);
    }

    private String normalizePaymentResult(String value) {
        return SitePaymentCallback.normalizeResult(value);
    }

    private void showPaymentResultDialog(String title, String message, String actionText, Runnable action, int buttonColorRes) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_result, null, false);
        TextView titleView = dialogView.findViewById(R.id.txtPaymentResultTitle);
        TextView messageView = dialogView.findViewById(R.id.txtPaymentResultMessage);
        Button actionButton = dialogView.findViewById(R.id.btnPaymentResultAction);

        titleView.setText(title);
        messageView.setText(message);
        actionButton.setText(actionText);
        actionButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, buttonColorRes)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        actionButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (action != null) action.run();
        });
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels
                    - (int) (48 * getResources().getDisplayMetrics().density);
            window.setLayout(Math.max(width, 0), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showPaymentProcessingDialog() {
        if (binding == null || isFinishing()) return;
        if (paymentProcessingDialog != null && paymentProcessingDialog.isShowing()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_processing, null, false);
        paymentProcessingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        paymentProcessingDialog.show();

        Window window = paymentProcessingDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = getResources().getDisplayMetrics().widthPixels
                    - (int) (48 * getResources().getDisplayMetrics().density);
            window.setLayout(Math.max(width, 0), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void dismissPaymentProcessingDialog() {
        if (paymentProcessingDialog == null) return;
        if (paymentProcessingDialog.isShowing()) {
            paymentProcessingDialog.dismiss();
        }
        paymentProcessingDialog = null;
    }

    public void applyThemeForRecreate() {
        if (themeManager != null) themeManager.applyTheme();
    }
}
