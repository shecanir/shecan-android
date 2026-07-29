package ir.shecan.data.api;

import android.content.Context;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ir.shecan.BuildConfig;
import ir.shecan.Shecan;
import ir.shecan.core.billing.BillingPlan;
import ir.shecan.core.billing.BillingPlanCatalog;
import ir.shecan.core.constant.Constant;
import ir.shecan.data.modelDio.ExistApiInput;
import ir.shecan.data.modelDio.BannerMatchApiInput;
import ir.shecan.data.modelDio.DialogActionApiInput;
import ir.shecan.data.modelDio.DialogDismissApiInput;
import ir.shecan.data.modelDio.DialogMatchApiInput;
import ir.shecan.data.modelDio.DiscountApiInput;
import ir.shecan.data.modelDio.LoginApiInput;
import ir.shecan.data.modelDio.PriceApiInput;
import ir.shecan.data.modelDio.SendOtpApiInput;
import ir.shecan.data.modelDio.UseDiscountApiInput;
import ir.shecan.data.modelDio.VerifyApiInput;
import ir.shecan.data.modelDto.AccountViewModel;
import ir.shecan.data.modelDto.BannerViewModel;
import ir.shecan.data.modelDto.DiscountViewModel;
import ir.shecan.data.modelDto.DynamicDialogViewModel;
import ir.shecan.data.modelDto.EmptyResponse;
import ir.shecan.data.modelDto.ExistApiViewModel;
import ir.shecan.data.modelDto.HomePage;
import ir.shecan.data.modelDto.IssuesViewModel;
import ir.shecan.data.modelDto.IapVerifyViewModel;
import ir.shecan.data.modelDto.PaymentIssueViewModel;
import ir.shecan.data.modelDto.PriceViewModel;
import ir.shecan.data.modelDto.SendOtpApiViewModel;
import ir.shecan.data.modelDto.ServicesViewModel;
import ir.shecan.data.modelDto.UserRating;
import ir.shecan.data.modelDto.VerifyApiViewModel;

public class AuthApi {

    private static final int USER_ORDERS_QUERY_ID = 106;

    private static final Object BANNER_MATCH_LOCK = new Object();
    private static final List<ApiCallback<BannerViewModel>> BANNER_MATCH_CALLBACKS = new ArrayList<>();
    private static boolean bannerMatchLoaded = false;
    private static boolean bannerMatchLoading = false;
    private static BannerViewModel bannerMatchCache = null;
    private static final Object DIALOG_MATCH_LOCK = new Object();
    private static final List<ApiCallback<DynamicDialogViewModel>> DIALOG_MATCH_CALLBACKS = new ArrayList<>();
    private static boolean dialogMatchLoaded = false;
    private static boolean dialogMatchLoading = false;
    private static DynamicDialogViewModel dialogMatchCache = null;

    private final ApiRepository repo;

    public AuthApi(Context context) {
        repo = new ApiRepository(context);
    }

    // ---------------------------------------------------
    // 1) exists
    // ---------------------------------------------------
    public void exists(String identifier, ApiCallback<ExistApiViewModel> callback) {

        ExistApiInput input = new ExistApiInput(identifier);

        repo.request(
                "otp_exists_" + identifier,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/auth/exists",
                HttpMethod.POST,
                false,
                callback,
                ExistApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 2) login
    // ---------------------------------------------------
    public void login(String identifier, String password, ApiCallback<VerifyApiViewModel> callback) {

        LoginApiInput input = new LoginApiInput(identifier, password);

        repo.request(
                "login_" + identifier,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/auth/login",
                HttpMethod.POST,
                false,
                callback,
                VerifyApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 3) send OTP
    // ---------------------------------------------------
    public void sendOtp(String identifier, ApiCallback<SendOtpApiViewModel> callback) {

        SendOtpApiInput input = new SendOtpApiInput(identifier);

        repo.request(
                "otp_send_" + identifier,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/auth/send-otp",
                HttpMethod.POST,
                false,
                callback,
                SendOtpApiViewModel.class
        );
    }

    // ---------------------------------------------------
    // 4) verify OTP
    // ---------------------------------------------------

    public void verifyOtpObject(String identifier, String code, ApiCallback<VerifyApiViewModel> callback) {

        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.request(
                "otp_verify_Object_" + identifier,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
                false,
                callback,
                VerifyApiViewModel.class
        );
    }

    public void verifyOtp(String identifier, String code, ApiCallback<VerifyApiViewModel> callback) {

        VerifyApiInput input = new VerifyApiInput(code, identifier);

        repo.requestList(
                "otp_verify_" + identifier,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/auth/verify",
                HttpMethod.POST,
                false,
                false,
                new ApiCallback<List<VerifyApiViewModel>>() {
                    @Override
                    public void onSuccess(List<VerifyApiViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(null, false);
                            return;
                        }

                        callback.onSuccess(list.get(0), false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                VerifyApiViewModel.class
        );
    }

    private Map<String, String> storeHeader() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", APIManager.getStoreHeaderValue());
        headers.put("X-App-Market", APIManager.getAppMarketHeaderValue());
        headers.put("X-App-Version", BuildConfig.VERSION_NAME);
        headers.put("X-App-Build", String.valueOf(BuildConfig.VERSION_CODE));
        headers.put("X-App-Platform", "android");
        return headers;
    }

    // ---------------------------------------------------
    // 5) update password
    // ---------------------------------------------------
    public void updatePassword(String apiKey, String password, ApiCallback<EmptyResponse> callback) {

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("password", password);

        repo.request(
                "update_pass",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                callback,
                EmptyResponse.class
        );
    }

    // ---------------------------------------------------
    // 6) update profile
    // ---------------------------------------------------
    public void updateProfile(
            String apiKey,
            String firstname,
            String lastname,
            String companyName,
            String mail,
            String phoneNumber,
            ApiCallback<EmptyResponse> callback
    ) {

        Map<String, String> payload = new HashMap<>();
        payload.put("api_key", apiKey);
        payload.put("firstname", firstname);
        payload.put("lastname", lastname);
        payload.put("phoneNumber", phoneNumber);

        if (companyName != null) payload.put("company_name", companyName);

        payload.put("mail", mail);

        repo.apiManager.setApiKey(apiKey);

        repo.request(
                "update_profile",
                payload,
                "https://my.shecan.ir/api/auth/update",
                HttpMethod.PUT,
                false,
                callback,
                EmptyResponse.class
        );
    }

    // ---------------------------------------------------
    // 7) me (account info)
    // ---------------------------------------------------
    public void me(String apiKey, ApiCallback<AccountViewModel> callback) {

        repo.apiManager.setApiKey(apiKey);

        repo.request(
                "my_account",
                null,
                "https://my.shecan.ir/my/account.json?key=" + apiKey
                        + "&_=" + System.currentTimeMillis(),
                HttpMethod.GET,
                false,
                callback,
                AccountViewModel.class
        );
    }

    // ---------------------------------------------------
    // 8) issues
    // ---------------------------------------------------
    public void issues(String apiKey, int offset, int limit, ApiCallback<IssuesViewModel> callback) {

        repo.apiManager.setApiKey(apiKey);

        String url = buildIssuesUrl(apiKey, offset, limit);

        repo.request(
                "issues",
                null,
                url,
                HttpMethod.GET,
                false,
                callback,
                IssuesViewModel.class
        );
    }

    static String buildIssuesUrl(String apiKey, int offset, int limit) {
        return Uri.parse("https://my.shecan.ir/issues.json")
                .buildUpon()
                .appendQueryParameter("query_id", String.valueOf(USER_ORDERS_QUERY_ID))
                .appendQueryParameter("offset", String.valueOf(offset))
                .appendQueryParameter("limit", String.valueOf(limit))
                .appendQueryParameter("key", apiKey)
                .build()
                .toString();
    }

    public void paymentIssue(long paymentId, ApiCallback<PaymentIssueViewModel> callback) {
        repo.request(
                "payment_issue_" + paymentId,
                null,
                storeHeader(),
                "https://my.shecan.ir/payments/" + paymentId + ".json",
                HttpMethod.GET,
                false,
                callback,
                PaymentIssueViewModel.class
        );
    }

    public void services(ApiCallback<ServicesViewModel> callback) {
        repo.request(
                "services_catalog",
                null,
                storeHeader(),
                "https://my.shecan.ir/api/services",
                HttpMethod.GET,
                false,
                callback,
                ServicesViewModel.class
        );
    }


    // ---------------------------------------------------
    // 9) banner
    // ---------------------------------------------------
    public void banner(ApiCallback<BannerViewModel> callback) {

        repo.requestList(
                "banner",
                null,
                "https://n8n.coolify.shcn.ir/webhook/banner?type=1",
                HttpMethod.GET,
                false,
                true,
                new ApiCallback<List<BannerViewModel>>() {
                    @Override
                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(null, false);
                            return;
                        }
                        // کوچکترین اوردر برداشته شود
                        int minIndex = 0;
                        for (int i = 1; i < list.size(); i++) {
                            if (list.get(i).getOrder() < list.get(minIndex).getOrder()) {
                                minIndex = i;
                            }
                        }
                        callback.onSuccess(list.get(minIndex), false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                BannerViewModel.class
        );
    }

    public void bannerList(String url, ApiCallback<List<BannerViewModel>> callback) {

        repo.requestList(
                "banner",
                null,
                url,
                HttpMethod.GET,
                false,
                true,
                new ApiCallback<List<BannerViewModel>>() {
                    @Override
                    public void onSuccess(List<BannerViewModel> list, boolean fromCache) {

                        if (list == null || list.isEmpty()) {
                            callback.onSuccess(new ArrayList<>(), false);
                            return;
                        }

                        // کل لیست را بده UI — بدون انتخاب کوچک‌ترین order
                        callback.onSuccess(list, false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                BannerViewModel.class
        );
    }

    public void bannerMatch(BannerMatchApiInput input, ApiCallback<BannerViewModel> callback) {
        synchronized (BANNER_MATCH_LOCK) {
            if (bannerMatchLoaded) {
                callback.onSuccess(bannerMatchCache, true);
                return;
            }

            BANNER_MATCH_CALLBACKS.add(callback);
            if (bannerMatchLoading) return;
            bannerMatchLoading = true;
        }

        repo.request(
                "banner_match_" + (input != null ? input.getApiKey() + "_" + input.getServiceType() + "_" + input.getPlan() : "guest"),
                input,
                Shecan.ShecanInfo.getDynamicBannerUrl(),
                HttpMethod.POST,
                false,
                new ApiCallback<BannerViewModel>() {
                    @Override
                    public void onSuccess(BannerViewModel banner, boolean fromCache) {
                        List<ApiCallback<BannerViewModel>> callbacks = completeBannerMatchSuccess(banner);
                        for (ApiCallback<BannerViewModel> cb : callbacks) {
                            cb.onSuccess(banner, fromCache);
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        List<ApiCallback<BannerViewModel>> callbacks = completeBannerMatchError();
                        for (ApiCallback<BannerViewModel> cb : callbacks) {
                            cb.onError(statusCode, message);
                        }
                    }
                },
                BannerViewModel.class
        );
    }

    private static List<ApiCallback<BannerViewModel>> completeBannerMatchSuccess(BannerViewModel banner) {
        synchronized (BANNER_MATCH_LOCK) {
            bannerMatchLoaded = true;
            bannerMatchLoading = false;
            bannerMatchCache = banner;
            List<ApiCallback<BannerViewModel>> callbacks = new ArrayList<>(BANNER_MATCH_CALLBACKS);
            BANNER_MATCH_CALLBACKS.clear();
            return callbacks;
        }
    }

    private static List<ApiCallback<BannerViewModel>> completeBannerMatchError() {
        synchronized (BANNER_MATCH_LOCK) {
            bannerMatchLoading = false;
            List<ApiCallback<BannerViewModel>> callbacks = new ArrayList<>(BANNER_MATCH_CALLBACKS);
            BANNER_MATCH_CALLBACKS.clear();
            return callbacks;
        }
    }

    public void dialogMatch(DialogMatchApiInput input, ApiCallback<DynamicDialogViewModel> callback) {
        synchronized (DIALOG_MATCH_LOCK) {
            if (dialogMatchLoaded) {
                callback.onSuccess(dialogMatchCache, true);
                return;
            }

            DIALOG_MATCH_CALLBACKS.add(callback);
            if (dialogMatchLoading) return;
            dialogMatchLoading = true;
        }

        repo.request(
                "dialog_match_" + (input != null ? input.getApiKey() + "_" + input.getServiceType() + "_" + input.getPlan() : "guest"),
                input,
                Shecan.ShecanInfo.getDialogMatchUrl(),
                HttpMethod.POST,
                false,
                new ApiCallback<DynamicDialogViewModel>() {
                    @Override
                    public void onSuccess(DynamicDialogViewModel dialog, boolean fromCache) {
                        List<ApiCallback<DynamicDialogViewModel>> callbacks = completeDialogMatchSuccess(dialog);
                        for (ApiCallback<DynamicDialogViewModel> cb : callbacks) {
                            cb.onSuccess(dialog, fromCache);
                        }
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        List<ApiCallback<DynamicDialogViewModel>> callbacks = completeDialogMatchError();
                        for (ApiCallback<DynamicDialogViewModel> cb : callbacks) {
                            cb.onError(statusCode, message);
                        }
                    }
                },
                DynamicDialogViewModel.class
        );
    }

    public void dialogDismiss(DialogDismissApiInput input, ApiCallback<EmptyResponse> callback) {
        repo.request(
                "dialog_dismiss",
                input,
                Shecan.ShecanInfo.getDialogDismissUrl(),
                HttpMethod.POST,
                false,
                callback,
                EmptyResponse.class
        );
    }

    public void dialogAction(DialogActionApiInput input, ApiCallback<EmptyResponse> callback) {
        repo.request(
                "dialog_action",
                input,
                Shecan.ShecanInfo.getDialogActionUrl(),
                HttpMethod.POST,
                false,
                callback,
                EmptyResponse.class
        );
    }

    private static List<ApiCallback<DynamicDialogViewModel>> completeDialogMatchSuccess(DynamicDialogViewModel dialog) {
        synchronized (DIALOG_MATCH_LOCK) {
            dialogMatchLoaded = true;
            dialogMatchLoading = false;
            dialogMatchCache = dialog;
            List<ApiCallback<DynamicDialogViewModel>> callbacks = new ArrayList<>(DIALOG_MATCH_CALLBACKS);
            DIALOG_MATCH_CALLBACKS.clear();
            return callbacks;
        }
    }

    private static List<ApiCallback<DynamicDialogViewModel>> completeDialogMatchError() {
        synchronized (DIALOG_MATCH_LOCK) {
            dialogMatchLoading = false;
            List<ApiCallback<DynamicDialogViewModel>> callbacks = new ArrayList<>(DIALOG_MATCH_CALLBACKS);
            DIALOG_MATCH_CALLBACKS.clear();
            return callbacks;
        }
    }

    // ---------------------------------------------------
    // 10) homePage
    // ---------------------------------------------------
    public void homePageApi(ApiCallback<HomePage> callback) {

        repo.request(
                "homePage",
                null,
                "https://shecan.ir/app/home-page/",
                HttpMethod.GET,
                false,
                new ApiCallback<HomePage>() {
                    @Override
                    public void onSuccess(HomePage homePage, boolean fromCache) {
                        Shecan.ShecanInfo.saveHomePageConfig(homePage);
                        callback.onSuccess(homePage, fromCache);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                HomePage.class
        );
    }

    public void price(String sla, String period, int discount, ApiCallback<PriceViewModel> callback) {
        price(null, sla, period, discount, callback);
    }

    public void price(String apiKey, String sla, String period, int discount, ApiCallback<PriceViewModel> callback) {
        PriceApiInput input = new PriceApiInput(sla, period, discount);
        repo.apiManager.setApiKey(apiKey != null ? apiKey : "");

        repo.request(
                "price_" + APIManager.getAppMarketHeaderValue() + "_" + sla + "_" + period + "_" + discount,
                input,
                storeHeader(),
                "https://my.shecan.ir/api/price",
                HttpMethod.POST,
                false,
                callback,
                PriceViewModel.class
        );
    }

    public void discount(
            String apiKey,
            long planPrice,
            int planId,
            String phone,
            String code,
            int durationId,
            String market,
            ApiCallback<DiscountViewModel> callback
    ) {
        Map<String, Object> input = new HashMap<>();
        input.put("api_key", apiKey);
        input.put("plan_price", planPrice);
        input.put("plan_id", planId);
        input.put("phone", phone);
        input.put("code", code);
        input.put("duration_id", durationId);
        input.put("market", market);
        input.put("app_version_name", BuildConfig.VERSION_NAME);
        input.put("app_version_code", BuildConfig.VERSION_CODE);

        repo.request(
                "discount_" + planId + "_" + durationId + "_" + code,
                input,
                "https://my.shecan.ir/api/discount",
                HttpMethod.POST,
                false,
                callback,
                DiscountViewModel.class
        );
    }

    public void useDiscount(
            String phone,
            String code,
            long orderId,
            long planPrice,
            ApiCallback<EmptyResponse> callback
    ) {
        UseDiscountApiInput input = new UseDiscountApiInput(phone, code, orderId, planPrice);
        repo.request(
                "use_discount_" + orderId + "_" + code,
                input,
                "https://n8n.coolify.shcn.ir/webhook/use-discount",
                HttpMethod.POST,
                false,
                callback,
                EmptyResponse.class
        );
    }

    public String buildSitePaymentUrl(
            String apiKey,
            long amount,
            String sla,
            String period,
            long discount,
            String discountCode,
            Long renewalOrderId
    ) {
        return createSitePaymentUrl(
                apiKey,
                amount,
                sla,
                period,
                discount,
                discountCode,
                renewalOrderId
        );
    }

    static String createSitePaymentUrl(
            String apiKey,
            long amount,
            String sla,
            String period,
            long discount,
            String discountCode,
            Long renewalOrderId
    ) {
        Uri.Builder builder = Uri.parse("https://my.shecan.ir/order/shecan/payment")
                .buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("amount", String.valueOf(amount))
                .appendQueryParameter("sla", sla)
                .appendQueryParameter("period", period)
                .appendQueryParameter("discount", String.valueOf(discount))
                .appendQueryParameter("callback_url", Constant.AppPaymentCallbackUrl);

        if (renewalOrderId != null && renewalOrderId > 0L) {
            builder.appendQueryParameter("order_id", String.valueOf(renewalOrderId));
        }

        if (discountCode != null && !discountCode.trim().isEmpty()) {
            builder.appendQueryParameter("discount_code", discountCode.trim());
        }

        return builder.build().toString();
    }

    public void verifyIap(
            String apiKey,
            String market,
            long issueId,
            String packageName,
            String productId,
            String purchaseToken,
            long amount,
            String storeOrderId,
            Long renewalOrderId,
            ApiCallback<IapVerifyViewModel> callback
    ) {
        repo.apiManager.setApiKey(apiKey);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", 1);
        if (renewalOrderId != null && renewalOrderId > 0L) {
            payload.put("order_id", renewalOrderId);
        }
        payload.put("api_key", apiKey);
        payload.put("market", market);
        if (issueId > 0) {
            payload.put("issue_id", issueId);
        }
        payload.put("package_name", packageName);
        payload.put("product_id", productId);
        BillingPlan plan = BillingPlanCatalog.findBySku(productId);
        if (plan != null) {
            payload.put("sla", plan.getSla().getApiValue());
            payload.put("period", plan.getPeriod().getApiValue());
        }
        payload.put("purchase_token", purchaseToken);
        if (amount > 0) {
            payload.put("amount", amount);
        }
        if (storeOrderId != null && !storeOrderId.trim().isEmpty()) {
            payload.put("store_order_id", storeOrderId);
        }

        repo.request(
                "iap_verify_" + market + "_" + productId + "_" + purchaseToken,
                payload,
                "https://my.shecan.ir/api/iap/verify",
                HttpMethod.POST,
                false,
                callback,
                IapVerifyViewModel.class
        );
    }

    private String getSitePaymentSubject(String sla, String period, VerifyApiViewModel user) {
        String name = getPaymentCompanyName(user);
        return sla + " " + period + " service - " + name;
    }

    private Map<String, Object> createWebPaymentUserPayload(String apiKey, VerifyApiViewModel user) {
        Map<String, Object> userPayload = new LinkedHashMap<>();

        if (user == null) {
            userPayload.put("id", 0);
            userPayload.put("login", "");
            userPayload.put("api_key", apiKey);
            userPayload.put("firstname", "");
            userPayload.put("lastname", "");
            userPayload.put("mail", "");
            userPayload.put("created_on", "");
            userPayload.put("last_login_on", "");
            userPayload.put("admin", false);
            userPayload.put("status", 1);
            userPayload.put("groups", new ArrayList<>());
            userPayload.put("memberships", new ArrayList<>());
            userPayload.put("welcome_text", createWebPaymentWelcomeText());
            return userPayload;
        }

        userPayload.put("id", user.getId());
        userPayload.put("login", createLegacyPaymentLogin(user.getLogin()));
        userPayload.put("api_key", apiKey);
        userPayload.put("firstname", safeString(user.getFirstname()));
        userPayload.put("lastname", safeString(user.getLastname()));
        userPayload.put("mail", safeString(user.getMail()));
        userPayload.put("created_on", safeString(user.getCreatedOn()));
        userPayload.put("last_login_on", safeString(user.getLastLoginOn()));
        userPayload.put("admin", user.getAdmin() != null ? user.getAdmin() : false);
        userPayload.put("status", user.getStatus());
        userPayload.put("groups", new ArrayList<>());
        userPayload.put("memberships", new ArrayList<>());
        userPayload.put("welcome_text", createWebPaymentWelcomeText());

        return userPayload;
    }

    private String createWebPaymentWelcomeText() {
        return "<blockquote data-end=\"664\" data-start=\"597\">\r\n"
                + "<p dir=\"rtl\" style=\"text-align: center;\"><span style=\"font-size:22px;\">🚀 پنل جدید شکن در دسترس قرار گرفت.</span></p>\r\n"
                + "\r\n"
                + "<p style=\"text-align: center;\">&nbsp;</p>\r\n"
                + "\r\n"
                + "<p dir=\"rtl\" style=\"text-align: center;\"><span style=\"font-size:22px;\">از حالا می&zwnj;تونید از طریق آدرس زیر وارد نسخه&zwnj;ی تازه بشید و تجربه&zwnj;ای روان&zwnj;تر و حرفه&zwnj;ای&zwnj;تر داشته باشید:</span></p>\r\n"
                + "\r\n"
                + "<p style=\"text-align: center;\">&nbsp;</p>\r\n"
                + "\r\n"
                + "<p style=\"text-align: center;\"><span style=\"font-size:24px;\"><a href=\"https://my.shecan.ir/panel\">&nbsp;my.shecan.ir/panel</a></span></p>\r\n"
                + "\r\n"
                + "<p dir=\"rtl\" style=\"text-align: center;\"><span style=\"font-size:22px;\">در جریان استفاده، هر فیدبکی دارید با ما در میون بگذارید تا نسخه&zwnj;ی بعدی رو دقیق&zwnj;تر و بهتر بسازیم.</span></p>\r\n"
                + "\r\n"
                + "<p dir=\"rtl\" style=\"text-align: center;\">&nbsp;</p>\r\n"
                + "\r\n"
                + "<p dir=\"rtl\" style=\"text-align: center;\"><span style=\"font-size:22px;\">به دنیای تازه&zwnj;ی شکن خوش اومدین💚</span></p>\r\n"
                + "</blockquote>\r\n";
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private Map<String, String> createWebPaymentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.put("accept-language", "en-US,en;q=0.9,fa-IR;q=0.8,fa;q=0.7");
        headers.put("cache-control", "max-age=0");
        headers.put("content-type", "application/x-www-form-urlencoded");
        headers.put("origin", "https://my.shecan.ir");
        headers.put("priority", "u=0, i");
        headers.put("referer", "https://my.shecan.ir/panel/order");
        headers.put("sec-ch-ua", "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("sec-fetch-dest", "document");
        headers.put("sec-fetch-mode", "navigate");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-user", "?1");
        headers.put("upgrade-insecure-requests", "1");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");
        return headers;
    }

    private String createLegacyPaymentLogin(String login) {
        String safeLogin = safeString(login).trim();
        if (safeLogin.regionMatches(true, 0, "Old", 0, 3)) {
            return safeLogin;
        }
        String normalized = normalizeIranMobile(safeLogin);
        String withoutLeadingZero = removeLeadingZero(normalized);
        return withoutLeadingZero == null || withoutLeadingZero.isEmpty()
                ? safeLogin
                : "Old" + withoutLeadingZero;
    }

    private Map<String, Object> createPaymentUserPayload(String apiKey, VerifyApiViewModel user) {
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("api_key", apiKey);
        if (user == null) return userPayload;

        userPayload.put("id", user.getId());
        userPayload.put("firstname", user.getFirstname());
        userPayload.put("lastname", user.getLastname());
        userPayload.put("mail", user.getMail());
        String normalizedPhone = normalizeIranMobile(user.getLogin());
        String normalizedPhoneWithoutZero = removeLeadingZero(normalizedPhone);
        userPayload.put("login", normalizedPhone);
        addPhoneAliases(userPayload, normalizedPhone, normalizedPhoneWithoutZero);
        userPayload.put("admin", user.getAdmin());

        List<Map<String, Object>> customFields = new ArrayList<>();
        if (user.getCustomFields() != null) {
            for (VerifyApiViewModel.CustomFieldsDTO field : user.getCustomFields()) {
                if (field == null) continue;
                Map<String, Object> item = new HashMap<>();
                item.put("id", field.getId());
                item.put("name", field.getName());
                item.put("value", field.getValue());
                customFields.add(item);
            }
        }
        addUserPhoneCustomFields(customFields, normalizedPhone);
        userPayload.put("custom_fields", customFields);

        return userPayload;
    }

    private String getPaymentCompanyName(VerifyApiViewModel user) {
        String companyName = getCompanyName(user);
        if (companyName != null && !companyName.trim().isEmpty()) {
            return companyName.trim();
        }
        if (user != null) {
            String fullName = ((user.getFirstname() != null ? user.getFirstname() : "") + " " +
                    (user.getLastname() != null ? user.getLastname() : "")).trim();
            if (!fullName.isEmpty()) return fullName;
            if (user.getLogin() != null && !user.getLogin().trim().isEmpty()) return user.getLogin().trim();
            if (user.getMail() != null && !user.getMail().trim().isEmpty()) return user.getMail().trim();
        }
        return "Shecan";
    }

    private String getCompanyName(VerifyApiViewModel user) {
        if (user == null || user.getCustomFields() == null) return null;
        for (VerifyApiViewModel.CustomFieldsDTO field : user.getCustomFields()) {
            if (field != null && field.getId() == 104) {
                return field.getValue();
            }
        }
        return null;
    }

    private String normalizeIranMobile(String value) {
        if (value == null) return null;
        String normalized = value.trim()
                .replace("Old", "")
                .replace("old", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        normalized = toEnglishDigits(normalized);
        if (normalized.startsWith("+98")) {
            normalized = "0" + normalized.substring(3);
        } else if (normalized.startsWith("0098")) {
            normalized = "0" + normalized.substring(4);
        } else if (normalized.startsWith("98") && normalized.length() == 12) {
            normalized = "0" + normalized.substring(2);
        } else if (normalized.startsWith("9") && normalized.length() == 10) {
            normalized = "0" + normalized;
        }

        return normalized;
    }

    private String removeLeadingZero(String value) {
        if (value != null && value.startsWith("0") && value.length() == 11) {
            return value.substring(1);
        }
        return value;
    }

    private void addPhoneAliases(Map<String, ?> target, String phoneWithZero, String phoneWithoutZero) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) target;
        map.put("phone", phoneWithoutZero);
        map.put("mobile", phoneWithZero);
        map.put("mobile_number", phoneWithZero);
        map.put("phone_number", phoneWithZero);
        map.put("phoneNumber", phoneWithZero);
    }

    private void addUserPhoneCustomFields(List<Map<String, Object>> customFields, String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.trim().isEmpty()) return;
        addCustomFieldIfMissing(customFields, 1, "تلفن همراه", normalizedPhone);
        addCustomFieldIfMissing(customFields, 2, "شماره موبایل", normalizedPhone);
        addCustomFieldIfMissing(customFields, 3, "موبایل", normalizedPhone);
        addCustomFieldIfMissing(customFields, 4, "mobile", normalizedPhone);
        addCustomFieldIfMissing(customFields, 5, "phone", normalizedPhone);
    }

    private void addCustomFieldIfMissing(List<Map<String, Object>> customFields, int id, String name, String value) {
        for (Map<String, Object> field : customFields) {
            Object existingId = field.get("id");
            if (existingId instanceof Number && ((Number) existingId).intValue() == id) return;
        }
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("value", value);
        customFields.add(item);
    }

    private String toEnglishDigits(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '۰' && c <= '۹') {
                builder.append((char) ('0' + (c - '۰')));
            } else if (c >= '٠' && c <= '٩') {
                builder.append((char) ('0' + (c - '٠')));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    // ---------------------------------------------------
    // 11) SendRating
    // ---------------------------------------------------
    public void sendRatingApi(
            String url,
            String mobile,
            long weeklyConnectionTime,
            String appVersion,
            String storeName,
            int rating,
            boolean isProUser,
            ApiCallback<Void> callback) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        String createdDate = sdf.format(new Date());
//
//        Map<String, String> payload = new HashMap<>();
//        payload.put("mobile_number", mobile);
//        payload.put("weekly_connection_time", String.valueOf(weeklyConnectionTime));
//        payload.put("app_version", appVersion);
//        payload.put("store_name", storeName);
//        payload.put("rating", String.valueOf(rating));
//        payload.put("is_pro_user", String.valueOf(isProUser));
//        payload.put("create_date", createdDate);

        UserRating payload = new UserRating(
                mobile,
                weeklyConnectionTime,
                appVersion,
                storeName,
                rating,
                isProUser,
                createdDate
        );

        repo.apiManager.setSecretKey("ksdasdjcu*@ndshW@1503SdD");

        repo.requestList(
                "appStoreRate",
                payload,
                url,
                HttpMethod.POST,
                false,
                false,
                new ApiCallback<List<UserRating>>() {
                    @Override
                    public void onSuccess(List<UserRating> list, boolean fromCache) {
                        callback.onSuccess(null, false);
                    }

                    @Override
                    public void onError(int statusCode, String message) {
                        callback.onError(statusCode, message);
                    }
                },
                UserRating.class
        );
    }

}
