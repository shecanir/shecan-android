package ir.shecan.ui.activity.mainActivityUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.service.ShecanVpnService;
import ir.shecan.core.util.ToastManager;
import ir.shecan.core.util.server.DNSServerHelper;

/**
 * Helper class to handle VPN activation and permission request.
 */
public class VpnManager {

    private final Activity activity;
    private ActivityResultLauncher<Intent> vpnPermissionLauncher;

    public VpnManager(@NonNull Activity activity) {
        this.activity = activity;
        registerVpnLauncher();
    }

    private void registerVpnLauncher() {
        if (!(activity instanceof androidx.fragment.app.FragmentActivity)) return;

        vpnPermissionLauncher = ((androidx.fragment.app.FragmentActivity) activity)
                .registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                onVpnPermissionGranted();
                            } else {
                                failActivation(R.string.connection_error_vpn_permission_denied);
                            }
                        }
                );
    }

    /**
     * Starts VPN activation process
     */
    public void startVpnActivation() {
        Intent intent = VpnService.prepare(activity);
        if (intent != null) {
            if (vpnPermissionLauncher != null
                    && intent.resolveActivity(activity.getPackageManager()) != null) {
                vpnPermissionLauncher.launch(intent);
            } else {
                failActivation(R.string.connection_error_vpn_not_supported);
            }
        } else {
            onVpnPermissionGranted();
        }

        incrementActivateCounter();
    }

    private void incrementActivateCounter() {
        long counter = Shecan.configurations.getActivateCounter();
        if (counter != -1) {
            Shecan.configurations.setActivateCounter(++counter);
        }
    }

    private void onVpnPermissionGranted() {
        try {
            setupDnsServers();
            Shecan.startVpnService(activity.getApplicationContext());
            Shecan.updateShortcut(activity.getApplicationContext());
        } catch (RuntimeException error) {
            failActivation(R.string.connection_error_vpn_start_failed);
        }
    }

    private void setupDnsServers() {
        if (ShecanVpnService.isProMode()) {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
        } else {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
        }
    }

    /**
     * Call this from Activity's onActivityResult
     */
    public void handleActivityResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            onVpnPermissionGranted();
        } else {
            failActivation(R.string.connection_error_vpn_permission_denied);
        }
    }

    private void failActivation(int messageRes) {
        Shecan app = Shecan.getInstance();
        String message = activity.getString(messageRes);
        if (app != null) {
            app.reportVpnFailure(message);
        } else {
            ToastManager.show(activity, message);
        }
    }
}
