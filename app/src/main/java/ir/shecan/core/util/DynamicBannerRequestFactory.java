package ir.shecan.core.util;

import android.content.Context;

import java.util.Locale;

import ir.shecan.BuildConfig;
import ir.shecan.core.constant.RequestStatus;
import ir.shecan.data.modelDio.BannerMatchApiInput;
import ir.shecan.data.modelDto.ServiceItem;
import ir.shecan.data.modelDto.VerifyApiViewModel;
import ir.shecan.data.storage.AppStorage;
import ir.shecan.ui.widget.rateHelper.RatingState;

public class DynamicBannerRequestFactory {

    private DynamicBannerRequestFactory() {
    }

    public static BannerMatchApiInput fromStorage(Context context) {
        AppStorage storage = new AppStorage(context);
        VerifyApiViewModel token = storage.getToken(VerifyApiViewModel.class);
        ServiceItem service = storage.getServiceStatus(ServiceItem.class);
        RatingState ratingState = storage.getRatingState(RatingState.class);

        String apiKey = token != null ? safe(token.getApiKey()) : "";
        String mobile = firstNotEmpty(
                token != null ? token.getLogin() : "",
                service != null ? service.cfMobile : ""
        );

        return new BannerMatchApiInput(
                mobile,
                apiKey,
                getServiceType(service),
                getPlan(service),
                service != null ? safe(service.startDate) : "",
                service != null ? safe(service.dueDate) : "",
                getStore(),
                isRated(ratingState),
                getPlanStatus(service)
        );
    }

    private static String getServiceType(ServiceItem service) {
        return service == null || service.cfServiceType == null || service.cfServiceType == 0
                ? "free"
                : "pro";
    }

    private static String getPlan(ServiceItem service) {
        if (service == null || service.cfServiceType == null) return "free";
        switch (service.cfServiceType) {
            case 47:
                return "bronze";
            case 48:
                return "silver";
            case 49:
                return "gold";
            case 83:
                return "commercial";
            default:
                return "free";
        }
    }

    private static String getPlanStatus(ServiceItem service) {
        if (service == null) return "inactive";
        RequestStatus status = RequestStatus.fromValue(service.statusId);
        return status == RequestStatus.ACTIVE
                || status == RequestStatus.IN_USE
                || status == RequestStatus.EXPIRING
                ? "active"
                : "inactive";
    }

    private static String getStore() {
        String store = BuildConfig.STORE != null ? BuildConfig.STORE.toLowerCase(Locale.US) : "";
        if ("cafebazaar".equals(store)) return "bazar";
        if ("myket".equals(store)) return "myket";
        if ("site".equals(store)) return "site";
        return store;
    }

    private static boolean isRated(RatingState state) {
        return state != null && (state.lastRatingValue > 0 || state.hasRatedInStore);
    }

    private static String firstNotEmpty(String first, String second) {
        return !safe(first).isEmpty() ? safe(first) : safe(second);
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
