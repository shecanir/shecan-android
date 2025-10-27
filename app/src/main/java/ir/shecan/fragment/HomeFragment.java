package ir.shecan.fragment;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.activity.MainActivity;
import ir.shecan.databinding.FragmentMainBinding;
import ir.shecan.dialog.ContactSupportDialog;
import ir.shecan.dialog.RenewalDialog;
import ir.shecan.dialog.UpdateDialog;
import ir.shecan.service.BaseApiResponseListener;
import ir.shecan.service.ConnectionStatusApiListener;
import ir.shecan.service.CoreApiResponseListener;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.AnimationUtils;
import ir.shecan.util.AppUtils;
import ir.shecan.util.ImageUtils;
import ir.shecan.util.PersianTools;

/**
 * Refactored HomeFragment
 * - Uses ViewBinding (FragmentMainBinding)
 * - Lifecycle-aware: cancels timers/animators/executors in onDestroyView
 * - Removes static UI state
 * - Uses ContextCompat for resources
 * - Safe calls (isAdded/isRemoving checks) for background tasks
 */
public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    private FragmentMainBinding binding;

    // non-static UI state
    private boolean isUpdateVersionCheck = false;
    private boolean shouldShowSupportDialog = false;
    private boolean isConnectBtnEnabled = true;
    private boolean isApiSuccess = false;

    private ScheduledExecutorService scheduler;
    private CountDownTimer countDownTimer;
    private ObjectAnimator blinkAnimator;

    private int countdownValue = 80; // default for dynamic mode

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // initialize views using binding
        setupLinkUpdater();
        setupModeButtons();
        setupDonatePadding();
        setupMainButton();

        // collapse pro layout initially
        AnimationUtils.collapse(binding.proModeExpandLayout);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUserInterface();
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_home).setChecked(true);
        toolbar.setTitle("");
        updateUserInterface();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData();
//        updateUserInterface();
    }

    private void setupLinkUpdater() {
        String updaterLink = ShecanVpnService.getUpdaterLink();
        if (!updaterLink.isEmpty()) {
            binding.clearBtn.setVisibility(View.VISIBLE);
            binding.linkUpdaterEditText.setText(updaterLink);
        } else {
            binding.clearBtn.setVisibility(View.GONE);
        }

        binding.linkUpdaterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.clearBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.clearBtn.setOnClickListener(v -> binding.linkUpdaterEditText.setText(""));

        binding.helpLinkUpdater.setOnClickListener(v -> {
            String url = Shecan.ShecanInfo.getDynamicIpGuideLink();
            if (!url.isEmpty() && isAdded()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }

    private void setupModeButtons() {
        final Context ctx = requireContext();

        binding.freeModeBtn.setOnClickListener(v -> {
            if (!ShecanVpnService.isActivated()) {
                if (ShecanVpnService.isProMode()) {
                    AnimationUtils.collapse(binding.proModeExpandLayout);
                    binding.freeModeBtn.setBackgroundResource(R.drawable.rounded_button);
                    binding.freeModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
                    binding.proModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
                    binding.proModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));
                }
                Shecan.setFreeMode();
            }
        });

        binding.proModeBtn.setOnClickListener(v -> {
            if (!ShecanVpnService.isActivated()) {
                if (!ShecanVpnService.isProMode()) {
                    AnimationUtils.expand(binding.proModeExpandLayout);
                    binding.proModeBtn.setBackgroundResource(R.drawable.rounded_button);
                    binding.proModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
                    binding.freeModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
                    binding.freeModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));
                }
                Shecan.setProMode();
            }
        });

        binding.dynamicRadioBtn.setOnClickListener(v -> {
            if (!ShecanVpnService.isDynamicIPMode()) {
                AnimationUtils.expand(binding.dynamicExpandLayout);
            }
            Shecan.setDynamicIPMode();
        });

        binding.staticRadioBtn.setOnClickListener(v -> {
            if (ShecanVpnService.isDynamicIPMode()) {
                AnimationUtils.collapse(binding.dynamicExpandLayout);
            }
            Shecan.setStaticIPMode();
        });
    }

    private void setupMainButton() {
        binding.buttonActivate.setOnClickListener(v -> {
            if (!isConnectBtnEnabled) return;

            if (ShecanVpnService.isActivated()) {
                ShecanVpnService.cancelConnectionStatusAPI(requireContext());
                ShecanVpnService.cancelCoreAPI(requireContext());
                Shecan.deactivateService(requireContext());
                return;
            }

            setViewIsConnecting();

            if (ShecanVpnService.isProMode()) {
                if (ShecanVpnService.isDynamicIPMode()) {
                    binding.linkUpdaterInputLayout.setError(null);
                    binding.linkUpdaterInputLayout.setErrorEnabled(false);
                    String updaterUrl = binding.linkUpdaterEditText.getText().toString().trim();
                    if (updaterUrl.isEmpty()) {
                        binding.linkUpdaterInputLayout.setError(getString(R.string.empty_link_updater_error));
                        binding.linkUpdaterInputLayout.setErrorEnabled(true);
                        return;
                    }
                    if (updaterUrl.contains("https://ddns.shecan.ir/update?password=")) {
                        Shecan.setUpdaterLink(updaterUrl);
                        ShecanVpnService.callCoreAPI(requireContext(), HomeFragment.this);
                    } else {
                        binding.linkUpdaterInputLayout.setError(getString(R.string.false_link_updater_error));
                        binding.linkUpdaterInputLayout.setErrorEnabled(true);
                    }
                } else {
                    isConnectBtnEnabled = false;
                    startActivity(new Intent(requireActivity(), MainActivity.class)
                            .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
                }
            } else {
                startActivity(new Intent(requireActivity(), MainActivity.class)
                        .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
            }
        });
    }

    private void setupDonatePadding() {
        final LinearLayout donate = binding.linearLayoutDonate;
        if (!ViewConfiguration.get(requireContext()).hasPermanentMenuKey()) {
            ViewCompat.setOnApplyWindowInsetsListener(donate, (v, insets) -> {
                Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, 0, 0, navBarInsets.bottom);
                return insets;
            });
        }
    }

    private void fetchData() {
        if (!isAdded()) return;

        Shecan.ShecanInfo.fetchData(requireContext(), new BaseApiResponseListener() {
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                loadBanner();
            }

            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                if (!isUpdateVersionCheck) {
                    checkIsUpdateAvailable();
                    isUpdateVersionCheck = true;
                }
                loadBanner();
            }
        });
    }

    private void loadBanner() {
        if (!isAdded()) return;
        final String imageUrl = Shecan.ShecanInfo.getBannerImageUrl();
        if (!imageUrl.isEmpty()) {
            ImageUtils.INSTANCE.loadImage(requireContext(), imageUrl, binding.bannerImageView);
        }
        binding.bannerImageView.setOnClickListener(v -> {
            String url = Shecan.ShecanInfo.getBannerLink();
            if (!url.isEmpty() && isAdded()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }

    private void checkIsUpdateAvailable() {
        if (!isAdded()) return;
        boolean isForce = false;
        String currentVersion = AppUtils.getVersionName(requireActivity());
        String minVersion = Shecan.ShecanInfo.getMinVersion();
        String latestVersion = Shecan.ShecanInfo.getCurrentVersion();

        if (AppUtils.compareVersionNames(minVersion, currentVersion) == 1) { // min > current
            isForce = true;
        }
        if (AppUtils.compareVersionNames(latestVersion, currentVersion) == 1) {
            new UpdateDialog(requireActivity()).show(isForce);
        }
    }

    private void updateUserInterface() {
        if (!isAdded()) return;

        final Context ctx = requireContext();
        final Resources res = getResources();

        boolean isActive = ShecanVpnService.isActivated();

        if (ShecanVpnService.isProMode()) {
            AnimationUtils.expand(binding.proModeExpandLayout);
            binding.proModeBtn.setBackgroundResource(R.drawable.rounded_button);
            binding.proModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
            binding.freeModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
            binding.freeModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));

            if (ShecanVpnService.isDynamicIPMode()) {
                AnimationUtils.expand(binding.dynamicExpandLayout);
                binding.dynamicRadioBtn.setChecked(true);
            } else {
                AnimationUtils.collapse(binding.dynamicExpandLayout);
                binding.staticRadioBtn.setChecked(true);
            }
        } else {
            binding.freeModeBtn.setBackgroundResource(R.drawable.rounded_button);
            binding.freeModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
            binding.proModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
            binding.proModeBtn.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));
        }

        if (isActive) {
            if (ShecanVpnService.isProMode()) {
                AnimationUtils.collapse(binding.proModeExpandLayout);
                setViewIsConnecting();
                ShecanVpnService.callConnectionStatusAPI(requireContext(), this, 5000);
            } else {
                setUiForConnected(false);
            }
        } else {
            setUiForDisconnected();
            if (shouldShowSupportDialog) {
                shouldShowSupportDialog = false;
                new ContactSupportDialog(requireActivity()).show();
            }
        }
    }

    private void setUiForConnected(boolean isDynamicMode) {
        if (!isAdded()) return;
        final Context ctx = requireContext();
        binding.getRoot().setBackground(ContextCompat.getDrawable(ctx, R.drawable.background_on));
        binding.buttonActivate.setBackground(ContextCompat.getDrawable(ctx, R.drawable.cloud_disconnected));
        binding.imageLogo.setBackgroundResource(R.drawable.home_logo);
        binding.textShecanStatus.setText(isDynamicMode ? R.string.shecan_status_pro_dynamic_active
                : R.string.shecan_status_pro_static_active);
        binding.textShecanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.colorStatusConnected));
        binding.imageViewStatus.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.status_connected));
        binding.imageViewStatus.setVisibility(View.VISIBLE);
        binding.textShecanDesctiption.setText(R.string.notice_main_connected);
        binding.textShecanDesctiption.setTextColor(ContextCompat.getColor(ctx, R.color.black));
        binding.homeTitle.setTextColor(ContextCompat.getColor(ctx, R.color.black));
    }

    private void setUiForDisconnected() {
        if (!isAdded()) return;
        final Context ctx = requireContext();
        binding.getRoot().setBackground(ContextCompat.getDrawable(ctx, R.drawable.background_off));
        binding.buttonActivate.setBackground(ContextCompat.getDrawable(ctx, R.drawable.cloud_connected));
        binding.imageLogo.setBackgroundResource(R.drawable.home_logo_white);
        binding.textShecanStatus.setText(R.string.shecan_status_deactive);
        binding.textShecanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.colorStatusDisconnected));
        binding.imageViewStatus.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.status_disconnected));
        binding.imageViewStatus.setVisibility(View.VISIBLE);
        binding.textShecanDesctiption.setText(R.string.notice_main_disconnected);
        binding.textShecanDesctiption.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        binding.homeTitle.setTextColor(ContextCompat.getColor(ctx, R.color.white));
    }

    private void setViewIsConnecting() {
        if (!isAdded()) return;
        isApiSuccess = false;
        shouldShowSupportDialog = false;
        final Context ctx = requireContext();

        binding.getRoot().setBackground(ContextCompat.getDrawable(ctx, R.drawable.background_off));
        binding.buttonActivate.setBackground(ContextCompat.getDrawable(ctx, R.drawable.cloud_connected));
        binding.buttonActivate.setAlpha(isConnectBtnEnabled ? 1f : 0.5f);
        binding.imageLogo.setBackgroundResource(R.drawable.home_logo_white);
        binding.textShecanStatus.setTextColor(ContextCompat.getColor(ctx, R.color.colorStatusDisconnected));
        binding.imageViewStatus.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.status_disconnected));
        binding.imageViewStatus.setVisibility(View.GONE);
        binding.textShecanDesctiption.setText(getString(R.string.notice_main_disconnected));
        binding.textShecanDesctiption.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        binding.homeTitle.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        startBlinkAnimation();
        String text = getResources().getString(R.string.shecan_status_connecting);
        startCountdown(binding.textShecanStatus, text);
    }

    private void startCountdown(final android.widget.TextView textView, final String message) {
        if (!isAdded()) return;
        stopCountdown();

        countdownValue = ShecanVpnService.isDynamicIPMode() ? 80 : 5;
        countDownTimer = new CountDownTimer(countdownValue * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isApiSuccess) {
                    cancel();
                    return;
                }
                countdownValue = (int) (millisUntilFinished / 1000L);
                int minutes = countdownValue / 60;
                int seconds = countdownValue % 60;
                String formattedTime = (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":"
                        + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
                String text = message + "\n" + PersianTools.convertToPersianDigits(formattedTime);
                if (isAdded()) {
                    textView.setText(text);
                }
            }

            @Override
            public void onFinish() {
                if (!isAdded()) return;
                countdownValue = ShecanVpnService.isDynamicIPMode() ? 80 : 5;
                if (!isApiSuccess) startCountdown(textView, message);
            }
        };
        countDownTimer.start();
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void startBlinkAnimation() {
        if (!isAdded()) return;
        stopBlinkAnimation();
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.6f);
        blinkAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.buttonActivate, scaleX, scaleY, alpha);
        blinkAnimator.setDuration(800);
        blinkAnimator.setInterpolator(new LinearInterpolator());
        blinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        blinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        blinkAnimator.start();
    }

    private void stopBlinkAnimation() {
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
            binding.buttonActivate.setAlpha(1f);
            binding.buttonActivate.setScaleX(1f);
            binding.buttonActivate.setScaleY(1f);
            blinkAnimator = null;
        }
    }

    @Override
    public void onSuccess(String response) {
        if (!isAdded()) return;
        startActivity(new Intent(requireActivity(), MainActivity.class)
                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
    }

    @Override
    public void onError(String errorMessage) {
        stopBlinkAnimation();
    }

    @Override
    public void onInvalid() {
        stopBlinkAnimation();
        if (isAdded()) new RenewalDialog(requireActivity()).show();
    }

    @Override
    public void onOutOfRange() {
        shouldShowSupportDialog = false;
        stopBlinkAnimation();
        if (isAdded()) new ContactSupportDialog(requireActivity()).show();
    }

    @Override
    public void onInTheRange() {
        if (isAdded()) {
            Shecan.setStaticIPMode();
            startActivity(new Intent(requireActivity(), MainActivity.class)
                    .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
        }
        stopBlinkAnimation();
    }

    @Override
    public void onConnected() {
        if (!isAdded()) return;
        isApiSuccess = true;
        isConnectBtnEnabled = true;
        stopCountdown();
        stopBlinkAnimation();

        setUiForConnected(ShecanVpnService.isDynamicIPMode());

        binding.buttonActivate.setAlpha(isConnectBtnEnabled ? 1f : 0.5f);
    }

    @Override
    public void onRetry() {
        if (ShecanVpnService.isDynamicIPMode()) {
            if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
            scheduler = Executors.newSingleThreadScheduledExecutor();
            // Schedule a background task that POSTS to the main thread rather than calling fragment methods directly.
            scheduler.schedule(() -> {
                // post to main (UI) thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    // now we are on UI thread, it's safe to check fragment state and call requireContext()
                    if (isAdded() && !isRemoving()) {
                        ShecanVpnService.callConnectionStatusAPI(requireContext(), HomeFragment.this, null);
                    }
                });
            }, 20, TimeUnit.SECONDS);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded() || isRemoving()) return;
                isConnectBtnEnabled = true;
                if (ShecanVpnService.isActivated()) shouldShowSupportDialog = true;
                Shecan.deactivateService(requireContext());
                isApiSuccess = false;
                stopCountdown();
                stopBlinkAnimation();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        stopCountdown();
        stopBlinkAnimation();
        binding = null; // avoid leaks
    }
}
