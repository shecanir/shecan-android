package ir.shecan.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ir.shecan.Shecan;
import ir.shecan.R;

import ir.shecan.activity.MainActivity;
import ir.shecan.service.ApiResponseListener;
import ir.shecan.service.ApiService;
import ir.shecan.service.ConnectionStatusListener;
import ir.shecan.service.ShecanVpnService;

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
public class HomeFragment extends ToolbarFragment implements ApiResponseListener, ConnectionStatusListener {

    ImageView bannerImageView;

    private static boolean isUpdateVersionCheck = false;
    private ScheduledExecutorService scheduler;

    Button btn;
    ImageView imageView;
    TextView shecanStatus;
    ImageView statusIcon;
    TextView shecanDescription;

    Resources resources;
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);

        btn = view.findViewById(R.id.button_activate);
        imageView = view.findViewById(R.id.imageLogo);
        shecanStatus = view.findViewById(R.id.textShecanStatus);
        statusIcon = view.findViewById(R.id.imageViewStatus);
        shecanDescription = view.findViewById(R.id.textShecanDesctiption);

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
        loadBanner(bannerImageView);


        if(!ShecanVpnService.getUpdaterLink().isEmpty()){
            clearTextBtn.setVisibility(View.VISIBLE);
            linkUpdaterEditText.setText(ShecanVpnService.getUpdaterLink());
        } else {
            clearTextBtn.setVisibility(View.GONE);
        }
        linkUpdaterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()> 0){
                    clearTextBtn.setVisibility(View.VISIBLE);
                } else {
                    clearTextBtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                linkUpdaterEditText.setText("");
            }
        });

        final Activity activity = getActivity();

        // collapse first
        collapse(proModeExpandLayout);

        if (!isUpdateVersionCheck) {
            checkIsUpdateAvailable();
            isUpdateVersionCheck = true;
        }


        helpLinkUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo: change it later
                String url = "https://shecan.ir"; // Replace with the URL you want to open
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        dynamicRBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShecanVpnService.isDynamicIPMode()) {
                    expand(dynamicExpandLayout);
                }
                Shecan.setDynamicIPMode();
            }
        });

        staticBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ShecanVpnService.isDynamicIPMode()) {
                    collapse(dynamicExpandLayout);
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
                            ShecanVpnService.callCoreAPI(getActivity().getApplicationContext(), HomeFragment.this);
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
                        collapse(proModeExpandLayout);
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
                        expand(proModeExpandLayout);
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

    private void loadBanner(final ImageView imageView) {
        // todo: change the image url later
        String imageUrl = "https://fakeimg.pl/600x360";

        Picasso.get()
                .load(imageUrl) // URL of the image
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        imageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        imageView.setVisibility(View.GONE); // Keep the ImageView hidden if image loading fails
                    }
                });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo: change the link later
                String url = "https://shecan.ir"; // Replace with the URL you want to open
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

    }

    // Expand function
    private void expand(final View view) {
        view.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                } else {
                    view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                }
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration((int) (targetHeight / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);
    }

    // Collapse function
    private void collapse(final View view) {
        final int initialHeight = view.getMeasuredHeight();

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration((int) (initialHeight / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showRenewalDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_active_service, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Button openUrlButton = dialogView.findViewById(R.id.renewalButton);
        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                // todo: change the url later
                String url = "https://shecan.ir"; // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showContactSupportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_contact_support, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Button openUrlButton = dialogView.findViewById(R.id.contactButton);
        final AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                // todo: change the url later
                String url = "https://shecan.ir"; // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showUpdateDialog(Boolean isForceUpdate) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_app, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Button openUrlButton = dialogView.findViewById(R.id.updateBtn);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        final AlertDialog dialog = builder.create();

        if (isForceUpdate) {
            cancelBtn.setVisibility(View.GONE);
            dialog.setCancelable(false);
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            if (isForceUpdate) {
                dialog.setCanceledOnTouchOutside(false);
            }
        }

        openUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo: change the url later
                String url = "https://shecan.ir"; // Replace with your desired URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });


        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void checkIsUpdateAvailable() {
        boolean isAvailable = false;
        boolean isForce = false;
        if (isAvailable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showUpdateDialog(isForce);
            }
        }
    }

    private void updateUserInterface() {
        boolean isActive = ShecanVpnService.isActivated();
        View view = getView();
        Button button = Objects.requireNonNull(view).findViewById(R.id.button_activate);
        ImageView imageView = Objects.requireNonNull(view).findViewById(R.id.imageLogo);
        TextView shecanStatus = view.findViewById(R.id.textShecanStatus);
        ImageView statusIcon = view.findViewById(R.id.imageViewStatus);
        TextView shecanDescription = view.findViewById(R.id.textShecanDesctiption);

        final Button freeModeBtn = view.findViewById(R.id.freeModeBtn);
        final Button proModeBtn = view.findViewById(R.id.proModeBtn);

        RadioButton dynamicRBtn = view.findViewById(R.id.dynamic_radio_btn);
        RadioButton staticBtn = view.findViewById(R.id.static_radio_btn);

        final LinearLayout proModeExpandLayout = view.findViewById(R.id.pro_mode_expand_layout);
        LinearLayout dynamicExpandLayout = view.findViewById(R.id.dynamic_expand_layout);

        Resources resources = getResources();

        if (ShecanVpnService.isProMode()) {
            expand(proModeExpandLayout);
            proModeBtn.setBackgroundResource(R.drawable.rounded_button);
            proModeBtn.setTextColor(getResources().getColor(android.R.color.white));
            freeModeBtn.setBackgroundResource(R.drawable.default_no_background_button);
            freeModeBtn.setTextColor(getResources().getColor(android.R.color.black));
            if (ShecanVpnService.isDynamicIPMode()) {
                expand(dynamicExpandLayout);
                dynamicRBtn.setChecked(true);
            } else {
                collapse(dynamicExpandLayout);
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
                collapse(proModeExpandLayout);
                setViewIsConnecting();
                ShecanVpnService.callConnectionStatusAPI(getActivity().getApplicationContext(), this);
            } else {
                view.setBackground(resources.getDrawable(R.drawable.background_on));
                button.setBackground(resources.getDrawable(R.drawable.cloud_disconnected));
                imageView.setBackgroundResource(R.drawable.home_logo);
                shecanStatus.setText(R.string.shecan_status_active);
                shecanStatus.setTextColor(resources.getColor(R.color.colorStatusConnected));
                statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_connected));
                shecanDescription.setText(R.string.notice_main_connected);
            }
        } else {
            view.setBackground(resources.getDrawable(R.drawable.background_off));
            button.setBackground(resources.getDrawable(R.drawable.cloud_connected));
            imageView.setBackgroundResource(R.drawable.home_logo_white);
            shecanStatus.setText(R.string.shecan_status_deactive);
            shecanStatus.setTextColor(resources.getColor(R.color.colorStatusDisconnected));
            statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_disconnected));
            shecanDescription.setText(R.string.notice_main_disconnected);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showRenewalDialog();
        }
    }

    @Override
    public void onOutOfRange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showContactSupportDialog();
        }
    }

    @Override
    public void onInTheRange() {
        startActivity(new Intent(getActivity(), MainActivity.class)
                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
    }

    @Override
    public void onConnected() {
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
    }

    private void setViewIsConnecting() {
        view.setBackground(resources.getDrawable(R.drawable.background_off));
        btn.setBackground(resources.getDrawable(R.drawable.cloud_connected));
        imageView.setBackgroundResource(R.drawable.home_logo_white);
        // todo: set text is connecting
        String text = resources.getString(R.string.shecan_status_connecting);
        text = text + "\n00:00";
        shecanStatus.setText(text);
        shecanStatus.setTextColor(resources.getColor(R.color.colorStatusDisconnected));
        statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_disconnected));
        statusIcon.setVisibility(View.GONE);
        shecanDescription.setText(R.string.notice_main_disconnected);
    }

    @Override
    public void onRetry() {
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                ShecanVpnService.callConnectionStatusAPI(getActivity().getApplicationContext(), HomeFragment.this);
            }
        }, 20, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
