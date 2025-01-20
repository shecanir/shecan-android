package ir.shecan.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import ir.shecan.Shecan;
import ir.shecan.R;

import ir.shecan.activity.MainActivity;
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
public class HomeFragment extends ToolbarFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        Button but = view.findViewById(R.id.button_activate);
        final Activity activity = getActivity();
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ShecanVpnService.isActivated()) {
                    Shecan.deactivateService(activity.getApplicationContext());
                } else {
                    startActivity(new Intent(activity, MainActivity.class)
                            .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_ACTIVATE));
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

    private void updateUserInterface() {
        boolean isActive = ShecanVpnService.isActivated();
        View view = getView();
        Button button = Objects.requireNonNull(view).findViewById(R.id.button_activate);
        ImageView imageView = Objects.requireNonNull(view).findViewById(R.id.imageLogo);
        TextView shecanStatus = view.findViewById(R.id.textShecanStatus);
        ImageView statusIcon = view.findViewById(R.id.imageViewStatus);
        TextView shecanDescription = view.findViewById(R.id.textShecanDesctiption);
        Resources resources = getResources();
        if (isActive) {
            view.setBackground(resources.getDrawable(R.drawable.background_on));
            button.setBackground(resources.getDrawable(R.drawable.cloud_disconnected));
            imageView.setBackgroundResource(R.drawable.home_logo);
            shecanStatus.setText(R.string.shecan_status_active);
            shecanStatus.setTextColor(resources.getColor(R.color.colorStatusConnected));
            statusIcon.setImageDrawable(resources.getDrawable(R.drawable.status_connected));
            shecanDescription.setText(R.string.notice_main_connected);
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


}
