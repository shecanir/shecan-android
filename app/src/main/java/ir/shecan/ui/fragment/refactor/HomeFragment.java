package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.BuildConfig;
import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.billing.BillingPeriod;
import ir.shecan.core.billing.BillingSla;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.core.service.BaseApiResponseListener;
import ir.shecan.core.service.ConnectionStatusApiListener;
import ir.shecan.core.service.CoreApiResponseListener;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.DynamicBannerRequestFactory;
import ir.shecan.core.util.DynamicDialogRequestFactory;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDio.DialogMatchApiInput;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.DynamicDialogViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.FragmentHomeBinding;
import ir.shecan.ui.activity.BillingPlansActivity;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.dialog.ContactSupportDialog;
import ir.shecan.ui.dialog.DynamicAppDialog;
import ir.shecan.ui.dialog.RenewalDialog;
import ir.shecan.ui.dialog.UpdateDialog;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.widget.rateHelper.RatingDialog;
import ir.shecan.ui.widget.rateHelper.RatingManager;

public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    private FragmentHomeBinding binding;
    private boolean isUpdateVersionCheck = false;
    private boolean hasBanner = false;
    private ScheduledExecutorService scheduler;
    private long dynamicIpCheckDeadlineMs = 0L;
    private ServiceItem currentServiceItem;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingDynamicDialogRunnable;
    MainActivityNew activity;

    private static final String TAG = "HomeFragment";
    private static final long DYNAMIC_IP_CHECK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(70);
    private static final long DYNAMIC_IP_CHECK_RETRY_DELAY_SECONDS = 10;
    private static boolean testSiteDirectUpdateDialogShown = false;
    private static final Object DYNAMIC_DIALOG_SESSION_LOCK = new Object();
    private static boolean dynamicDialogRequested = false;
    private static boolean dynamicDialogRequestFinished = false;
    private static boolean dynamicDialogDisplayed = false;
    private static DynamicDialogViewModel dynamicDialogSessionModel = null;
    private static String dynamicDialogSessionMobile = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        activity = (MainActivityNew) getActivity();


        root.addOnLayoutChangeListener((v, left, top, right, bottom,
                                        oldLeft, oldTop, oldRight, oldBottom) -> {

            int width = v.getWidth();
            int height = v.getHeight();

            boolean hide = height < width * 1.5f;

            binding.bannerSlider.setVisibility(hide || !hasBanner ? GONE : VISIBLE);
            binding.constraintLayout.setVisibility(hide ? GONE : VISIBLE);
        });

        setupDonatePadding();

        if (activity.bannerUrl == null) updateBanner();
        else handleBannerImage(activity.bannerUrl);

        AppStorage appStorage = new AppStorage(getContext());
        currentServiceItem = appStorage.getServiceStatus(ServiceItem.class);
        currentServiceItem = resolveCurrentServiceItem(appStorage, currentServiceItem);

//        ServiceItem finalServiceItem = serviceItem;

        binding.vpnButton.setOnClickListener(v -> {
            Shecan app = (Shecan) requireContext().getApplicationContext();

            if (ShecanVpnService.isActivated()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_DISCONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button"));
                cancelDynamicIpStatusCheck();
                app.getVpnState().setValue(0);
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
            } else if (binding.vpnButton.isLoading()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_DISCONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button_loading"));
                cancelDynamicIpStatusCheck();
                app.getVpnState().setValue(0);
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
            } else if (shouldOpenRenewalBeforeConnect()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BILLING_PURCHASE_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_expired_service"));
                openBillingPlansForCurrentService();
            } else {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECT_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_button"));
                app.connectVpn(requireContext(), HomeFragment.this);
            }
        });

        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;

            switch (state) {
                case 0:
                    binding.vpnButton.showLoading(false);
                    binding.statusTv.setVisibility(GONE);
                    if (app.getVpnStatus().getValue() != null && !app.getVpnStatus().getValue().isEmpty()) {
                        ToastManager.show(getContext(), app.getVpnStatus().getValue());
                        app.getVpnStatus().setValue("");
                    }
                    break;
                case 1:
                    binding.vpnButton.showLoading(true);
                    binding.statusTv.setVisibility(GONE);
                    break;
                case 2:
                    binding.vpnButton.setConnected(true);
                    binding.statusTv.setVisibility(VISIBLE);
                    break;
            }
        });

        app.getProActivatedEvent().observe(getViewLifecycleOwner(), activated -> {
            if (activated == null || !activated) return;

            startActivity(new Intent(requireActivity(), MainActivityNew.class)
                    .putExtra(MainActivityNew.LAUNCH_ACTION,
                            MainActivityNew.LAUNCH_ACTION_ACTIVATE));

            // consume event
            app.getProActivatedEvent().setValue(false);
        });


        binding.chooseConfig.setOnClickListener(v -> {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_SERVICE_DETAILS_CLICK,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_choose_config"));
            activity.updateFragment(0);
            activity.binding.customBar.select(0);
        });


        if (currentServiceItem == null) {
            currentServiceItem = new ServiceItem("", ContextCompat.getString(getContext(), R.string.free), "", "", "", 0, 0, IssuesViewModel.IssuesDTO.createDefault());
        }
        try {
            binding.servicePanel.setStatus(currentServiceItem);
        } catch (ParseException ignored) {

        }

        binding.servicePanel.setOnClickListener(view -> {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_SERVICE_DETAILS_CLICK,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "home_service_panel"));
            activity.updateFragment(0);
            activity.binding.customBar.select(0);
        });

        return root;
    }

    private ServiceItem resolveCurrentServiceItem(AppStorage storage, ServiceItem savedItem) {
        if (storage == null || !isFreeService(savedItem)) {
            return savedItem;
        }

        IssuesViewModel issues = storage.getIssue(IssuesViewModel.class);
        if (issues == null || issues.getIssues() == null) {
            return savedItem;
        }

        for (IssuesViewModel.IssuesDTO issue : issues.getIssues()) {
            ServiceItem item = ServiceItemMapper.map(getContext(), issue);
            if (isActivePaidService(item)) {
                storage.saveServiceStatus(item);
                return item;
            }
        }
        return savedItem;
    }

    private boolean isActivePaidService(ServiceItem item) {
        if (item == null || isFreeService(item)) return false;
        RequestStatus status = RequestStatus.fromValue(item.statusId);
        return status == RequestStatus.ACTIVE
                || status == RequestStatus.IN_USE
                || status == RequestStatus.EXPIRING;
    }

    private boolean isFreeService(ServiceItem item) {
        return item == null || "0".equals(item.getOrderCode());
    }

    private boolean shouldOpenRenewalBeforeConnect() {
        RequestStatus status = currentServiceItem != null
                ? RequestStatus.fromValue(currentServiceItem.statusId)
                : null;
        if (status == RequestStatus.SUPPORT_FINISHED
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_RENEW
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_ACTIVATION
                || status == RequestStatus.SUSPENDED) {
            return true;
        }
        return isDueDateExpired(currentServiceItem != null ? currentServiceItem.dueDate : null);
    }

    private boolean isDueDateExpired(String dueDate) {
        try {
            if (dueDate == null || dueDate.trim().isEmpty()) return false;
            String normalized = dueDate.trim();
            if (normalized.contains("T")) normalized = normalized.substring(0, normalized.indexOf("T"));
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date date = sdf.parse(normalized);
            if (date == null) return false;
            return date.getTime() < System.currentTimeMillis();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void openBillingPlansForCurrentService() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), BillingPlansActivity.class);

        BillingSla sla = BillingSla.fromPlanId(currentServiceItem != null && currentServiceItem.cfServiceType != null
                ? currentServiceItem.cfServiceType
                : -1);
        BillingPeriod period = BillingPeriod.fromDurationId(currentServiceItem != null && currentServiceItem.cfDuration != null
                ? currentServiceItem.cfDuration
                : -1);

        if (sla != null) {
            intent.putExtra(BillingPlansActivity.EXTRA_PREFILL_SLA, sla.getApiValue());
        }
        if (period != null) {
            intent.putExtra(BillingPlansActivity.EXTRA_PREFILL_PERIOD, period.getApiValue());
        }
        long orderId = currentServiceItem != null ? currentServiceItem.id : 0L;
        if (orderId > 0L) {
            intent.putExtra(BillingPlansActivity.EXTRA_RENEWAL_ORDER_ID, orderId);
        }

        startActivity(intent);
    }

    private boolean isUpdateLinkMode(ServiceItem serviceItem) {
        if (serviceItem == null) {
            return false;
        }
        if (serviceItem.getUpdateLink() == null) {
            return false;
        }
        return !serviceItem.getUpdateLink().isEmpty();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void checkStatus() {
    }

    @Override
    public void onResume() {
        super.onResume();
        maybeShowTestSiteDirectUpdateDialog();
        fetchData();
        ((MainActivityNew) getActivity()).binding.customBar.select(1);

        if (activity.configIsChange) {
            activity.configIsChange = false;

            Shecan app = (Shecan) requireContext().getApplicationContext();

            if (ShecanVpnService.isActivated()) {
                app.pendingReconnect = true;
                Shecan.deactivateService(requireContext());
                app.waitForDeactivateThenReconnect(requireContext());
            } else {
                app.connectVpn(requireContext(), HomeFragment.this);
            }
        }

    }

    @Override
    public void onDestroyView() {
        cancelPendingDynamicDialog();
        super.onDestroyView();

        if (binding != null) binding.bannerSlider.stop();
        cancelDynamicIpStatusCheck();
        binding = null;
    }

    private void setupDonatePadding() {
//        final LinearLayout donate = binding.linearLayoutDonate;
//        if (!ViewConfiguration.get(requireContext()).hasPermanentMenuKey()) {
//            ViewCompat.setOnApplyWindowInsetsListener(donate, (v, insets) -> {
//                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
//                v.setPadding(0, 0, 0, navBarInsets.bottom);
//                return insets;
//            });
//        }
    }

    private void fetchData() {
        if (!isAdded()) return;

        Shecan.ShecanInfo.fetchData(requireContext(), new BaseApiResponseListener() {
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                maybeShowDynamicDialog();
                loadBanner();
            }

            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                if (!isUpdateVersionCheck) {
                    checkIsUpdateAvailable();
                    isUpdateVersionCheck = true;
                }
                maybeShowDynamicDialog();
                loadBanner();
            }
        });
    }

    private void loadBanner() {
        if (!isAdded()) return;
        updateBanner();
    }

    private void maybeShowDynamicDialog() {
        if (!isAdded()) return;

        synchronized (DYNAMIC_DIALOG_SESSION_LOCK) {
            if (dynamicDialogDisplayed) return;
            if (dynamicDialogRequestFinished) {
                scheduleDynamicDialog(dynamicDialogSessionModel, dynamicDialogSessionMobile);
                return;
            }
            if (dynamicDialogRequested) return;
            dynamicDialogRequested = true;
        }

        AuthApi auth = new AuthApi(requireContext());
        DialogMatchApiInput input = DynamicDialogRequestFactory.fromStorage(requireContext());
        auth.dialogMatch(input, new ApiCallback<DynamicDialogViewModel>() {
            @Override
            public void onSuccess(DynamicDialogViewModel dialog, boolean fromCache) {
                synchronized (DYNAMIC_DIALOG_SESSION_LOCK) {
                    dynamicDialogRequestFinished = true;
                    dynamicDialogSessionModel = dialog;
                    dynamicDialogSessionMobile = input.getMobileNumber();
                }
                if (!isAdded()) return;
                scheduleDynamicDialog(dialog, input.getMobileNumber());
            }

            @Override
            public void onError(int statusCode, String message) {
                synchronized (DYNAMIC_DIALOG_SESSION_LOCK) {
                    dynamicDialogRequestFinished = true;
                    dynamicDialogSessionModel = null;
                    dynamicDialogSessionMobile = "";
                }
                if (!isAdded()) return;
                Log.e(TAG, "Dynamic dialog match failed: " + statusCode + " " + message);
            }
        });
    }

    private void checkIsUpdateAvailable() {
        if (!isAdded()) return;
        if (BuildConfig.TEST_SITE_DIRECT_UPDATE && "Site".equals(BuildConfig.STORE)) {
            return;
        }

        boolean isForce = false;
        String currentVersion = AppUtils.getVersionName(requireActivity());
        String minVersion = Shecan.ShecanInfo.getMinVersion();
        String latestVersion = Shecan.ShecanInfo.getCurrentVersion();

        if (AppUtils.compareVersionNames(minVersion, currentVersion) == 1) {
            isForce = true;
        }
        if (AppUtils.compareVersionNames(latestVersion, currentVersion) == 1) {
            new UpdateDialog(requireActivity()).show(isForce);
        }
    }

    private void maybeShowTestSiteDirectUpdateDialog() {
        if (!shouldShowTestSiteDirectUpdate()) return;
        testSiteDirectUpdateDialogShown = true;
        isUpdateVersionCheck = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                new UpdateDialog(requireActivity()).show(false);
            }
        }, 300);
    }

    private boolean shouldShowTestSiteDirectUpdate() {
        return BuildConfig.TEST_SITE_DIRECT_UPDATE
                && "Site".equals(BuildConfig.STORE)
                && !testSiteDirectUpdateDialogShown;
    }

    private void scheduleDynamicDialog(DynamicDialogViewModel dialog, String mobileNumber) {
        cancelPendingDynamicDialog();
        if (dialog == null) return;
        long appStartedElapsedMs = Shecan.getAppStartedElapsedMs();
        long dialogDueElapsedMs = appStartedElapsedMs > 0L
                ? appStartedElapsedMs + dialog.getTimeToShowMs()
                : SystemClock.elapsedRealtime() + dialog.getTimeToShowMs();
        long delayMs = Math.max(0L, dialogDueElapsedMs - SystemClock.elapsedRealtime());
        pendingDynamicDialogRunnable = () -> {
            pendingDynamicDialogRunnable = null;
            if (!isAdded()) return;
            boolean shown = new DynamicAppDialog(requireActivity(), mobileNumber).showIfValid(dialog);
            if (shown || !dialog.isActive() || !dialog.hasContent()) {
                synchronized (DYNAMIC_DIALOG_SESSION_LOCK) {
                    dynamicDialogDisplayed = true;
                }
            }
        };
        mainHandler.postDelayed(pendingDynamicDialogRunnable, delayMs);
    }

    private void cancelPendingDynamicDialog() {
        if (pendingDynamicDialogRunnable == null) return;
        mainHandler.removeCallbacks(pendingDynamicDialogRunnable);
        pendingDynamicDialogRunnable = null;
    }

    @Override
    public void onSuccess(String response) {
        if (!isAdded()) return;
        if (ShecanVpnService.isDynamicIPMode()) {
            waitForDynamicIpActivation();
        } else {
            startVpnAfterConnectionStatusVerified("static");
        }
    }

    @Override
    public void onError(String errorMessage) {
        if (isAdded()) {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_ERROR,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_ERROR, errorMessage != null ? errorMessage : "unknown"));
        }
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().setValue(0);
    }

    @Override
    public void onInvalid() {
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnState().setValue(0);
        if (isAdded()) new RenewalDialog(requireActivity()).show();
    }

    @Override
    public void onOutOfRange() {
        if (isAdded()) {
            new ContactSupportDialog(requireActivity()).show();
            Shecan app = (Shecan) requireContext().getApplicationContext();
            app.getVpnState().setValue(0);
        }
    }

    @Override
    public void onInTheRange() {
        if (isAdded()) {
            TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECTED,
                    TrackingUtils.bundleOf(TrackingUtils.PARAM_METHOD, "static"));
            Shecan.setStaticIPMode();
            startActivity(new Intent(requireActivity(), MainActivityNew.class)
                    .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
        }
    }

    @Override
    public void onConnected() {
        if (!isAdded()) return;
        cancelDynamicIpStatusCheck();
        startVpnAfterConnectionStatusVerified(ShecanVpnService.isDynamicIPMode() ? "dynamic" : "static");
    }

    @Override
    public void onRetry() {
        if (!isAdded() || isRemoving()) return;

        if (ShecanVpnService.isDynamicIPMode()) {
            if (!isDynamicIpCheckInProgress()) {
                failDynamicIpStatusCheck();
                return;
            }
            cancelScheduler();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && !isRemoving() && isDynamicIpCheckInProgress()) {
                        ShecanVpnService.callConnectionStatusAPI(requireContext(), HomeFragment.this, null);
                    } else if (isAdded() && !isRemoving()) {
                        failDynamicIpStatusCheck();
                    }
                });
            }, DYNAMIC_IP_CHECK_RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || isRemoving()) return;
                Shecan.deactivateService(requireContext());
            });
        }
    }

    private void waitForDynamicIpActivation() {
        dynamicIpCheckDeadlineMs = System.currentTimeMillis() + DYNAMIC_IP_CHECK_TIMEOUT_MS;
        ShecanVpnService.callConnectionStatusAPI(requireContext(), this, null);
    }

    private boolean isDynamicIpCheckInProgress() {
        return dynamicIpCheckDeadlineMs > 0 && System.currentTimeMillis() <= dynamicIpCheckDeadlineMs;
    }

    private void failDynamicIpStatusCheck() {
        cancelDynamicIpStatusCheck();
        Shecan app = (Shecan) requireContext().getApplicationContext();
        app.getVpnStatus().setValue(getString(R.string.dynamic_ip_connection_not_ready));
        app.getVpnState().setValue(0);
    }

    private void startVpnAfterConnectionStatusVerified(String method) {
        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_VPN_CONNECTED,
                TrackingUtils.bundleOf(TrackingUtils.PARAM_METHOD, method));
        startActivity(new Intent(requireActivity(), MainActivityNew.class)
                .putExtra(MainActivityNew.LAUNCH_ACTION, MainActivityNew.LAUNCH_ACTION_ACTIVATE));
    }

    private void cancelDynamicIpStatusCheck() {
        dynamicIpCheckDeadlineMs = 0L;
        cancelScheduler();
    }

    private void cancelScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    public void updateBanner() {
        AuthApi auth = new AuthApi(getContext());

        auth.homePageApi(
                new ApiCallback<HomePage>() {
                    @Override
                    public void onSuccess(HomePage res, boolean fromCache) {
                        showBanner(auth, res, fromCache);
                        checkRatingRule(res.getAppStoreRate().getAndroid());
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        Log.e(TAG, "onError: ");
                    }
                }
        );
    }

    private void showBanner(AuthApi auth, HomePage res, boolean fromCache) {
        auth.bannerMatch(
                DynamicBannerRequestFactory.fromStorage(requireContext()),
                new ApiCallback<BannerViewModel>() {
                    @Override
                    public void onSuccess(BannerViewModel banner, boolean fromCache) {
                        if (!isAdded()) return;
                        List<BannerViewModel> list = banner != null
                                ? Collections.singletonList(banner)
                                : new ArrayList<>();
                        activity.bannerUrl = list;
                        handleBannerImage(list);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        loadLegacyBanners(auth, res);
                    }
                }
        );
    }

    private void loadLegacyBanners(AuthApi auth, HomePage res) {
        if (res == null || res.getBannerService() == null) {
            handleBannerImage(new ArrayList<>());
            return;
        }

        auth.bannerList(
                res.getBannerService().getAndroid(),
                new ApiCallback<List<BannerViewModel>>() {
                    @Override
                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {
                        if (!isAdded()) return;
                        activity.bannerUrl = list;
                        handleBannerImage(list);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        if (!isAdded()) return;
                        handleBannerImage(new ArrayList<>());
                        ToastManager.show(getContext(), message);
                    }
                }
        );
    }

    private void checkRatingRule(String url) {
        if (!isAdded()) return;

        RatingManager ratingManager = new RatingManager(requireContext());
        ratingManager.initFirstOpenIfNeeded();

        if (ratingManager.shouldShowRatingDialog()) {
            RatingDialog.showIfNotVisible(requireContext(), url);
        }
    }

    private void handleBannerImage(List<BannerViewModel> banners) {

        if (!isAdded() || binding == null) return;

        if (banners == null || banners.isEmpty()) {
            hasBanner = false;
            binding.bannerSlider.stop();
            binding.bannerSlider.setVisibility(GONE);
            return;
        }

        hasBanner = true;
        binding.bannerSlider.setVisibility(VISIBLE);
        binding.bannerSlider.setBanners(banners);
        binding.bannerSlider.start();

        binding.bannerSlider.getImageView().setOnClickListener(v -> {
            BannerViewModel banner = binding.bannerSlider.getCurrentBanner();
            if (banner != null && banner.getUrl() != null && !banner.getUrl().isEmpty()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BANNER_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_BANNER_URL, banner.getUrl()));
                AppUtils.openUrl(banner.getUrl(), getActivity());
            }
        });
    }

}
