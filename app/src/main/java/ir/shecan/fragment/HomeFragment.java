package ir.shecan.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.Shecan;
import ir.shecan.R;

import ir.shecan.activity.MainActivity;
import ir.shecan.dialog.ContactSupportDialog;
import ir.shecan.dialog.RenewalDialog;
import ir.shecan.dialog.UpdateDialog;
import ir.shecan.service.CoreApiResponseListener;
import ir.shecan.service.BaseApiResponseListener;
import ir.shecan.service.ConnectionStatusApiListener;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.AnimationUtils;
import ir.shecan.util.AppUtils;
import ir.shecan.util.ImageUtils;
import ir.shecan.util.PersianTools;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class HomeFragment extends ToolbarFragment implements CoreApiResponseListener, ConnectionStatusApiListener {

    SimpleDraweeView bannerImageView;

    private static boolean isUpdateVersionCheck = false;
    private ScheduledExecutorService scheduler;

    Button btn;
    ImageView imageView;
    TextView shecanStatus;
    ImageView statusIcon;
    TextView shecanDescription;
    TextView shecanMainTitle;

    Resources resources;
    View view;

    Activity activity;

    private int countdownValue = 80; // Start from 80 seconds
    private boolean isApiSuccess = false;
    private CountDownTimer countDownTimer;
    private static boolean shouldShowSupportDialog = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);

        btn = view.findViewById(R.id.button_activate);
        imageView = view.findViewById(R.id.imageLogo);
        shecanStatus = view.findViewById(R.id.textShecanStatus);
        statusIcon = view.findViewById(R.id.imageViewStatus);
        shecanDescription = view.findViewById(R.id.textShecanDesctiption);
        shecanMainTitle = view.findViewById(R.id.homeTitle);

        final ImageView clearTextBtn = view.findViewById(R.id.clear_btn);

        resources = getResources();

        final Button freeModeBtn = view.findViewById(R.id.freeModeBtn);
        final Button proModeBtn = view.findViewById(R.id.proModeBtn);

        RadioButton dynamicRBtn = view.findViewById(R.id.dynamic_radio_btn);
        RadioButton staticBtn = view.findViewById(R.id.static_radio_btn);

        final LinearLayout proModeExpandLayout = view.findViewById(R.id.pro_mode_expand_layout);
        final LinearLayout dynamicExpandLayout = view.findViewById(R.id.dynamic_expand_layout);

        final EditText linkUpdaterEditText = view.findViewById(R.id.link_updater_edit_text);
        final TextInputLayout linkUpdaterInputLayout = view.findViewById(R.id.link_updater_input_layout);

        TextView helpLinkUpdater = view.findViewById(R.id.help_link_updater);

        bannerImageView = view.findViewById(R.id.banner_image_view);

        if (!ShecanVpnService.getUpdaterLink().isEmpty()) {
            clearTextBtn.setVisibility(View.VISIBLE);
            linkUpdaterEditText.setText(ShecanVpnService.getUpdaterLink());
        } else {
            clearTextBtn.setVisibility(View.GONE);
        }
        linkUpdaterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearTextBtn.setVisibility(View.VISIBLE);
                } else {
                    clearTextBtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        clearTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkUpdaterEditText.setText("");
            }
        });

        activity = getActivity();

        // collapse first
        AnimationUtils.collapse(proModeExpandLayout);

        helpLinkUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = Shecan.ShecanInfo.getDynamicIpGuideLink(); // Replace with the URL you want to open
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        dynamicRBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShecanVpnService.isDynamicIPMode()) {
                    AnimationUtils.expand(dynamicExpandLayout);
                }
                Shecan.setDynamicIPMode();
            }
        });

        staticBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ShecanVpnService.isDynamicIPMode()) {
                    AnimationUtils.collapse(dynamicExpandLayout);
                }
                Shecan.setStaticIPMode();
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ShecanVpnService.isActivated()) {
                    Shecan.deactivateService(activity.getApplicationContext());
                } else {
                    if (ShecanVpnService.isProMode()) {
                        if (ShecanVpnService.isDynamicIPMode()) {
                            linkUpdaterInputLayout.setError(null);
                            linkUpdaterInputLayout.setErrorEnabled(false);
                            String updaterUrl = linkUpdaterEditText.getText().toString();
                            if (updaterUrl.isEmpty()) {
                                linkUpdaterInputLayout.setError(getString(R.string.empty_link_updater_error));
                                linkUpdaterInputLayout.setErrorEnabled(true);
                            } else if (updaterUrl.contains("https://ddns.shecan.ir/update?password=")) {
                                Shecan.setUpdaterLink(updaterUrl);
                                ShecanVpnService.callCoreAPI(getActivity().getApplicationContext(), HomeFragment.this);
                            } else {
                                linkUpdaterInputLayout.setError(getString(R.string.false_link_updater_error));
                                linkUpdaterInputLayout.setErrorEnabled(true);
                            }
                        } else {
                            startActivity(new Intent(getActivity(), MainActivity.class)
                                    .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
                        }
                    } else {
                        startActivity(new Intent(activity, MainActivity.class)
                                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
                    }

                }
            }
        });

        freeModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShecanVpnService.isActivated()) {
                    if (ShecanVpnService.isProMode()) {
                        AnimationUtils.collapse(proModeExpandLayout);
                        freeModeBtn.setBackgroundResource(R.drawable.rounded_button);
                        freeModeBtn.setTextColor(getResources().getColor(android.R.color.white));
                        proModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
                        proModeBtn.setTextColor(getResources().getColor(android.R.color.black));
                    }
                    Shecan.setFreeMode();
                }
            }
        });

        proModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShecanVpnService.isActivated()) {
                    if (!ShecanVpnService.isProMode()) {
                        AnimationUtils.expand(proModeExpandLayout);
                        proModeBtn.setBackgroundResource(R.drawable.rounded_button);
                        proModeBtn.setTextColor(getResources().getColor(android.R.color.white));
                        freeModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
                        freeModeBtn.setTextColor(getResources().getColor(android.R.color.black));
                    }
                    Shecan.setProMode();
                }
            }
        });

        LinearLayout linearLayoutDonate = view.findViewById(R.id.linearLayoutDonate);

        if (!ViewConfiguration.get(activity.getApplicationContext()).hasPermanentMenuKey()) {
            Resources resources = activity.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                int padding = resources.getDimensionPixelSize(resourceId);

                linearLayoutDonate.setPadding(0, 0, 0, padding);
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData();
    }

    private void fetchData() {
        Shecan.ShecanInfo.fetchData(getActivity().getApplicationContext(), new BaseApiResponseListener() {
            @Override
            public void onError(String errorMessage) {
                loadBanner(bannerImageView);
            }

            @Override
            public void onSuccess() {
                if (!isUpdateVersionCheck) {
                    checkIsUpdateAvailable();
                    isUpdateVersionCheck = true;
                }
                loadBanner(bannerImageView);
            }
        });
    }

    private void loadBanner(final SimpleDraweeView imageView) {
        String imageUrl = Shecan.ShecanInfo.getBannerImageUrl();

        ImageUtils.INSTANCE.loadImage(activity.getApplicationContext(), imageUrl, imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = Shecan.ShecanInfo.getBannerLink(); // Replace with the URL you want to open
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

    }

    private void checkIsUpdateAvailable() {
        boolean isForce = false;

        String currentVersion = AppUtils.getVersionName(activity);
        String minVersion = Shecan.ShecanInfo.getMinVersion();
        String latestVersion = Shecan.ShecanInfo.getCurrentVersion();

        if (AppUtils.compareVersionNames(minVersion, currentVersion) == 1) { // (greater=1, smaller=-1, equal=0)
            isForce = true;
        }

        if (AppUtils.compareVersionNames(latestVersion, currentVersion) == 1) {
            new UpdateDialog(activity).show(isForce);
        }
    }

    private void updateUserInterface() {
        boolean isActive = ShecanVpnService.isActivated();
        View view = getView();
        btn = Objects.requireNonNull(view).findViewById(R.id.button_activate);
        imageView = Objects.requireNonNull(view).findViewById(R.id.imageLogo);
        shecanStatus = view.findViewById(R.id.textShecanStatus);
        statusIcon = view.findViewById(R.id.imageViewStatus);
        shecanDescription = view.findViewById(R.id.textShecanDesctiption);

        final Button freeModeBtn = view.findViewById(R.id.freeModeBtn);
        final Button proModeBtn = view.findViewById(R.id.proModeBtn);

        RadioButton dynamicRBtn = view.findViewById(R.id.dynamic_radio_btn);
        RadioButton staticBtn = view.findViewById(R.id.static_radio_btn);

        final LinearLayout proModeExpandLayout = view.findViewById(R.id.pro_mode_expand_layout);
        LinearLayout dynamicExpandLayout = view.findViewById(R.id.dynamic_expand_layout);

        Resources resources = getResources();

        if (ShecanVpnService.isProMode()) {
            AnimationUtils.expand(proModeExpandLayout);
            proModeBtn.setBackgroundResource(R.drawable.rounded_button);
            proModeBtn.setTextColor(getResources().getColor(android.R.color.white));
            freeModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
            freeModeBtn.setTextColor(getResources().getColor(android.R.color.black));
            if (ShecanVpnService.isDynamicIPMode()) {
                AnimationUtils.expand(dynamicExpandLayout);
                dynamicRBtn.setChecked(true);
            } else {
                AnimationUtils.collapse(dynamicExpandLayout);
                staticBtn.setChecked(true);
            }
        } else {
            freeModeBtn.setBackgroundResource(R.drawable.rounded_button);
            freeModeBtn.setTextColor(getResources().getColor(android.R.color.white));
            proModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
            proModeBtn.setTextColor(getResources().getColor(android.R.color.black));
        }

        if (isActive) {
            if (ShecanVpnService.isProMode()) {
                AnimationUtils.collapse(proModeExpandLayout);
                setViewIsConnecting();
                ShecanVpnService.callConnectionStatusAPI(getActivity().getApplicationContext(), this, 5000);
            } else {
                view.setBackground(resources.getDrawable(R.drawable.background_on));
                btn.setBackground(resources.getDrawable(R.drawable.cloud_disconnected));
                imageView.setBackgroundResource(R.drawable.home_logo);
                shecanStatus.setText(R.string.shecan_status_active);
                shecanStatus.setTextColor(resources.getColor(R.color.colorStatusConnected));
                statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_connected));
                shecanDescription.setText(R.string.notice_main_connected);
                shecanDescription.setTextColor(resources.getColor(R.color.black));
                shecanMainTitle.setTextColor(resources.getColor(R.color.black));
            }
        } else {
            view.setBackground(resources.getDrawable(R.drawable.background_off));
            btn.setBackground(resources.getDrawable(R.drawable.cloud_connected));
            imageView.setBackgroundResource(R.drawable.home_logo_white);
            shecanStatus.setText(R.string.shecan_status_deactive);
            shecanStatus.setTextColor(resources.getColor(R.color.colorStatusDisconnected));
            statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_disconnected));
            shecanDescription.setText(R.string.notice_main_disconnected);
            shecanDescription.setTextColor(resources.getColor(R.color.white));
            shecanMainTitle.setTextColor(resources.getColor(R.color.white));

            if (shouldShowSupportDialog) {
                shouldShowSupportDialog = false;
                new ContactSupportDialog(activity).show();
            }
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        updateUserInterface();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_home).setChecked(true);
        toolbar.setTitle("");
        updateUserInterface();

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            updateUserInterface();
        }
    }

    @Override
    public void onSuccess(String response) {
        startActivity(new Intent(getActivity(), MainActivity.class)
                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
    }

    @Override
    public void onError(String errorMessage) {
        // todo: show toast
    }

    @Override
    public void onInvalid() {
        new RenewalDialog(activity).show();
    }

    @Override
    public void onOutOfRange() {
        shouldShowSupportDialog = false;
        new ContactSupportDialog(activity).show();
    }

    @Override
    public void onInTheRange() {
        Shecan.setStaticIPMode();
        startActivity(new Intent(getActivity(), MainActivity.class)
                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
    }

    @Override
    public void onConnected() {
        isApiSuccess = true;
        stopCountdown();
        view.setBackground(resources.getDrawable(R.drawable.background_on));
        btn.setBackground(resources.getDrawable(R.drawable.cloud_disconnected));
        imageView.setBackgroundResource(R.drawable.home_logo);
        if (ShecanVpnService.isDynamicIPMode()) {
            shecanStatus.setText(R.string.shecan_status_pro_dynamic_active);
        } else {
            shecanStatus.setText(R.string.shecan_status_pro_static_active);
        }
        shecanStatus.setTextColor(resources.getColor(R.color.colorStatusConnected));
        statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_connected));
        statusIcon.setVisibility(View.VISIBLE);
        shecanDescription.setText(R.string.notice_main_connected);
        shecanDescription.setTextColor(resources.getColor(R.color.black));
        shecanMainTitle.setTextColor(resources.getColor(R.color.black));
    }

    private void setViewIsConnecting() {
        isApiSuccess = false;
        shouldShowSupportDialog = false;
        view.setBackground(resources.getDrawable(R.drawable.background_off));
        btn.setBackground(resources.getDrawable(R.drawable.cloud_connected));
        imageView.setBackgroundResource(R.drawable.home_logo_white);
        shecanStatus.setTextColor(resources.getColor(R.color.colorStatusDisconnected));
        statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_disconnected));
        statusIcon.setVisibility(View.GONE);
        shecanDescription.setText(resources.getString(R.string.notice_main_disconnected));
        shecanDescription.setTextColor(resources.getColor(R.color.white));
        shecanMainTitle.setTextColor(resources.getColor(R.color.white));
        String text = resources.getString(R.string.shecan_status_connecting);
        startCountdown(shecanStatus, text);
    }

    private void startCountdown(final TextView textView, final String message) {
        countDownTimer = new CountDownTimer(countdownValue * 1000, 1000) { // 80 seconds, 1-second interval

            @Override
            public void onTick(long millisUntilFinished) {
                if (isApiSuccess) {
                    cancel();
                    return;
                }
                countdownValue = (int) (millisUntilFinished / 1000);
                int minutes = countdownValue / 60;
                int seconds = countdownValue % 60;
                String formattedTime =
                        (minutes < 10 ? "0" + minutes : String.valueOf(minutes)) + ":" +
                                (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
                String text = message + "\n" + PersianTools.convertToPersianDigits(formattedTime);
                textView.setText(text);
            }

            @Override
            public void onFinish() {
                countdownValue = 80; // Reset countdown
                if (!isApiSuccess)
                    startCountdown(textView, message);
            }
        };

        countDownTimer.start();
    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onRetry() {
        if (ShecanVpnService.isDynamicIPMode()) {
            scheduler = Executors.newScheduledThreadPool(1);

            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    ShecanVpnService.callConnectionStatusAPI(getActivity().getApplicationContext(), HomeFragment.this, null);
                }
            }, 20, TimeUnit.SECONDS);
        } else {
            if (ShecanVpnService.isActivated())
                shouldShowSupportDialog = true;
            Shecan.deactivateService(activity.getApplicationContext());
            isApiSuccess = false;
            stopCountdown();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        stopCountdown();
    }
}
