package ir.shecan.ui.fragment.refactor;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.shecan.R;
import ir.shecan.core.billing.BillingPeriod;
import ir.shecan.core.billing.BillingSla;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.core.service.MonitoringService;
import ir.shecan.core.util.AppUtils;
import ir.shecan.core.util.DebugJsonLogger;
import ir.shecan.core.util.DynamicBannerRequestFactory;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.TrackingUtils;
import ir.shecan.data.api.ApiCallback;
import ir.shecan.data.api.AuthApi;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.ServiceItemMapper;
import ir.shecan.data.modelDto.ServicesViewModel;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.databinding.FragmentConfigListBinding;
import ir.shecan.ui.activity.BillingPlansActivity;
import ir.shecan.ui.activity.MainActivityNew;
import ir.shecan.ui.activity.PanelWebActivity;
import ir.shecan.ui.adapter.ServiceAdapter;
import ir.shecan.ui.fragment.ToolbarFragment;
import ir.shecan.ui.fragment.bottomSheet.SubscriptionBottomSheet;

public class ConfigListFragment extends ToolbarFragment {

    private FragmentConfigListBinding binding;
    MainActivityNew activity;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentConfigListBinding.inflate(inflater, container, false);
        activity = (MainActivityNew) getActivity();

        startLoading();

        AppStorage appStorage = new AppStorage(getContext());
        reloadServices(appStorage);

        refreshPage();

        binding.swipeRefresh.setOnRefreshListener(this::refreshPage);

        return binding.getRoot();
    }


    private void refreshPage() {

        AppStorage storage = new AppStorage(getContext());
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);

        if (token == null || token.getApiKey() == null) {
            updateBanner();
            reloadServices(storage);
            stopLoading();
            return;
        }

        AuthApi authApi = new AuthApi(getContext());
        authApi.services(new ApiCallback<ServicesViewModel>() {
            @Override
            public void onSuccess(ServicesViewModel res, boolean fromCache) {
                if (res != null) {
                    storage.saveServiceCatalog(res);
                    reloadServices(storage);
                }
            }

            @Override
            public void onError(int statusCode, String message) {
                // The most recently saved catalog remains available as a display fallback.
            }
        });

        authApi.issues(
                token.getApiKey(),
                0,
                1000,
                new ApiCallback<IssuesViewModel>() {

                    @Override
                    public void onSuccess(IssuesViewModel res, boolean fromCache) {
                        DebugJsonLogger.log("ShecanIssuesJson", res);
                        storage.saveIssues(res);
                        reloadServices(storage);
                        updateBanner();
                        stopLoading();
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        showSilentWarning(
                                message != null ? message : "خطا در دریافت سرویس‌ها"
                        );
                        reloadServices(storage);
                        stopLoading();
                    }
                }
        );
    }

    private void reloadServices(AppStorage storage) {
        setupRecyclerView(buildServiceItems(storage), storage);
    }

    private List<ServiceItem> buildServiceItems(AppStorage appStorage) {

        List<ServiceItem> items = new ArrayList<>();

        // free همیشه هست
        items.add(
                ServiceItemMapper.map(
                        getContext(),
                        IssuesViewModel.IssuesDTO.createDefault()
                )
        );

        IssuesViewModel viewModel = appStorage.getIssue(IssuesViewModel.class);

        if (viewModel == null || viewModel.getIssues() == null || viewModel.getIssues().isEmpty()) {
            showSilentWarning("اطلاعات سرویس‌ها کامل بارگذاری نشد");
            return items;
        }

        for (IssuesViewModel.IssuesDTO issue : viewModel.getIssues()) {
            items.add(ServiceItemMapper.map(getContext(), issue));
        }

        return items;
    }

    private void showSilentWarning(String message) {
        if (!isAdded()) return;
        ToastManager.show(getContext(), message);
    }

    private void setupRecyclerView(List<ServiceItem> items, AppStorage appStorage) {
        Context context = getContext();
        if(context == null){
            return;
        }
        ServiceItem savedItem = appStorage.getServiceStatus(ServiceItem.class);
        ServiceItem preferredItem = findPreferredActivePaidService(items);
        boolean savedItemMissing = savedItem != null
                && !containsOrder(items, savedItem.getOrderCode());
        boolean shouldChooseDefault = savedItem == null
                || savedItemMissing
                || (!appStorage.isServiceSelectionExplicit() && isFreeService(savedItem));
        if (shouldChooseDefault) {
            savedItem = preferredItem != null ? preferredItem : items.get(0);
            appStorage.saveServiceStatus(savedItem);
            if (savedItemMissing) {
                appStorage.clearServiceSelectionExplicit();
            }
        }

        int defaultSelected = -1;
        if (savedItem != null) {
            for (int i = 0; i < items.size(); i++) {
                ServiceItem item = items.get(i);
                String itemOrderCode = item != null ? item.getOrderCode() : null;
                if (itemOrderCode != null && itemOrderCode.equals(savedItem.getOrderCode())) {
                    defaultSelected = i;
                    break;
                }
            }
        }

        ServiceAdapter adapter = getServiceAdapter(items, appStorage, defaultSelected);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        binding.recyclerView.setAdapter(adapter);
    }

    @NonNull
    private ServiceAdapter getServiceAdapter(List<ServiceItem> items, AppStorage appStorage, int defaultSelected) {
        ServiceAdapter adapter = new ServiceAdapter(
                getContext(),
                items,
                new ServiceAdapter.OnMoreClickListener() {
                    @Override
                    public void onBackgroundClicked(ServiceItem item) {
                        if (item == null || item.isClosed()) return;
                        logServiceEvent(TrackingUtils.EVENT_SERVICE_SELECTED, item);
                        if (shouldOpenRenewal(item)) {
                            openBillingPlans(item);
                            return;
                        }

                        appStorage.saveServiceStatus(item);
                        appStorage.markServiceSelectionExplicit();
                        MonitoringService.refresh(requireContext());

                        activity.configIsChange = true;

                        ((MainActivityNew) getActivity()).updateFragment(1);
                        ((MainActivityNew) getActivity()).currentTab = 1;
                    }

                    @Override
                    public void onOptionClicked(ServiceItem item) {
                        if (item == null || item.isClosed()) return;
                        logServiceEvent(TrackingUtils.EVENT_SERVICE_DETAILS_CLICK, item);
                        SubscriptionBottomSheet bottomSheet = SubscriptionBottomSheet.newInstance(item);
                        bottomSheet.show(getParentFragmentManager(), "subscription_sheet");
                    }

                    @Override
                    public void onPurchaseClicked() {
                        TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BILLING_PURCHASE_CLICK,
                                TrackingUtils.bundleOf(TrackingUtils.PARAM_SOURCE, "service_list_fab"));
                        openBillingPlans(null);
                    }
                }
        );

        adapter.setSelectedPosition(defaultSelected);
        return adapter;
    }

    private boolean containsOrder(List<ServiceItem> items, String orderCode) {
        if (items == null || orderCode == null) return false;
        for (ServiceItem item : items) {
            if (item != null && !item.isClosed() && orderCode.equals(item.getOrderCode())) {
                return true;
            }
        }
        return false;
    }

    private ServiceItem findPreferredActivePaidService(List<ServiceItem> items) {
        if (items == null) return null;
        for (ServiceItem item : items) {
            if (isActivePaidService(item)) {
                return item;
            }
        }
        return null;
    }

    private boolean isActivePaidService(ServiceItem item) {
        if (item == null || item.isClosed() || isFreeService(item)) return false;
        RequestStatus status = RequestStatus.fromValue(item.statusId);
        return status == RequestStatus.ACTIVE
                || status == RequestStatus.IN_USE
                || status == RequestStatus.EXPIRING;
    }

    private boolean isFreeService(ServiceItem item) {
        return item == null || "0".equals(item.getOrderCode());
    }

    private boolean shouldOpenRenewal(ServiceItem item) {
        if (item == null) return false;
        if (RequestStatus.isRenewalBlocked(item.statusId)) return false;
        RequestStatus status = RequestStatus.fromValue(item.statusId);
        if (status == RequestStatus.SUPPORT_FINISHED
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_RENEW
                || status == RequestStatus.WAITING_FOR_PAYMENT_OR_ACTIVATION
                || status == RequestStatus.SUSPENDED) {
            return true;
        }
        return isDueDateExpired(item.dueDate);
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

    private void openBillingPlans(ServiceItem item) {
        if (!isAdded()) return;
        if (item != null && RequestStatus.isRenewalBlocked(item.statusId)) return;
        Intent intent = new Intent(requireContext(), BillingPlansActivity.class);

        BillingSla sla = BillingSla.fromPlanId(item != null && item.cfServiceType != null
                ? item.cfServiceType
                : -1);
        BillingPeriod period = BillingPeriod.fromDurationId(item != null && item.cfDuration != null
                ? item.cfDuration
                : -1);

        if (sla != null) {
            intent.putExtra(BillingPlansActivity.EXTRA_PREFILL_SLA, sla.getApiValue());
        }
        if (period != null) {
            intent.putExtra(BillingPlansActivity.EXTRA_PREFILL_PERIOD, period.getApiValue());
        }
        long orderId = item != null ? item.id : 0L;
        if (orderId > 0L) {
            intent.putExtra(BillingPlansActivity.EXTRA_RENEWAL_ORDER_ID, orderId);
        }

        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.bannerSlider.stop();
        binding = null;
    }

    @Override
    public void checkStatus() {
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivityNew) getActivity()).binding.customBar.select(0);
    }

    public void updateBanner() {
        AuthApi auth = new AuthApi(getContext());

        auth.homePageApi(
                new ApiCallback<HomePage>() {
                    @Override
                    public void onSuccess(HomePage res, boolean fromCache) {
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
                                        if (!isAdded()) return;
                                        loadLegacyBanners(auth, res);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(int statusCode, String message) {

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
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void handleBannerImage(List<BannerViewModel> banners) {

        if (!isAdded() || binding == null) return;

        if (banners == null || banners.isEmpty()) {
            binding.bannerSlider.stop();
            binding.bannerSlider.setVisibility(GONE);
            return;
        }

        binding.bannerSlider.setVisibility(View.VISIBLE);
        binding.bannerSlider.setBanners(banners);
        binding.bannerSlider.start();

        binding.bannerSlider.getImageView().setOnClickListener(v -> {
            BannerViewModel banner = binding.bannerSlider.getCurrentBanner();
            if (banner != null && banner.getUrl() != null && !banner.getUrl().isEmpty()) {
                TrackingUtils.logEvent(requireContext(), TrackingUtils.EVENT_BANNER_CLICK,
                        TrackingUtils.bundleOf(TrackingUtils.PARAM_BANNER_URL, banner.getUrl()));
                PanelWebActivity.openBanner(requireContext(), banner.getUrl());
            }
        });
    }

    private void logServiceEvent(String eventName, ServiceItem item) {
        if (!isAdded() || item == null) return;
        android.os.Bundle params = new android.os.Bundle();
        TrackingUtils.put(params, TrackingUtils.PARAM_SERVICE_TYPE, item.getServiceType());
        TrackingUtils.put(params, TrackingUtils.PARAM_ORDER_CODE, item.getOrderCode());
        TrackingUtils.logEvent(requireContext(), eventName, params);
    }

    private void startLoading() {
        if (binding == null) return;
        binding.swipeRefresh.post(() ->
                binding.swipeRefresh.setRefreshing(true)
        );
    }

    private void stopLoading() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(false);
    }
}
