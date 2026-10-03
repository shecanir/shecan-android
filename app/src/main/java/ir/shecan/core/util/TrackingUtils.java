package ir.shecan.core.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class TrackingUtils {

    public static final String EVENT_APP_OPEN = "app_open_custom";
    public static final String EVENT_TAB_SELECTED = "tab_selected";
    public static final String EVENT_VPN_CONNECT_CLICK = "vpn_connect_click";
    public static final String EVENT_VPN_DISCONNECT_CLICK = "vpn_disconnect_click";
    public static final String EVENT_VPN_CONNECTED = "vpn_connected";
    public static final String EVENT_VPN_ERROR = "vpn_error";
    public static final String EVENT_SERVICE_SELECTED = "service_selected";
    public static final String EVENT_SERVICE_DETAILS_CLICK = "service_details_click";
    public static final String EVENT_BANNER_CLICK = "banner_click";
    public static final String EVENT_VIP_CLICK = "vip_click";
    public static final String EVENT_LOGIN_CONTINUE_CLICK = "login_continue_click";
    public static final String EVENT_LOGIN_IDENTIFIER_EXISTS = "login_identifier_exists";
    public static final String EVENT_LOGIN_PASSWORD_ATTEMPT = "login_password_attempt";
    public static final String EVENT_LOGIN_PASSWORD_SUCCESS = "login_password_success";
    public static final String EVENT_LOGIN_OTP_REQUEST = "login_otp_request";
    public static final String EVENT_LOGIN_OTP_VERIFY_ATTEMPT = "login_otp_verify_attempt";
    public static final String EVENT_LOGIN_OTP_SUCCESS = "login_otp_success";
    public static final String EVENT_BILLING_PLAN_SELECTED = "billing_plan_selected";
    public static final String EVENT_BILLING_DISCOUNT_CLICK = "billing_discount_click";
    public static final String EVENT_BILLING_DISCOUNT_SUCCESS = "billing_discount_success";
    public static final String EVENT_BILLING_PURCHASE_CLICK = "billing_purchase_click";
    public static final String EVENT_BILLING_PURCHASE_START = "billing_purchase_start";
    public static final String EVENT_BILLING_PURCHASE_SUCCESS = "billing_purchase_success";
    public static final String EVENT_BILLING_PURCHASE_CANCEL = "billing_purchase_cancel";
    public static final String EVENT_BILLING_PURCHASE_ERROR = "billing_purchase_error";
    public static final String EVENT_DYNAMIC_DIALOG_DISPLAY = "dynamic_dialog_display";
    public static final String EVENT_DYNAMIC_DIALOG_DISMISS = "dynamic_dialog_dismiss";
    public static final String EVENT_DYNAMIC_DIALOG_ACTION = "dynamic_dialog_action";

    public static final String PARAM_SOURCE = "source";
    public static final String PARAM_TAB_INDEX = "tab_index";
    public static final String PARAM_TAB_NAME = "tab_name";
    public static final String PARAM_SERVICE_TYPE = "service_type";
    public static final String PARAM_ORDER_CODE = "order_code";
    public static final String PARAM_BANNER_URL = "banner_url";
    public static final String PARAM_HAS_ACCOUNT = "has_account";
    public static final String PARAM_METHOD = "method";
    public static final String PARAM_STORE = "store";
    public static final String PARAM_PLAN_SKU = "plan_sku";
    public static final String PARAM_PLAN_TITLE = "plan_title";
    public static final String PARAM_SERVICE_LEVEL = "service_level";
    public static final String PARAM_PERIOD = "period";
    public static final String PARAM_AMOUNT = "amount";
    public static final String PARAM_DISCOUNT_APPLIED = "discount_applied";
    public static final String PARAM_ERROR = "error";
    public static final String PARAM_DIALOG_ID = "dialog_id";
    public static final String PARAM_BUTTON_TYPE = "button_type";

    public static void logEvent(Context context, String eventName) {
        logEvent(context, eventName, null);
    }

    public static void logEvent(Context context, String eventName, Bundle params) {
        if (context == null || eventName == null || eventName.trim().isEmpty()) return;
        try {
            FirebaseAnalytics.getInstance(context.getApplicationContext()).logEvent(eventName, params);
        } catch (RuntimeException ignored) {
        }
    }

    public static void setUserId(Context context, String userId) {
        if (context == null || userId == null || userId.trim().isEmpty()) return;
        try {
            FirebaseAnalytics.getInstance(context.getApplicationContext()).setUserId(userId);
        } catch (RuntimeException ignored) {
        }
    }

    public static Bundle bundleOf(String key, String value) {
        Bundle bundle = new Bundle();
        put(bundle, key, value);
        return bundle;
    }

    public static void put(Bundle bundle, String key, String value) {
        if (bundle == null || key == null || value == null) return;
        bundle.putString(key, limit(value));
    }

    public static void put(Bundle bundle, String key, boolean value) {
        if (bundle == null || key == null) return;
        bundle.putString(key, value ? "true" : "false");
    }

    public static void put(Bundle bundle, String key, long value) {
        if (bundle == null || key == null) return;
        bundle.putLong(key, value);
    }

    private static String limit(String value) {
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    @SuppressLint("HardwareIds")
    public String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo active = cm.getActiveNetworkInfo();

        if (active == null || !active.isConnectedOrConnecting()) return "none";

        switch (active.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return "wifi";
            case ConnectivityManager.TYPE_MOBILE:
                return "mobile";
            case ConnectivityManager.TYPE_ETHERNET:
                return "ethernet";
            default:
                return "unknown";
        }
    }

    public String getAppVersion(Context context){
        String appVersion = AppUtils.getVersionName(context);
        int versionCode = AppUtils.getVersionCode(context);
        return "-v" + versionCode + "(" + appVersion + ")";
    }

    public String getDeviceModel(){
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

}
