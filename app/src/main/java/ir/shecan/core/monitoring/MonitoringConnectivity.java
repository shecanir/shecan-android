package ir.shecan.core.monitoring;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.telephony.TelephonyManager;

import java.util.Locale;

import ir.shecan.data.modelDto.monitoring.MonitoringNetworkInfo;

class MonitoringConnectivity {
    private final Context context;

    MonitoringConnectivity(Context context) {
        this.context = context.getApplicationContext();
    }

    boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network[] networks = cm.getAllNetworks();
        if (networks == null) return false;

        for (Network network : networks) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (hasInternet(capabilities)) return true;
        }

        return false;
    }

    MonitoringNetworkInfo networkInfo() {
        return new MonitoringNetworkInfo(
                networkType(),
                carrierName(),
                countryCode()
        );
    }

    private String networkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "unknown";

        Network[] networks = cm.getAllNetworks();
        if (networks == null) return "none";

        for (Network network : networks) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (!hasInternet(capabilities)) continue;

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "mobile";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return "vpn";
            return "unknown";
        }

        return "none";
    }

    private boolean hasInternet(NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    String carrierName() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) return "";
        String name = tm.getNetworkOperatorName();
        return name != null ? name : "";
    }

    String countryCode() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String country = tm != null ? tm.getNetworkCountryIso() : "";
        if (country == null || country.trim().isEmpty()) {
            country = Locale.getDefault().getCountry();
        }
        return country != null ? country.toUpperCase(Locale.US) : "";
    }
}
